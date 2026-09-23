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
import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.hibernate.LockMode;
import org.hibernate.Session;

import de.tuclausthal.submissioninterface.answerclasses.AnswerClassToEvaluate;
import de.tuclausthal.submissioninterface.answerclasses.java.JavaDetectionTypes;
import de.tuclausthal.submissioninterface.persistence.dao.AnswerClassDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.dao.ParticipationDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.TaskDAOIf;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.Participation;
import de.tuclausthal.submissioninterface.persistence.datamodel.ParticipationRole;
import de.tuclausthal.submissioninterface.persistence.datamodel.ProgrammingLanguage;
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
			String functionTestCode = request.getParameter("functionTestCode");
			String functionTestExpectedOutput = request.getParameter("functionTestExpectedOutput");
			int answerClassId = Util.parseInteger(request.getParameter("answerclassid"), 0);
			boolean creating = answerClassId <= 0;
			List<AnswerClass> existingAnswerClasses = answerClassDAO.getAnswerClassesForTask(task);
			// Same rule as the form: language is choosable only while the task has at most one AC.
			boolean languageLocked = existingAnswerClasses.size() >= 2 || (creating && !existingAnswerClasses.isEmpty());
			ProgrammingLanguage programmingLanguage = languageLocked ? answerClassDAO.getProgrammingLanguageForTask(task) : ProgrammingLanguage.fromParam(request.getParameter("programmingLanguage"));
			int timesRunnableByStudents = Util.parseInteger(request.getParameter("timesRunnableByStudents"), 0);
			// Details stay in DB for tutors/debugging but are never shown to students (no advisor toggle).
			boolean giveDetailsToStudents = false;
			// MANUAL is a language-neutral pipeline concept (see AnswerClassToEvaluate / AnswerClassPipeline):
			// it always resolves to PENDING for the tutor to grade, without ever consulting a checker
			// registry or LanguageProvider artifacts - so it is valid regardless of programmingLanguage.
			boolean isManual = AnswerClassToEvaluate.MANUAL_DETECTION_TYPE.equals(detectionType);
			if (!isManual && programmingLanguage == ProgrammingLanguage.JAVA && !JavaDetectionTypes.isKnown(detectionType)) {
				forwardFormValidationError(request, response, task, "Ungültige Erkennungsart");
				return;
			}

			// only meaningful for detectionType WHILE_LOOP; whileLoopMax blank means "unbounded"
			int whileLoopMin = Util.parseInteger(request.getParameter("whileLoopMin"), 1);
			String whileLoopMaxParam = request.getParameter("whileLoopMax");
			Integer whileLoopMax = (whileLoopMaxParam == null || whileLoopMaxParam.isBlank()) ? null : Util.parseInteger(whileLoopMaxParam, -1);
			if (whileLoopMin < 0 || (whileLoopMax != null && (whileLoopMax < 0 || whileLoopMax < whileLoopMin))) {
				forwardFormValidationError(request, response, task, "Ungültiger Bereich für die Anzahl while-Schleifen");
				return;
			}
			// only meaningful for detectionType FOR_LOOP; forLoopMax blank means "unbounded"
			int forLoopMin = Util.parseInteger(request.getParameter("forLoopMin"), 1);
			String forLoopMaxParam = request.getParameter("forLoopMax");
			Integer forLoopMax = (forLoopMaxParam == null || forLoopMaxParam.isBlank()) ? null : Util.parseInteger(forLoopMaxParam, -1);
			if (forLoopMin < 0 || (forLoopMax != null && (forLoopMax < 0 || forLoopMax < forLoopMin))) {
				forwardFormValidationError(request, response, task, "Ungültiger Bereich für die Anzahl for-Schleifen");
				return;
			}

			session.beginTransaction();
			AnswerClass answerClass;
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
				answerClass.setGiveDetailsToStudents(giveDetailsToStudents);
			} else {
				answerClass = answerClassDAO.createAnswerClass(task, title, description, detectionType, timesRunnableByStudents, giveDetailsToStudents);
			}
			// only meaningful for the matching detectionType, but stored regardless (like description)
			answerClass.setFunctionTestCode(functionTestCode);
			answerClass.setFunctionTestExpectedOutput(functionTestExpectedOutput);
			answerClass.setWhileLoopMin(whileLoopMin);
			answerClass.setWhileLoopMax(whileLoopMax);
			answerClass.setForLoopMin(forLoopMin);
			answerClass.setForLoopMax(forLoopMax);

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
			// one student button / one batch pass evaluates all ACs of a task together → shared
			// run budget and programming language must stay identical on every sibling
			answerClassDAO.setTimesRunnableByStudentsForTask(task, timesRunnableByStudents);
			answerClassDAO.setProgrammingLanguageForTask(task, programmingLanguage);
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

	/**
	 * Validation failed while saving; keep the advisor on the form (browser restores the
	 * entered values) or let them leave back to the task editor. The default MessageView
	 * link would otherwise send them to the lecture overview.
	 */
	private void forwardFormValidationError(HttpServletRequest request, HttpServletResponse response, Task task, String title) throws ServletException, IOException {
		String cancel = Util.generateHTMLLink(TaskManager.class.getSimpleName() + "?action=editTask&taskid=" + task.getTaskid() + "&lecture=" + task.getTaskGroup().getLecture().getId(), response);
		request.setAttribute("title", title);
		request.setAttribute("message", "<div class=mid><a href=\"javascript:window.history.back();\">zurück zur Antwortklasse</a> - <a href=\"" + cancel + "\">Abbrechen</a></div>");
		getServletContext().getNamedDispatcher(MessageView.class.getSimpleName()).forward(request, response);
	}
}
