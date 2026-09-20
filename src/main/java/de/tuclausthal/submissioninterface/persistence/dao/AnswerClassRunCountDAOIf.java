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

import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.persistence.datamodel.User;

/**
 * Data Access Object Interface for answer class run counts (per task and user).
 */
public interface AnswerClassRunCountDAOIf {
	/**
	 * Increments the counter if the student may still request answer classes for the task.
	 * The limit is the shared student run budget of the task ({@code timesRunnableByStudents}
	 * on its answer classes; kept in sync by {@code AnswerClassManager}).
	 * @return true if the request is allowed and the counter was incremented
	 */
	boolean canSeeResultAndIncrementCounterTransaction(Task task, User user);

	/**
	 * @return how many times the student may still request answer classes for the task
	 */
	int canStillRunXTimes(Task task, User user);
}
