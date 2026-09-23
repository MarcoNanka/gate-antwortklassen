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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.hibernate.Session;

import de.tuclausthal.submissioninterface.answerclasses.AnswerClassToEvaluate;
import de.tuclausthal.submissioninterface.answerclasses.java.JavaDetectionTypes;
import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.ProgrammingLanguage;
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
 * shared student run-limit, prerequisites). Mirrors {@code TestManagerAddTestFormView}.
 *
 * <p>The student run budget is shared across all answer classes of the task: the field is
 * prefilled from the siblings and saving writes the value onto every AC of the task.</p>
 *
 * <p>The built-in Java detectionTypes ({@link JavaDetectionTypes}) are offered based on the
 * task's programming language; once more languages/checkers exist, this is the place to make
 * that part of the option list language-aware. The language-neutral
 * {@link AnswerClassToEvaluate#MANUAL_DETECTION_TYPE} (tutor grades manually, no checker/artifact
 * involved - see {@code AnswerClassPipeline}) is always offered in addition.</p>
 *
 * <p>Result {@code details} are stored for tutors but never shown to students; there is no
 * advisor toggle for that (unlike classic tests).</p>
 *
 * <p>The programming language is shared by every answer class of the task. It stays editable
 * while the task has at most one answer class; once a second exists, the dropdown is disabled
 * on every form and the controller keeps the stored language.</p>
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
		// shared budget / language for the whole task (prefill from siblings; defaults when none exist yet)
		int sharedTimesRunnableByStudents = allAnswerClasses.isEmpty() ? 3 : allAnswerClasses.stream().mapToInt(AnswerClass::getTimesRunnableByStudents).max().orElse(3);
		ProgrammingLanguage sharedProgrammingLanguage = DAOFactory.AnswerClassDAOIf(session).getProgrammingLanguageForTask(task);
		// Editable only for the first answer class (create, or edit while it is the only one).
		boolean languageLocked = allAnswerClasses.size() >= 2 || (answerClass == null && !allAnswerClasses.isEmpty());

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
		out.println("<th>Programmiersprache:</th>");
		out.println("<td>");
		if (languageLocked) {
			// Disabled controls are omitted from the POST; the hidden field keeps the stored language.
			out.println("<input type=hidden name=programmingLanguage value=\"" + Util.escapeHTML(sharedProgrammingLanguage.name()) + "\">");
			out.println("<select size=1 disabled>");
			out.println("<option selected>" + Util.escapeHTML(sharedProgrammingLanguage.getDisplayName()) + "</option>");
			out.println("</select> <em>Es kann nur eine gemeinsame Sprachumgebung für alle Antwortklassen einer Aufgabe geben.</em>");
		} else {
			// Only languages with an implemented LanguageProvider are listed (currently JAVA only).
			out.println("<select name=programmingLanguage size=1 required=required>");
			for (ProgrammingLanguage language : ProgrammingLanguage.values()) {
				boolean selected = language == sharedProgrammingLanguage;
				out.println("<option value=\"" + Util.escapeHTML(language.name()) + "\"" + (selected ? " selected" : "") + ">" + Util.escapeHTML(language.getDisplayName()) + "</option>");
			}
			out.println("</select> <em>gilt für alle Antwortklassen dieser Aufgabe</em>");
		}
		out.println("</td>");
		out.println("</tr>");
		out.println("<tr>");
		out.println("<th>Erkennungsart:</th>");
		out.println("<td><select name=detectionType id=detectionType size=1 required=required onchange=\"gateAnswerClassUpdateFields()\">");
		if (answerClass == null) {
			out.println("<option value=\"\" disabled selected>bitte wählen</option>");
		}
		List<String> detectionTypes = new ArrayList<>();
		if (sharedProgrammingLanguage == ProgrammingLanguage.JAVA) {
			detectionTypes.addAll(List.of(JavaDetectionTypes.COMPILE_OK, JavaDetectionTypes.WHILE_LOOP, JavaDetectionTypes.FOR_LOOP, JavaDetectionTypes.FUNCTION_TEST));
		}
		// MANUAL is a language-neutral pipeline concept (see AnswerClassToEvaluate), not a Java checker,
		// so it is always offered regardless of the task's programming language.
		detectionTypes.add(AnswerClassToEvaluate.MANUAL_DETECTION_TYPE);
		for (String detectionType : detectionTypes) {
			boolean selected = answerClass != null && detectionType.equals(answerClass.getDetectionType());
			String label = AnswerClassToEvaluate.MANUAL_DETECTION_TYPE.equals(detectionType) ? "Manuell (durch Tutor bewertet)" : JavaDetectionTypes.displayName(detectionType);
			out.println("<option value=\"" + Util.escapeHTML(detectionType) + "\"" + (selected ? " selected" : "") + ">" + Util.escapeHTML(label) + "</option>");
		}
		out.println("</select> <em>legt fest, was geprüft wird</em> (Titel/Beschreibung sind nur Anzeige)</td>");
		out.println("</tr>");
		out.println("<tr data-detectiontype=\"" + JavaDetectionTypes.WHILE_LOOP + "\">");
		out.println("<th>while-Schleifen - Anzahl:</th>");
		out.println("<td>mind. <input type=text name=whileLoopMin value=\"" + (answerClass == null ? 1 : answerClass.getWhileLoopMin()) + "\" size=3 pattern=\"[0-9]+\"> max. <input type=text name=whileLoopMax value=\"" + (answerClass != null && answerClass.getWhileLoopMax() != null ? answerClass.getWhileLoopMax() : "") + "\" size=3 pattern=\"[0-9]*\"> <em>max. leer lassen für „unbegrenzt“; z.B. mind.=max.=1 für „genau eine“, max.=0 für „keine“</em></td>");
		out.println("</tr>");
		out.println("<tr data-detectiontype=\"" + JavaDetectionTypes.FOR_LOOP + "\">");
		out.println("<th>for-Schleifen - Anzahl:</th>");
		out.println("<td>mind. <input type=text name=forLoopMin value=\"" + (answerClass == null ? 1 : answerClass.getForLoopMin()) + "\" size=3 pattern=\"[0-9]+\"> max. <input type=text name=forLoopMax value=\"" + (answerClass != null && answerClass.getForLoopMax() != null ? answerClass.getForLoopMax() : "") + "\" size=3 pattern=\"[0-9]*\"> <em>max. leer lassen für „unbegrenzt“; z.B. mind.=max.=1 für „genau eine“, max.=0 für „keine“ (zählt nur klassische for-Schleifen, keine for-each)</em></td>");
		out.println("</tr>");
		out.println("<tr data-detectiontype=\"" + JavaDetectionTypes.FUNCTION_TEST + "\">");
		out.println("<th>Funktionstest - Testcode:</th>");
		out.println("<td><textarea cols=60 rows=6 name=functionTestCode placeholder=\"z.B. System.out.println(Hello.greet());\">" + Util.escapeHTML(answerClass == null ? "" : answerClass.getFunctionTestCode()) + "</textarea> <em>wird in eine main()-Methode eingesetzt und gegen die Abgabe ausgeführt</em></td>");
		out.println("</tr>");
		out.println("<tr data-detectiontype=\"" + JavaDetectionTypes.FUNCTION_TEST + "\">");
		out.println("<th>Funktionstest - erwartete Ausgabe:</th>");
		out.println("<td><textarea cols=60 rows=4 name=functionTestExpectedOutput>" + Util.escapeHTML(answerClass == null ? "" : answerClass.getFunctionTestExpectedOutput()) + "</textarea> <em>wird getrimmt mit der tatsächlichen Ausgabe (stdout) verglichen</em></td>");
		out.println("</tr>");
		out.println("<tr>");
		out.println("<th># ausführbar für Studierende:</th>");
		out.println("<td><input type=text name=timesRunnableByStudents value=\"" + sharedTimesRunnableByStudents + "\" required=required pattern=\"[0-9]+\"> <em>gemeinsames Limit für alle Antwortklassen dieser Aufgabe</em> (Änderung beim Speichern wird auf alle übernommen)</td>");
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
		out.println("<script>");
		out.println("function gateAnswerClassUpdateFields() {");
		out.println("	var select = document.getElementById('detectionType');");
		out.println("	var type = select ? select.value : '';");
		out.println("	var rows = document.querySelectorAll('tr[data-detectiontype]');");
		out.println("	for (var i = 0; i < rows.length; i++) {");
		out.println("		rows[i].style.display = (rows[i].getAttribute('data-detectiontype') === type) ? '' : 'none';");
		out.println("	}");
		out.println("}");
		out.println("gateAnswerClassUpdateFields();");
		out.println("</script>");
		out.println("</form>");
		if (answerClass != null) {
			out.println("<p><a onclick=\"return sendAsPost(this, 'Wirklich löschen?')\" href=\"" + Util.generateHTMLLink(AnswerClassManager.class.getSimpleName() + "?action=deleteAnswerClass&taskid=" + task.getTaskid() + "&answerclassid=" + answerClass.getId(), response) + "\">Antwortklasse löschen</a></p>");
		}
		template.printTemplateFooter();
	}
}
