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
import java.util.List;

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
import de.tuclausthal.submissioninterface.answerclasses.PipelineTrigger;
import de.tuclausthal.submissioninterface.persistence.dao.AnswerClassResultDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.AnswerClassRunCountDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.dao.ParticipationDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.SubmissionDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.TaskDAOIf;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult;
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
 * Controller-Servlet for students requesting an answer-class run on their own submission
 * (trigger {@code STUDENT}, result source {@code STUDENT_PREVIEW}).
 *
 * <p>Mirrors {@code PerformStudentTest}, but runs synchronously (no background executor/polling
 * needed — unlike JUnit-based tests, the Java checkers here are lightweight compile+AST checks)
 * and follows the same POST-triggers-then-redirect-to-GET (PRG) pattern so a page reload never
 * re-triggers a run.</p>
 *
 * <p>Per {@code .cursor/rules/antwortklassen.mdc}: the student run-limit check
 * ({@code AnswerClassRunCountDAOIf}) lives here in the web controller, not in
 * {@code AnswerClassPipeline}.</p>
 */
@GATEController
public class PerformStudentAnswerClasses extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

		request.setAttribute("task", task);
		request.setAttribute("submission", submission);
		request.setAttribute("answerClasses", answerClasses);
		request.setAttribute("results", results);
		request.setAttribute("runnable", runnable);
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
		try {
			new AnswerClassRunner(session).run(PipelineTrigger.STUDENT, submission, Configuration.getInstance().getDataPath());
			tx.commit();
		} catch (Exception e) {
			tx.rollback();
			LOG.error("Failed to run answer classes for submission {}", submission.getSubmissionid(), e);
			request.setAttribute("title", "Beim Ausführen der Antwortklassen ist ein Fehler aufgetreten.");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
			return;
		}
		response.sendRedirect(Util.generateRedirectURL(PerformStudentAnswerClasses.class.getSimpleName() + "?taskid=" + task.getTaskid(), response));
	}
}
