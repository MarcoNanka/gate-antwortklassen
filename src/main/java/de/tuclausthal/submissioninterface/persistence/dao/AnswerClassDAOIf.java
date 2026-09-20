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

package de.tuclausthal.submissioninterface.persistence.dao;

import java.util.List;

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.ProgrammingLanguage;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;

/**
 * Data Access Object Interface for the AnswerClass-class
 */
public interface AnswerClassDAOIf {
	AnswerClass createAnswerClass(Task task, String title, String description, String detectionType, int timesRunnableByStudents, boolean giveDetailsToStudents);

	AnswerClass getAnswerClass(int id);

	/**
	 * Like {@link #getAnswerClass(int)}, but with a pessimistic write lock (mirrors
	 * {@code TestDAOIf#getTestLocked}) — use before mutating (edit/delete) an answer class.
	 */
	AnswerClass getAnswerClassLocked(int id);

	List<AnswerClass> getAnswerClassesForTask(Task task);

	/**
	 * Writes the same student run budget onto every answer class of {@code task}. One student
	 * button runs all visible answer classes together, so the limit is shared (not per AC).
	 */
	void setTimesRunnableByStudentsForTask(Task task, int timesRunnableByStudents);

	/**
	 * Writes the same {@link ProgrammingLanguage} onto every answer class of {@code task}
	 * (task-wide setting maintained via the advisor answer-class form).
	 */
	void setProgrammingLanguageForTask(Task task, ProgrammingLanguage programmingLanguage);

	/**
	 * Resolves the shared programming language for {@code task} from its answer classes
	 * (defaults to {@link ProgrammingLanguage#JAVA} if none exist).
	 */
	ProgrammingLanguage getProgrammingLanguageForTask(Task task);

	void deleteAnswerClass(AnswerClass answerClass);

	/**
	 * Batch queue analogous to {@code TestDAOIf#takeTestTransacted()}: atomically claims (in its
	 * own transaction) one {@code Task} that has at least one answer class with
	 * {@code needsToRun == true} and whose deadline is more than 2 minutes in the past, and
	 * flips {@code needsToRun} to {@code false} for <b>all</b> of that task's answer classes
	 * (since one {@code AnswerClassRunner} pass evaluates all of a task's answer classes
	 * together for a given submission).
	 *
	 * @return the claimed task, or {@code null} if no task is currently due
	 */
	Task takeTaskForAnswerClassBatchTransacted();
}
