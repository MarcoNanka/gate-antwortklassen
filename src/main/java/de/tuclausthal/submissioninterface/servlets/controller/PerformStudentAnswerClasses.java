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
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tuclausthal.submissioninterface.answerclasses.AnswerClassRunner;
import de.tuclausthal.submissioninterface.answerclasses.PipelineResult;
import de.tuclausthal.submissioninterface.answerclasses.PipelineTrigger;
import de.tuclausthal.submissioninterface.persistence.dao.AnswerClassResultDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.AnswerClassRunCountDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.dao.ParticipationDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.SubmissionDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.TaskDAOIf;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;
import de.tuclausthal.submissioninterface.persistence.datamodel.Participation;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.servlets.GATEController;
import de.tuclausthal.submissioninterface.servlets.RequestAdapter;
import de.tuclausthal.submissioninterface.servlets.view.MessageView;
import de.tuclausthal.submissioninterface.servlets.view.ShowAnswerClassResultsView;
import de.tuclausthal.submissioninterface.util.Configuration;
import de.tuclausthal.submissioninterface.util.Util;

/**
 * Controller-Servlet for students requesting an answer-class run on their own submission.
 *
 * <p>The run is a one-time preview: it is not written to {@code answerclassresults}. The result
 * page shows it once after the redirect; a reload does not. After the deadline the task page
 * shows batch results and tutor grades only.</p>
 *
 * <p>Mirrors {@code PerformStudentTest} in that it runs synchronously and uses
 * POST-then-redirect so a reload never starts another run. The run-limit check lives here,
 * not in {@code AnswerClassPipeline}.</p>
 */
@GATEController
public class PerformStudentAnswerClasses extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String PREVIEW_SESSION_KEY = "studentAnswerClassPreview-";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Session session = RequestAdapter.getSession(request);
		TaskDAOIf taskDAO = DAOFactory.TaskDAOIf(session);
		Task task = taskDAO.getTask(Util.parseInteger(request.getParameter("taskid"), 0));
		if (task == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			request.setAttribute("title", "Aufgabe nicht gefunden");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		}

		ParticipationDAOIf participationDAO = DAOFactory.ParticipationDAOIf(session);
		Participation participation = participationDAO.getParticipation(RequestAdapter.getUser(request), task.getTaskGroup().getLecture());
		if (participation == null) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "insufficient rights");
			return;
		}

		SubmissionDAOIf submissionDAO = DAOFactory.SubmissionDAOIf(session);
		Submission submission = submissionDAO.getSubmission(task, RequestAdapter.getUser(request));
		if (submission == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			request.setAttribute("title", "Keine Abgabe gefunden");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		}

		List<AnswerClass> answerClasses = DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(task);
		List<AnswerClassResult> results = DAOFactory.AnswerClassResultDAOIf(session).getResultsForSubmission(submission);
		int runnable = DAOFactory.AnswerClassRunCountDAOIf(session).canStillRunXTimes(task, RequestAdapter.getUser(request));
		String previewKey = PREVIEW_SESSION_KEY + task.getTaskid();
		Object preview = request.getSession().getAttribute(previewKey);
		request.getSession().removeAttribute(previewKey);

		request.setAttribute("task", task);
		request.setAttribute("submission", submission);
		request.setAttribute("answerClasses", answerClasses);
		request.setAttribute("results", results);
		request.setAttribute("runnable", runnable);
		if (preview instanceof Map<?, ?>) {
			request.setAttribute("answerClassPreview", preview);
		}
		getServletContext().getNamedDispatcher(ShowAnswerClassResultsView.class.getSimpleName()).forward(request, response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Session session = RequestAdapter.getSession(request);
		TaskDAOIf taskDAO = DAOFactory.TaskDAOIf(session);
		Task task = taskDAO.getTask(Util.parseInteger(request.getParameter("taskid"), 0));
		if (task == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			request.setAttribute("title", "Aufgabe nicht gefunden");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		}

		ParticipationDAOIf participationDAO = DAOFactory.ParticipationDAOIf(session);
		Participation participation = participationDAO.getParticipation(RequestAdapter.getUser(request), task.getTaskGroup().getLecture());
		if (participation == null) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "insufficient rights");
			return;
		}

		SubmissionDAOIf submissionDAO = DAOFactory.SubmissionDAOIf(session);
		Submission submission = submissionDAO.getSubmission(task, RequestAdapter.getUser(request));
		if (submission == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			request.setAttribute("title", "Keine Abgabe gefunden");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		}

		if (task.getDeadline().isBefore(ZonedDateTime.now()) || (task.isAllowPrematureSubmissionClosing() && submission.isClosed())) {
			request.setAttribute("title", "Ausführen bzw. Abruf des Ergebnisses nicht mehr möglich");
			request.setAttribute("message", "<div class=mid><a href=\"" + Util.generateHTMLLink(ShowTask.class.getSimpleName() + "?taskid=" + task.getTaskid(), response) + "\">zurück zur Aufgabe</a></div>");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		}

		AnswerClassRunCountDAOIf runCountDAO = DAOFactory.AnswerClassRunCountDAOIf(session);
		Transaction tx = session.beginTransaction();
		session.lock(submission, LockMode.PESSIMISTIC_WRITE);
		if (!runCountDAO.canSeeResultAndIncrementCounterTransaction(task, RequestAdapter.getUser(request))) {
			tx.commit();
			request.setAttribute("title", "Antwortklassen können nicht mehr ausgeführt werden. Limit erreicht.");
			request.setAttribute("message", "<div class=mid><a href=\"" + Util.generateHTMLLink(ShowTask.class.getSimpleName() + "?taskid=" + task.getTaskid(), response) + "\">zurück zur Aufgabe</a></div>");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		}
		List<PipelineResult> previewResults;
		try {
			previewResults = new AnswerClassRunner(session).preview(PipelineTrigger.STUDENT, submission, Configuration.getInstance().getDataPath());
			tx.commit();
		} catch (Exception e) {
			tx.rollback();
			LOG.error("Failed to run answer classes for submission {}", submission.getSubmissionid(), e);
			request.setAttribute("title", "Beim Ausführen der Antwortklassen ist ein Fehler aufgetreten.");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		}
		Map<Integer, AnswerClassResultStatus> previewStatuses = new HashMap<>();
		for (PipelineResult previewResult : previewResults) {
			previewStatuses.put(previewResult.getAnswerClassId(), previewResult.getStatus());
		}
		request.getSession().setAttribute(PREVIEW_SESSION_KEY + task.getTaskid(), previewStatuses);
		response.sendRedirect(Util.generateRedirectURL(PerformStudentAnswerClasses.class.getSimpleName() + "?taskid=" + task.getTaskid(), response));
	}
}
