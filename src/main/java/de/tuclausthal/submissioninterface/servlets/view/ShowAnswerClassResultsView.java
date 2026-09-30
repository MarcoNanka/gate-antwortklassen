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

package de.tuclausthal.submissioninterface.servlets.view;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import de.tuclausthal.submissioninterface.answerclasses.StudentAnswerClassVisibility;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.servlets.GATEView;
import de.tuclausthal.submissioninterface.servlets.controller.PerformStudentAnswerClasses;
import de.tuclausthal.submissioninterface.servlets.controller.ShowTask;
import de.tuclausthal.submissioninterface.servlets.view.fragments.ShowAnswerClassResult;
import de.tuclausthal.submissioninterface.template.Template;
import de.tuclausthal.submissioninterface.template.TemplateFactory;
import de.tuclausthal.submissioninterface.util.Util;

/**
 * Result page after a student asks for an answer-class run. That run is shown once and is not
 * stored. After the deadline this page shows batch results and tutor grades only.
 * Free-text {@code details} stay tutor-only.
 */
@GATEView
public class ShowAnswerClassResultsView extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Template template = TemplateFactory.getTemplate(request, response);

		Task task = (Task) request.getAttribute("task");
		Submission submission = (Submission) request.getAttribute("submission");
		@SuppressWarnings("unchecked")
		List<AnswerClass> answerClasses = (List<AnswerClass>) request.getAttribute("answerClasses");
		@SuppressWarnings("unchecked")
		List<AnswerClassResult> results = (List<AnswerClassResult>) request.getAttribute("results");
		int runnable = (Integer) request.getAttribute("runnable");

		Map<Integer, AnswerClassResult> resultByAnswerClassId = results.stream().collect(Collectors.toMap(result -> result.getAnswerClass().getId(), Function.identity()));
		boolean canRun = task.getDeadline().isAfter(ZonedDateTime.now()) && !(task.isAllowPrematureSubmissionClosing() && submission.isClosed());
		// The official (batch/automatic) results are only meaningful together with the grading,
		// so - like on the task page - they stay hidden until the task's Punktedatum
		// (task.getShowPoints()), not already right after the deadline.
		boolean pointsVisible = task.getShowPoints() != null && task.getShowPoints().isBefore(ZonedDateTime.now());
		@SuppressWarnings("unchecked")
		Map<Integer, AnswerClassResultStatus> preview = canRun ? (Map<Integer, AnswerClassResultStatus>) request.getAttribute("answerClassPreview") : null;

		template.printTemplateHeader(task);
		PrintWriter out = response.getWriter();

		out.println("<h2>Antwortklassen</h2>");
		List<AnswerClass> visibleAnswerClasses = answerClasses.stream().filter(answerClass -> answerClass.getTimesRunnableByStudents() > 0).collect(Collectors.toList());
		if (visibleAnswerClasses.isEmpty()) {
			out.println("<p>Für diese Aufgabe sind aktuell keine Antwortklassen für Studierende verfügbar.</p>");
		} else {
			if (preview != null || (!canRun && pointsVisible)) {
				out.println("<table>");
				out.println("<tr><th>Titel</th><th>Beschreibung</th><th>Ergebnis</th></tr>");
				for (AnswerClass answerClass : visibleAnswerClasses) {
					AnswerClassResultStatus status = preview != null ? preview.get(answerClass.getId()) : StudentAnswerClassVisibility.officialStatus(resultByAnswerClassId.get(answerClass.getId()));
					out.println("<tr>");
					out.println("<th>" + Util.escapeHTML(answerClass.getTitle()) + "</th>");
					out.println("<td>" + Util.textToHTML(answerClass.getDescription() == null ? "" : answerClass.getDescription()) + "</td>");
					out.println("<td>");
					out.println("<b>" + ShowAnswerClassResult.coloredStatusLabel(status) + "</b>");
					out.println("</td>");
					out.println("</tr>");
				}
				out.println("</table>");
			}

			// A fresh preview is for this submission as it stands. Running again from here would
			// spend another attempt on the same files, so the next run starts from the task page.
			if (preview == null && canRun && runnable > 0) {
				out.println("<form method=post action=\"" + Util.generateHTMLLink(PerformStudentAnswerClasses.class.getSimpleName() + "?taskid=" + task.getTaskid(), response) + "\"><input type=submit value=\"Antwortklassen ausführen\"></form>");
				out.println("<p>noch " + runnable + "x ausführbar</p>");
			} else if (preview == null && canRun) {
				out.println("<p>Limit erreicht</p>");
			}
		}
		out.println("<p><a href=\"" + Util.generateHTMLLink(ShowTask.class.getSimpleName() + "?taskid=" + task.getTaskid(), response) + "\">zurück zur Aufgabe</a></p>");

		template.printTemplateFooter();
	}
}
