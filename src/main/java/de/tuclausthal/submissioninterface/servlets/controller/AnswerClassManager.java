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
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.hibernate.LockMode;
import org.hibernate.Session;

import de.tuclausthal.submissioninterface.persistence.dao.AnswerClassDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.dao.ParticipationDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.TaskDAOIf;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.Participation;
import de.tuclausthal.submissioninterface.persistence.datamodel.ParticipationRole;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.servlets.GATEController;
import de.tuclausthal.submissioninterface.servlets.RequestAdapter;
import de.tuclausthal.submissioninterface.servlets.view.AnswerClassManagerFormView;
import de.tuclausthal.submissioninterface.servlets.view.MessageView;
import de.tuclausthal.submissioninterface.util.Util;

/**
 * Controller-Servlet for managing (add, edit, delete, rerun) answer classes by advisors.
 * Mirrors {@code TestManager}: kept separate from the {@code Test}/{@code TestExecutor}
 * hierarchy.
 */
@GATEController
public class AnswerClassManager extends HttpServlet {
	private static final long serialVersionUID = 1L;

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
		if (participation == null || participation.getRoleType() != ParticipationRole.ADVISOR) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "insufficient rights");
			return;
		}

		if ("newAnswerClass".equals(request.getParameter("action"))) {
			request.setAttribute("task", task);
			getServletContext().getNamedDispatcher(AnswerClassManagerFormView.class.getSimpleName()).forward(request, response);
		} else if ("editAnswerClass".equals(request.getParameter("action"))) {
			AnswerClassDAOIf answerClassDAO = DAOFactory.AnswerClassDAOIf(session);
			AnswerClass answerClass = answerClassDAO.getAnswerClass(Util.parseInteger(request.getParameter("answerclassid"), 0));
			if (answerClass == null || answerClass.getTask().getTaskid() != task.getTaskid()) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				request.setAttribute("title", "Antwortklasse nicht gefunden");
				getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
				return;
			}
			request.setAttribute("task", task);
			request.setAttribute("answerClass", answerClass);
			getServletContext().getNamedDispatcher(AnswerClassManagerFormView.class.getSimpleName()).forward(request, response);
		} else {
			request.setAttribute("title", "Ungültiger Aufruf");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
		}
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
		if (participation == null || participation.getRoleType() != ParticipationRole.ADVISOR) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "insufficient rights");
			return;
		}

		AnswerClassDAOIf answerClassDAO = DAOFactory.AnswerClassDAOIf(session);

		if ("saveAnswerClass".equals(request.getParameter("action"))) {
			String title = request.getParameter("title");
			String description = request.getParameter("description");
			String detectionType = request.getParameter("detectionType");
			int timesRunnableByStudents = Util.parseInteger(request.getParameter("timesRunnableByStudents"), 0);
			boolean giveDetailsToStudents = request.getParameter("giveDetailsToStudents") != null;

			session.beginTransaction();
			AnswerClass answerClass;
			int answerClassId = Util.parseInteger(request.getParameter("answerclassid"), 0);
			if (answerClassId > 0) {
				answerClass = answerClassDAO.getAnswerClassLocked(answerClassId);
				if (answerClass == null || answerClass.getTask().getTaskid() != task.getTaskid()) {
					session.getTransaction().rollback();
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
					request.setAttribute("title", "Antwortklasse nicht gefunden");
					getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
					return;
				}
				answerClass.setTitle(title);
				answerClass.setDescription(description);
				answerClass.setDetectionType(detectionType);
				answerClass.setTimesRunnableByStudents(timesRunnableByStudents);
				answerClass.setGiveDetailsToStudents(giveDetailsToStudents);
			} else {
				answerClass = answerClassDAO.createAnswerClass(task, title, description, detectionType, timesRunnableByStudents, giveDetailsToStudents);
			}

			Set<AnswerClass> prerequisites = new HashSet<>();
			String[] prerequisiteIds = request.getParameterValues("prerequisite");
			if (prerequisiteIds != null) {
				for (String prerequisiteId : prerequisiteIds) {
					int id = Util.parseInteger(prerequisiteId, 0);
					if (id == answerClass.getId()) {
						continue; // no self-prerequisite
					}
					AnswerClass prerequisite = answerClassDAO.getAnswerClass(id);
					if (prerequisite != null && prerequisite.getTask().getTaskid() == task.getTaskid()) {
						prerequisites.add(prerequisite);
					}
				}
			}
			answerClass.setPrerequisites(prerequisites);
			// every answer class of a task is evaluated together in one AnswerClassRunner pass,
			// so a create/edit should be picked up again by the next batch run
			for (AnswerClass sibling : answerClassDAO.getAnswerClassesForTask(task)) {
				sibling.setNeedsToRun(true);
			}
			session.getTransaction().commit();
			response.sendRedirect(Util.generateRedirectURL(TaskManager.class.getSimpleName() + "?action=editTask&lecture=" + task.getTaskGroup().getLecture().getId() + "&taskid=" + task.getTaskid(), response));
		} else if ("deleteAnswerClass".equals(request.getParameter("action"))) {
			session.beginTransaction();
			AnswerClass answerClass = answerClassDAO.getAnswerClassLocked(Util.parseInteger(request.getParameter("answerclassid"), 0));
			if (answerClass != null && answerClass.getTask().getTaskid() == task.getTaskid()) {
				answerClassDAO.deleteAnswerClass(answerClass);
			}
			session.getTransaction().commit();
			response.sendRedirect(Util.generateRedirectURL(TaskManager.class.getSimpleName() + "?action=editTask&lecture=" + task.getTaskGroup().getLecture().getId() + "&taskid=" + task.getTaskid(), response));
		} else if ("rerunAnswerClasses".equals(request.getParameter("action"))) {
			session.beginTransaction();
			for (AnswerClass answerClass : answerClassDAO.getAnswerClassesForTask(task)) {
				session.lock(answerClass, LockMode.PESSIMISTIC_WRITE);
				answerClass.setNeedsToRun(true);
			}
			session.getTransaction().commit();
			response.sendRedirect(Util.generateRedirectURL(TaskManager.class.getSimpleName() + "?action=editTask&lecture=" + task.getTaskGroup().getLecture().getId() + "&taskid=" + task.getTaskid(), response));
		} else {
			request.setAttribute("title", "Ungültiger Aufruf");
			getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
		}
	}
}
