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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hibernate.Transaction;
import org.junit.jupiter.api.Test;

import de.tuclausthal.submissioninterface.BasicTest;
import de.tuclausthal.submissioninterface.GATEDBTest;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.persistence.datamodel.User;

@GATEDBTest
class AnswerClassRunCountDAOIfTest extends BasicTest {
	@Test
	void testIncrementUntilLimit() {
		Transaction tx = session.beginTransaction();
		Task task = DAOFactory.TaskDAOIf(session).getTask(2);
		User user = DAOFactory.UserDAOIf(session).getUser(2);
		List<AnswerClass> answerClasses = DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(task);
		assertFalse(answerClasses.isEmpty());
		int limit = answerClasses.get(0).getTimesRunnableByStudents();
		assertTrue(limit > 0);

		assertEquals(limit, DAOFactory.AnswerClassRunCountDAOIf(session).canStillRunXTimes(task, user));
		for (int i = 0; i < limit; i++) {
			assertTrue(DAOFactory.AnswerClassRunCountDAOIf(session).canSeeResultAndIncrementCounterTransaction(task, user));
			assertEquals(limit - i - 1, DAOFactory.AnswerClassRunCountDAOIf(session).canStillRunXTimes(task, user));
		}
		assertFalse(DAOFactory.AnswerClassRunCountDAOIf(session).canSeeResultAndIncrementCounterTransaction(task, user));

		tx.rollback();
	}
}
