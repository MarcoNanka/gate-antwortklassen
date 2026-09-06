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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hibernate.Transaction;
import org.junit.jupiter.api.Test;

import de.tuclausthal.submissioninterface.BasicTest;
import de.tuclausthal.submissioninterface.GATEDBTest;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;

@GATEDBTest
class AnswerClassDAOIfTest extends BasicTest {
	@Test
	void testCreateGetDeleteAnswerClass() {
		Transaction tx = session.beginTransaction();
		Task task = DAOFactory.TaskDAOIf(session).getTask(1);
		List<AnswerClass> before = DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(task);

		AnswerClass created = DAOFactory.AnswerClassDAOIf(session).createAnswerClass(task, "DAO Test AC", "desc", "UNUSED_VARIABLE", 3, true);
		assertTrue(created.getId() > 0);
		assertEquals(before.size() + 1, DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(task).size());

		AnswerClass loaded = DAOFactory.AnswerClassDAOIf(session).getAnswerClass(created.getId());
		assertNotNull(loaded);
		assertEquals("DAO Test AC", loaded.getTitle());
		assertEquals("UNUSED_VARIABLE", loaded.getDetectionType());
		assertEquals(3, loaded.getTimesRunnableByStudents());
		assertTrue(loaded.isGiveDetailsToStudents());

		DAOFactory.AnswerClassDAOIf(session).deleteAnswerClass(loaded);
		assertNull(DAOFactory.AnswerClassDAOIf(session).getAnswerClass(created.getId()));
		assertEquals(before.size(), DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(task).size());

		tx.rollback();
	}

	@Test
	void testGetAnswerClassesForTaskOrdered() {
		List<AnswerClass> answerClasses = DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(DAOFactory.TaskDAOIf(session).getTask(1));
		assertFalse(answerClasses.isEmpty());
		for (int i = 1; i < answerClasses.size(); i++) {
			assertTrue(answerClasses.get(i - 1).getId() <= answerClasses.get(i).getId());
		}
	}
}
