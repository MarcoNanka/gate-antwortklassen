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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tuclausthal.submissioninterface.answerclasses.AnswerClassRunner;
import de.tuclausthal.submissioninterface.answerclasses.PipelineTrigger;
import de.tuclausthal.submissioninterface.persistence.dao.AnswerClassDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.dao.ParticipationDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.SubmissionDAOIf;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;
import de.tuclausthal.submissioninterface.persistence.datamodel.Participation;
import de.tuclausthal.submissioninterface.persistence.datamodel.ParticipationRole;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.servlets.GATEController;
import de.tuclausthal.submissioninterface.servlets.RequestAdapter;
import de.tuclausthal.submissioninterface.servlets.view.MessageView;
import de.tuclausthal.submissioninterface.util.Configuration;
import de.tuclausthal.submissioninterface.util.Util;

/**
 * Controller-Servlet for tutors on one submission's answer classes:
 * <ul>
 * <li>no {@code answerclassid} parameter: run the full pipeline (trigger {@code TUTOR}) on the
 * given submission - {@code AnswerClassPipeline#sourceForTrigger} already maps that to result
 * source {@code MANUAL}, so no extra mapping is needed here;</li>
 * <li>{@code answerclassid} + {@code status} parameters: manually set/override one answer
 * class's result (e.g. resolve a {@code PENDING} manual answer class), source
 * {@code MANUAL}, {@code evaluatedBy} the current tutor.</li>
 * </ul>
 * Mirrors the "Test auf Abgabe ausführen"-form from {@code PerformTest}, but - like
 * {@code AnswerClassRunner} in general - does not touch the {@code Test}/{@code TestExecutor}
 * hierarchy.
 */
@GATEController
public class PerformTutorAnswerClasses extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
		} else {
			session.beginTransaction();
			try {
				new AnswerClassRunner(session).run(PipelineTrigger.TUTOR, submission, Configuration.getInstance().getDataPath());
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				LOG.error("Failed to run answer classes for submission {}", submission.getSubmissionid(), e);
				request.setAttribute("title", "Beim Ausführen der Antwortklassen ist ein Fehler aufgetreten.");
				getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
				return;
			}
		}

		response.sendRedirect(Util.generateRedirectURL(ShowSubmission.class.getSimpleName() + "?sid=" + submission.getSubmissionid(), response));
	}
}
