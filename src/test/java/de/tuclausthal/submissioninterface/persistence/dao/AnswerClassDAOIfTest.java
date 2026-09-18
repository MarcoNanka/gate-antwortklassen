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
	void testGetAnswerClassesForTaskNone() {
		assertEquals(0, DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(DAOFactory.TaskDAOIf(session).getTask(1)).size());
	}

	@Test
	void testGetAnswerClassesForTask() {
		List<AnswerClass> answerClasses = DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(DAOFactory.TaskDAOIf(session).getTask(3));
		assertEquals(2, answerClasses.size());
		assertEquals(1, answerClasses.get(0).getId());
		assertEquals("Code compiles", answerClasses.get(0).getTitle());
		assertEquals("COMPILE_OK", answerClasses.get(0).getDetectionType());
		assertEquals(2, answerClasses.get(0).getTimesRunnableByStudents());
		assertTrue(answerClasses.get(0).isGiveDetailsToStudents());
		assertEquals(2, answerClasses.get(1).getId());
		assertEquals("While loop", answerClasses.get(1).getTitle());
		assertEquals("WHILE_LOOP", answerClasses.get(1).getDetectionType());
	}

	@Test
	void testGetAnswerClassesForTaskLoadsPrerequisites() {
		List<AnswerClass> answerClasses = DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(DAOFactory.TaskDAOIf(session).getTask(3));

		AnswerClass compileOk = answerClasses.get(0);
		assertTrue(compileOk.getPrerequisites().isEmpty());

		AnswerClass whileLoop = answerClasses.get(1);
		assertEquals(1, whileLoop.getPrerequisites().size());
		assertEquals(compileOk.getId(), whileLoop.getPrerequisites().iterator().next().getId());
	}

	@Test
	void testGetAnswerClass() {
		AnswerClass answerClass = DAOFactory.AnswerClassDAOIf(session).getAnswerClass(1);
		assertNotNull(answerClass);
		assertEquals("Code compiles", answerClass.getTitle());
		assertEquals(3, answerClass.getTask().getTaskid());
		assertNull(DAOFactory.AnswerClassDAOIf(session).getAnswerClass(9999));
	}

	@Test
	void testCreateDeleteAnswerClass() {
		Transaction tx = session.beginTransaction();
		Task task = DAOFactory.TaskDAOIf(session).getTask(3);
		List<AnswerClass> before = DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(task);

		AnswerClass created = DAOFactory.AnswerClassDAOIf(session).createAnswerClass(task, "DAO Test AC", "desc", "COMPILE_OK", 3, true);
		assertTrue(created.getId() > 0);
		assertFalse(before.contains(created));
		assertEquals(before.size() + 1, DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(task).size());
		assertTrue(DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(task).contains(created));

		DAOFactory.AnswerClassDAOIf(session).deleteAnswerClass(created);
		assertNull(DAOFactory.AnswerClassDAOIf(session).getAnswerClass(created.getId()));
		assertEquals(before.size(), DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(task).size());

		tx.rollback();
	}
}
