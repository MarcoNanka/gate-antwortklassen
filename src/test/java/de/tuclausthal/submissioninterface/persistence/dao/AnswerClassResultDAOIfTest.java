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
import de.tuclausthal.submissioninterface.persistence.datamodel.User;

@GATEDBTest
class AnswerClassResultDAOIfTest extends BasicTest {
	@Test
	void testGetResultNone() {
		AnswerClass answerClass = DAOFactory.AnswerClassDAOIf(session).getAnswerClass(2);
		Submission submission = DAOFactory.SubmissionDAOIf(session).getSubmission(7);
		assertNull(DAOFactory.AnswerClassResultDAOIf(session).getResult(answerClass, submission));
	}

	@Test
	void testGetResultMatched() {
		AnswerClassResult result = DAOFactory.AnswerClassResultDAOIf(session).getResult(DAOFactory.AnswerClassDAOIf(session).getAnswerClass(1), DAOFactory.SubmissionDAOIf(session).getSubmission(3));
		assertNotNull(result);
		assertEquals(1, result.getId());
		assertEquals(AnswerClassResultStatus.MATCHED, result.getStatus());
		assertEquals("unused variable found", result.getDetails());
		assertEquals(AnswerClassResultSource.AUTOMATIC, result.getSource());
		assertNull(result.getEvaluatedBy());
	}

	@Test
	void testGetResultManual() {
		AnswerClassResult result = DAOFactory.AnswerClassResultDAOIf(session).getResult(DAOFactory.AnswerClassDAOIf(session).getAnswerClass(2), DAOFactory.SubmissionDAOIf(session).getSubmission(3));
		assertNotNull(result);
		assertEquals(3, result.getId());
		assertEquals(AnswerClassResultStatus.MATCHED, result.getStatus());
		assertEquals(AnswerClassResultSource.MANUAL, result.getSource());
		assertEquals(3, result.getEvaluatedBy().getUid());
	}

	@Test
	void testStoreResultAndOverwrite() {
		Transaction tx = session.beginTransaction();
		AnswerClass answerClass = DAOFactory.AnswerClassDAOIf(session).getAnswerClass(2);
		Submission submission = DAOFactory.SubmissionDAOIf(session).getSubmission(7);
		assertNull(DAOFactory.AnswerClassResultDAOIf(session).getResult(answerClass, submission));

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
