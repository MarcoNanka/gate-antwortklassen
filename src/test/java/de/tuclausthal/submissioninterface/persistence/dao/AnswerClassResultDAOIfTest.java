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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hibernate.Transaction;
import org.junit.jupiter.api.Test;

import de.tuclausthal.submissioninterface.BasicTest;
import de.tuclausthal.submissioninterface.GATEDBTest;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.persistence.datamodel.User;

@GATEDBTest
class AnswerClassResultDAOIfTest extends BasicTest {
	@Test
	void testGetResultNone() {
		Transaction tx = session.beginTransaction();
		Task task = DAOFactory.TaskDAOIf(session).getTask(1);
		AnswerClass answerClass = DAOFactory.AnswerClassDAOIf(session).createAnswerClass(task, "Result Test AC", "", "WHILE_LOOP", 3, true);
		Submission submission = DAOFactory.SubmissionDAOIf(session).getSubmission(3);
		assertNull(DAOFactory.AnswerClassResultDAOIf(session).getResult(answerClass, submission));
		tx.rollback();
	}

	@Test
	void testStoreResultAndOverwrite() {
		Transaction tx = session.beginTransaction();
		Task task = DAOFactory.TaskDAOIf(session).getTask(1);
		AnswerClass answerClass = DAOFactory.AnswerClassDAOIf(session).createAnswerClass(task, "Result Overwrite AC", "", "WHILE_LOOP", 3, true);
		Submission submission = DAOFactory.SubmissionDAOIf(session).getSubmission(3);

		DAOFactory.AnswerClassResultDAOIf(session).storeResult(answerClass, submission, AnswerClassResultStatus.MATCHED, "first", AnswerClassResultSource.STUDENT_PREVIEW, null);
		AnswerClassResult first = DAOFactory.AnswerClassResultDAOIf(session).getResult(answerClass, submission);
		assertNotNull(first);
		assertEquals(AnswerClassResultStatus.MATCHED, first.getStatus());
		assertEquals("first", first.getDetails());
		assertEquals(AnswerClassResultSource.STUDENT_PREVIEW, first.getSource());
		assertNull(first.getEvaluatedBy());
		int resultId = first.getId();

		User tutor = DAOFactory.UserDAOIf(session).getUser(3);
		DAOFactory.AnswerClassResultDAOIf(session).storeResult(answerClass, submission, AnswerClassResultStatus.NOT_MATCHED, "second", AnswerClassResultSource.MANUAL, tutor);
		AnswerClassResult second = DAOFactory.AnswerClassResultDAOIf(session).getResult(answerClass, submission);
		assertNotNull(second);
		assertEquals(resultId, second.getId());
		assertEquals(AnswerClassResultStatus.NOT_MATCHED, second.getStatus());
		assertEquals("second", second.getDetails());
		assertEquals(AnswerClassResultSource.MANUAL, second.getSource());
		assertEquals(tutor.getUid(), second.getEvaluatedBy().getUid());
		assertNotNull(second.getCheckedAt());

		tx.rollback();
	}
}
