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

import org.hibernate.Transaction;
import org.junit.jupiter.api.Test;

import de.tuclausthal.submissioninterface.BasicTest;
import de.tuclausthal.submissioninterface.GATEDBTest;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.persistence.datamodel.User;

@GATEDBTest
class AnswerClassRunCountDAOIfTest extends BasicTest {
	@Test
	void testCanStillRunXTimesNoneConfigured() {
		assertEquals(0, DAOFactory.AnswerClassRunCountDAOIf(session).canStillRunXTimes(DAOFactory.TaskDAOIf(session).getTask(1), DAOFactory.UserDAOIf(session).getUser(3)));
	}

	@Test
	void testCanStillRunXTimesUnused() {
		assertEquals(2, DAOFactory.AnswerClassRunCountDAOIf(session).canStillRunXTimes(DAOFactory.TaskDAOIf(session).getTask(3), DAOFactory.UserDAOIf(session).getUser(3)));
	}

	@Test
	void testCanStillRunXTimesUsedAll() {
		assertEquals(0, DAOFactory.AnswerClassRunCountDAOIf(session).canStillRunXTimes(DAOFactory.TaskDAOIf(session).getTask(3), DAOFactory.UserDAOIf(session).getUser(8)));
	}

	@Test
	void testCanStillRunXTimesUsesMaxWhenSiblingsDiverge() {
		Transaction tx = session.beginTransaction();
		Task task = DAOFactory.TaskDAOIf(session).getTask(3);
		User user = DAOFactory.UserDAOIf(session).getUser(5);

		DAOFactory.AnswerClassDAOIf(session).getAnswerClassLocked(1).setTimesRunnableByStudents(2);
		DAOFactory.AnswerClassDAOIf(session).getAnswerClassLocked(2).setTimesRunnableByStudents(7);
		assertEquals(7, DAOFactory.AnswerClassRunCountDAOIf(session).canStillRunXTimes(task, user));

		tx.rollback();
	}

	@Test
	void testIncrementUntilLimit() {
		Transaction tx = session.beginTransaction();
		Task task = DAOFactory.TaskDAOIf(session).getTask(3);
		User user = DAOFactory.UserDAOIf(session).getUser(5);
		int limit = 2;

		assertEquals(limit, DAOFactory.AnswerClassRunCountDAOIf(session).canStillRunXTimes(task, user));
		for (int i = 0; i < limit; i++) {
			assertTrue(DAOFactory.AnswerClassRunCountDAOIf(session).canSeeResultAndIncrementCounterTransaction(task, user));
			assertEquals(limit - i - 1, DAOFactory.AnswerClassRunCountDAOIf(session).canStillRunXTimes(task, user));
		}
		assertFalse(DAOFactory.AnswerClassRunCountDAOIf(session).canSeeResultAndIncrementCounterTransaction(task, user));

		tx.rollback();
	}
}
