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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.hibernate.Session;

import de.tuclausthal.submissioninterface.answerclasses.java.JavaDetectionTypes;
import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.servlets.GATEView;
import de.tuclausthal.submissioninterface.servlets.RequestAdapter;
import de.tuclausthal.submissioninterface.servlets.controller.AnswerClassManager;
import de.tuclausthal.submissioninterface.servlets.controller.TaskManager;
import de.tuclausthal.submissioninterface.template.Template;
import de.tuclausthal.submissioninterface.template.TemplateFactory;
import de.tuclausthal.submissioninterface.util.Util;

/**
 * Advisor form to create or edit one {@link AnswerClass} (title, description, detectionType,
 * student run-limit/details, prerequisites). Mirrors {@code TestManagerAddTestFormView}.
 *
 * <p>Only the two built-in Java detectionTypes ({@link JavaDetectionTypes}) are offered right
 * now; once more languages/checkers exist, this is the place to make the option list
 * language-aware.</p>
 */
@GATEView
public class AnswerClassManagerFormView extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Template template = TemplateFactory.getTemplate(request, response);
		Session session = RequestAdapter.getSession(request);

		Task task = (Task) request.getAttribute("task");
		AnswerClass answerClass = (AnswerClass) request.getAttribute("answerClass"); // null when creating

		template.addKeepAlive();
		template.printEditTaskTemplateHeader(answerClass == null ? "Antwortklasse erstellen" : "Antwortklasse bearbeiten", task);
		PrintWriter out = response.getWriter();

		List<AnswerClass> allAnswerClasses = DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(task);
		Set<Integer> selectedPrerequisiteIds = answerClass == null ? Set.of() : answerClass.getPrerequisites().stream().map(AnswerClass::getId).collect(Collectors.toSet());

		out.println("<h2>Antwortklasse</h2>");
		out.println("<form action=\"" + Util.generateHTMLLink("?", response) + "\" method=post>");
		out.println("<input type=hidden name=taskid value=\"" + task.getTaskid() + "\">");
		out.println("<input type=hidden name=action value=saveAnswerClass>");
		if (answerClass != null) {
			out.println("<input type=hidden name=answerclassid value=\"" + answerClass.getId() + "\">");
		}
		out.println("<table>");
		out.println("<tr>");
		out.println("<th>Titel:</th>");
		out.println("<td><input type=text name=title value=\"" + Util.escapeHTML(answerClass == null ? "" : answerClass.getTitle()) + "\" required=required></td>");
		out.println("</tr>");
		out.println("<tr>");
		out.println("<th>Beschreibung:</th>");
		out.println("<td><textarea cols=60 rows=6 name=description>" + Util.escapeHTML(answerClass == null ? "" : answerClass.getDescription()) + "</textarea></td>");
		out.println("</tr>");
		out.println("<tr>");
		out.println("<th>Erkennungsart:</th>");
		out.println("<td><select name=detectionType size=1 required=required>");
		for (String detectionType : List.of(JavaDetectionTypes.COMPILE_OK, JavaDetectionTypes.WHILE_LOOP)) {
			boolean selected = answerClass != null && detectionType.equals(answerClass.getDetectionType());
			out.println("<option value=\"" + Util.escapeHTML(detectionType) + "\"" + (selected ? " selected" : "") + ">" + Util.escapeHTML(detectionType) + "</option>");
		}
		out.println("</select></td>");
		out.println("</tr>");
		out.println("<tr>");
		out.println("<th># ausführbar für Studierende:</th>");
		out.println("<td><input type=text name=timesRunnableByStudents value=\"" + (answerClass == null ? 3 : answerClass.getTimesRunnableByStudents()) + "\" required=required pattern=\"[0-9]+\"> (gilt gemeinsam für alle Antwortklassen dieser Aufgabe)</td>");
		out.println("</tr>");
		out.println("<tr>");
		out.println("<th>Studierenden Details anzeigen:</th>");
		out.println("<td><input type=checkbox name=giveDetailsToStudents" + (answerClass == null || answerClass.isGiveDetailsToStudents() ? " checked" : "") + "></td>");
		out.println("</tr>");
		if (!allAnswerClasses.isEmpty()) {
			out.println("<tr>");
			out.println("<th>Voraussetzungen:</th>");
			out.println("<td>");
			boolean any = false;
			for (AnswerClass candidate : allAnswerClasses) {
				if (answerClass != null && candidate.getId() == answerClass.getId()) {
					continue; // no self-prerequisite
				}
				any = true;
				String checked = selectedPrerequisiteIds.contains(candidate.getId()) ? " checked" : "";
				out.println("<input type=checkbox name=prerequisite value=\"" + candidate.getId() + "\" id=\"prereq" + candidate.getId() + "\"" + checked + "> <label for=\"prereq" + candidate.getId() + "\">" + Util.escapeHTML(candidate.getTitle()) + "</label><br>");
			}
			if (!any) {
				out.println("(keine weiteren Antwortklassen vorhanden)");
			}
			out.println("</td>");
			out.println("</tr>");
		}
		out.println("<tr>");
		out.print("<td colspan=2 class=mid><input type=submit value=speichern> <a href=\"");
		out.print(Util.generateHTMLLink(TaskManager.class.getSimpleName() + "?action=editTask&taskid=" + task.getTaskid() + "&lecture=" + task.getTaskGroup().getLecture().getId(), response));
		out.println("\">Abbrechen</a></td>");
		out.println("</tr>");
		out.println("</table>");
		out.println("</form>");
		if (answerClass != null) {
			out.println("<p><a onclick=\"return sendAsPost(this, 'Wirklich löschen?')\" href=\"" + Util.generateHTMLLink(AnswerClassManager.class.getSimpleName() + "?action=deleteAnswerClass&taskid=" + task.getTaskid() + "&answerclassid=" + answerClass.getId(), response) + "\">Antwortklasse löschen</a></p>");
		}
		template.printTemplateFooter();
	}
}
