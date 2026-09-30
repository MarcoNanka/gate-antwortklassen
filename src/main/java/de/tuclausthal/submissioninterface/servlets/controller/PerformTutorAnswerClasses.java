/*
 * Copyright 2026
 *
 * This file is part of the GATE.
 *
 * GATE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * GATE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GATE. If not, see <http://www.gnu.org/licenses/>.
 */

package de.tuclausthal.submissioninterface.servlets.controller;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tuclausthal.submissioninterface.answerclasses.AnswerClassExecutor;
import de.tuclausthal.submissioninterface.answerclasses.AnswerClassRunner;
import de.tuclausthal.submissioninterface.answerclasses.AnswerClassToEvaluate;
import de.tuclausthal.submissioninterface.answerclasses.PipelineResult;
import de.tuclausthal.submissioninterface.answerclasses.TutorAnswerClassPreview;
import de.tuclausthal.submissioninterface.persistence.dao.AnswerClassDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.dao.ParticipationDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.SubmissionDAOIf;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;
import de.tuclausthal.submissioninterface.persistence.datamodel.Participation;
import de.tuclausthal.submissioninterface.persistence.datamodel.ParticipationRole;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.servlets.GATEController;
import de.tuclausthal.submissioninterface.servlets.RequestAdapter;
import de.tuclausthal.submissioninterface.servlets.view.MessageView;
import de.tuclausthal.submissioninterface.servlets.view.PerformStudentTestRunningView;
import de.tuclausthal.submissioninterface.util.Configuration;
import de.tuclausthal.submissioninterface.util.HibernateSessionHelper;
import de.tuclausthal.submissioninterface.util.Util;

/**
 * Controller-Servlet for tutors on one submission's answer classes:
 * <ul>
 * <li>no {@code answerclassid} parameter: run the pipeline as a tutor preview. Results are
 * not written to {@code answerclassresults}; they are put in the HTTP session together with
 * the submission's {@code lastModified} only for the page view right after this redirect (see
 * {@code ShowSubmission}, which removes them from the session as soon as it reads them, and
 * {@code TutorAnswerClassPreview}). Leaving/reloading the submission page afterwards shows an
 * empty "Probelauf"-column again. The button is refused once every automatic answer class of
 * this submission already has a batch result.
 * <p>{@code doPost} only submits the run to {@link AnswerClassExecutor} and redirects to this
 * servlet's own {@code doGet}, instead of running the (potentially multi-second) pipeline
 * synchronously in the Tomcat request thread that handled the button click. {@code doGet} shows
 * a "wird ausgeführt, bitte warten" polling page while the run is not done yet (same
 * pattern/pool as {@code PerformStudentAnswerClasses}/classic {@code PerformStudentTest}), and
 * once it is done, stores the {@code Snapshot} and redirects on to {@code ShowSubmission} exactly
 * like the old synchronous code did.</li>
 * <li>{@code answerclassid} + {@code status} parameters: manually set one answer class whose
 * detection type is {@code MANUAL}, and only after the deadline or a final submission.
 * Source {@code MANUAL}, {@code evaluatedBy} the current tutor. Automatic answer classes
 * cannot be overwritten here.</li>
 * </ul>
 * Mirrors the "Test auf Abgabe ausführen"-form from {@code PerformTest}, but - like
 * {@code AnswerClassRunner} in general - does not touch the {@code Test} entity/DAO hierarchy;
 * it only shares {@code LocalExecutor}'s bounded thread pool via {@code AnswerClassExecutor}.
 */
