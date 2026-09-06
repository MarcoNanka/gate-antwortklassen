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
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;

/**
 * Data Access Object Interface for the AnswerClass-class
 */
public interface AnswerClassDAOIf {
	AnswerClass createAnswerClass(Task task, String title, String description, String detectionType, int timesRunnableByStudents, boolean giveDetailsToStudents);

	AnswerClass getAnswerClass(int id);

	List<AnswerClass> getAnswerClassesForTask(Task task);

	void deleteAnswerClass(AnswerClass answerClass);
}