@GATEController
public class PerformTutorAnswerClasses extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Session session = RequestAdapter.getSession(request);
		SubmissionDAOIf submissionDAO = DAOFactory.SubmissionDAOIf(session);
		Submission submission = submissionDAO.getSubmission(Util.parseInteger(request.getParameter("sid"), 0));
		if (submission == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			request.setAttribute("title", "Abgabe nicht gefunden");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		}
		Task task = submission.getTask();

		ParticipationDAOIf participationDAO = DAOFactory.ParticipationDAOIf(session);
		Participation participation = participationDAO.getParticipation(RequestAdapter.getUser(request), task.getTaskGroup().getLecture());
		if (participation == null || participation.getRoleType().compareTo(ParticipationRole.TUTOR) < 0) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "insufficient rights");
			return;
		}

		String queuedKey = TutorAnswerClassPreview.queuedSessionKey(submission.getSubmissionid());
		Object storedQueued = request.getSession().getAttribute(queuedKey);
		if (!(storedQueued instanceof TutorAnswerClassPreview.QueuedRun queued) || queued.getSubmissionId() != submission.getSubmissionid()) {
			// Nothing queued (e.g. direct GET without a preceding POST) - nothing to do here.
			response.sendRedirect(Util.generateRedirectURL(ShowSubmission.class.getSimpleName() + "?sid=" + submission.getSubmissionid(), response));
			return;
		}

		if (!queued.getResultFuture().isDone()) {
			request.setAttribute("task", task);
			request.setAttribute("refreshurl", Util.generateRedirectURL(PerformTutorAnswerClasses.class.getSimpleName() + "?sid=" + submission.getSubmissionid(), response));
			request.setAttribute("redirectTime", 5);
			request.setAttribute("runningPageTitle", "Antwortklassen...");
			request.setAttribute("runningMessage", "Der Antwortklassen-Probelauf wird im Hintergrund ausgeführt. Bitte warten.<br>");
			getServletContext().getNamedDispatcher(PerformStudentTestRunningView.class.getSimpleName()).forward(request, response);
			return;
		}

		request.getSession().removeAttribute(queuedKey);

		List<PipelineResult> previewResults;
		try {
			previewResults = queued.getResultFuture().get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOG.warn("Was interrupted while waiting for answer classes to finish for submission {}", submission.getSubmissionid(), e);
			request.setAttribute("title", "Beim Prüfen der Antwortklassen ist ein Fehler aufgetreten.");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		} catch (ExecutionException e) {
			LOG.error("Failed to preview answer classes for submission {}", submission.getSubmissionid(), e.getCause());
			request.setAttribute("title", "Beim Prüfen der Antwortklassen ist ein Fehler aufgetreten.");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		}

		String previewKey = TutorAnswerClassPreview.sessionKey(submission.getSubmissionid());
		if (Objects.equals(queued.getSubmissionLastModifiedAtStart(), submission.getLastModified())) {
			Map<Integer, PipelineResult> byAnswerClassId = previewResults.stream().collect(Collectors.toMap(PipelineResult::getAnswerClassId, Function.identity()));
			request.getSession().setAttribute(previewKey, new TutorAnswerClassPreview.Snapshot(submission.getSubmissionid(), queued.getSubmissionLastModifiedAtStart(), byAnswerClassId));
		} else {
			request.getSession().removeAttribute(previewKey);
		}
		response.sendRedirect(Util.generateRedirectURL(ShowSubmission.class.getSimpleName() + "?sid=" + submission.getSubmissionid(), response));
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Session session = RequestAdapter.getSession(request);
		SubmissionDAOIf submissionDAO = DAOFactory.SubmissionDAOIf(session);
		Submission submission = submissionDAO.getSubmission(Util.parseInteger(request.getParameter("sid"), 0));
		if (submission == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			request.setAttribute("title", "Abgabe nicht gefunden");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		}
		Task task = submission.getTask();

		ParticipationDAOIf participationDAO = DAOFactory.ParticipationDAOIf(session);
		Participation participation = participationDAO.getParticipation(RequestAdapter.getUser(request), task.getTaskGroup().getLecture());
		if (participation == null || participation.getRoleType().compareTo(ParticipationRole.TUTOR) < 0) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "insufficient rights");
			return;
		}

		if (request.getParameter("answerclassid") != null) {
			AnswerClassDAOIf answerClassDAO = DAOFactory.AnswerClassDAOIf(session);
			AnswerClass answerClass = answerClassDAO.getAnswerClass(Util.parseInteger(request.getParameter("answerclassid"), 0));
			if (answerClass == null || answerClass.getTask().getTaskid() != task.getTaskid()) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				request.setAttribute("title", "Antwortklasse nicht gefunden");
				getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
				return;
			}
			if (!AnswerClassToEvaluate.MANUAL_DETECTION_TYPE.equals(answerClass.getDetectionType())) {
				request.setAttribute("title", "Diese Antwortklasse kann nicht manuell gesetzt werden.");
				getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
				return;
			}
			if (!(task.getDeadline().isBefore(ZonedDateTime.now()) || (task.isAllowPrematureSubmissionClosing() && submission.isClosed()))) {
				request.setAttribute("title", "Manuelle Bewertung ist erst nach Ablauf der Deadline oder nach endgültiger Abgabe möglich.");
				getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
				return;
			}
			AnswerClassResultStatus status;
			try {
				status = AnswerClassResultStatus.valueOf(request.getParameter("status"));
			} catch (IllegalArgumentException | NullPointerException e) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid status");
				return;
			}
			session.beginTransaction();
			DAOFactory.AnswerClassResultDAOIf(session).storeResult(answerClass, submission, status, request.getParameter("details"), AnswerClassResultSource.MANUAL, RequestAdapter.getUser(request));
			session.getTransaction().commit();
			response.sendRedirect(Util.generateRedirectURL(ShowSubmission.class.getSimpleName() + "?sid=" + submission.getSubmissionid(), response));
			return;
		}

		List<AnswerClass> answerClasses = DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(task);
		List<AnswerClassResult> storedResults = DAOFactory.AnswerClassResultDAOIf(session).getResultsForSubmission(submission);
		Map<Integer, AnswerClassResult> storedByAnswerClassId = storedResults.stream().collect(Collectors.toMap(result -> result.getAnswerClass().getId(), Function.identity()));
		List<String> submittedFiles = Util.listFilesAsRelativeStringListSorted(Util.constructPath(Configuration.getInstance().getDataPath(), submission));
		if (submittedFiles.isEmpty()) {
			request.setAttribute("title", "Keine Dateien in der Abgabe.");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		}
		if (!TutorAnswerClassPreview.isAvailable(answerClasses, storedByAnswerClassId)) {
			request.setAttribute("title", "Die automatischen Antwortklassen dieser Abgabe sind bereits ausgewertet.");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		}

		String queuedKey = TutorAnswerClassPreview.queuedSessionKey(submission.getSubmissionid());
		Object storedQueued = request.getSession().getAttribute(queuedKey);
		if (storedQueued instanceof TutorAnswerClassPreview.QueuedRun queued && !queued.getResultFuture().isDone()) {
			request.setAttribute("title", "Für diese Abgabe läuft bereits ein Probelauf.");
			request.setAttribute("message", "<div class=mid><a href=\"" + Util.generateHTMLLink(PerformTutorAnswerClasses.class.getSimpleName() + "?sid=" + submission.getSubmissionid(), response) + "\">weiter zum bereits laufenden Probelauf</a></div>");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		}

		final int submissionId = submission.getSubmissionid();
		final Path dataPath = Configuration.getInstance().getDataPath();
		Future<List<PipelineResult>> resultFuture = AnswerClassExecutor.execute(() -> runInBackground(submissionId, dataPath));
		request.getSession().setAttribute(queuedKey, new TutorAnswerClassPreview.QueuedRun(submissionId, submission.getLastModified(), resultFuture));
		response.sendRedirect(Util.generateRedirectURL(PerformTutorAnswerClasses.class.getSimpleName() + "?sid=" + submission.getSubmissionid(), response));
	}

	/**
	 * Runs on a {@code LocalExecutor} worker thread, not the calling Tomcat request thread - so,
	 * like {@code TestTask#performTask}, it opens and closes its own Hibernate {@code Session}
	 * instead of reusing the caller's.
	 */
	private static List<PipelineResult> runInBackground(int submissionId, Path dataPath) throws Exception {
		Session bgSession = HibernateSessionHelper.getSessionFactory().openSession();
		try {
			Transaction tx = bgSession.beginTransaction();
			try {
				Submission bgSubmission = DAOFactory.SubmissionDAOIf(bgSession).getSubmission(submissionId);
				List<PipelineResult> result = new AnswerClassRunner(bgSession).preview(bgSubmission, dataPath);
				tx.commit();
				return result;
			} catch (Exception e) {
				tx.rollback();
				throw e;
			}
		} finally {
			bgSession.close();
		}
	}
}
