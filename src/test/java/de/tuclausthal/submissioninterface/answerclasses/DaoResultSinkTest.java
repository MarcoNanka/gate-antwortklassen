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

package de.tuclausthal.submissioninterface.answerclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.hibernate.Transaction;
import org.junit.jupiter.api.Test;

import de.tuclausthal.submissioninterface.BasicTest;
import de.tuclausthal.submissioninterface.GATEDBTest;
import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;
import de.tuclausthal.submissioninterface.persistence.datamodel.User;

/**
 * Integration test for {@link DaoResultSink}, against the fixtures from {@code test-data.sql}
 * (task 3, submission 7). Per the fixture, answer class 1 (COMPILE_OK) already has a stored
 * result for submission 7 ("compilation failed" / STUDENT_PREVIEW), while answer class 2
 * (WHILE_LOOP) does not have one yet — which lets us cover both the "create" and the
 * "overwrite" path of {@code AnswerClassResultDAO#storeResult}.
 */
@GATEDBTest
class DaoResultSinkTest extends BasicTest {
	@Test
	void storesNewResultForAnswerClassWithoutExistingResult() {
		Transaction tx = session.beginTransaction();
		try {
			AnswerClass whileLoop = DAOFactory.AnswerClassDAOIf(session).getAnswerClass(2);
			Submission submission = DAOFactory.SubmissionDAOIf(session).getSubmission(7);
			assertNull(DAOFactory.AnswerClassResultDAOIf(session).getResult(whileLoop, submission));

			DaoResultSink sink = new DaoResultSink(session, submission, List.of(whileLoop));
			sink.store(new PipelineResult(whileLoop.getId(), AnswerClassResultStatus.MATCHED, "while loop present", AnswerClassResultSource.AUTOMATIC));

			AnswerClassResult stored = DAOFactory.AnswerClassResultDAOIf(session).getResult(whileLoop, submission);
			assertNotNull(stored);
			assertEquals(AnswerClassResultStatus.MATCHED, stored.getStatus());
			assertEquals("while loop present", stored.getDetails());
			assertEquals(AnswerClassResultSource.AUTOMATIC, stored.getSource());
			assertNull(stored.getEvaluatedBy());
		} finally {
			tx.rollback();
		}
	}

	@Test
	void overwritesExistingResultForSameAnswerClass() {
		Transaction tx = session.beginTransaction();
		try {
			AnswerClass compileOk = DAOFactory.AnswerClassDAOIf(session).getAnswerClass(1);
			Submission submission = DAOFactory.SubmissionDAOIf(session).getSubmission(7);
			AnswerClassResult before = DAOFactory.AnswerClassResultDAOIf(session).getResult(compileOk, submission);
			assertNotNull(before); // fixture: NOT_MATCHED / "compilation failed" / STUDENT_PREVIEW
			assertEquals(AnswerClassResultStatus.NOT_MATCHED, before.getStatus());
			int resultId = before.getId();

			DaoResultSink sink = new DaoResultSink(session, submission, List.of(compileOk));
			sink.store(new PipelineResult(compileOk.getId(), AnswerClassResultStatus.MATCHED, "compiled successfully", AnswerClassResultSource.AUTOMATIC));

			AnswerClassResult after = DAOFactory.AnswerClassResultDAOIf(session).getResult(compileOk, submission);
			assertNotNull(after);
			assertEquals(resultId, after.getId()); // same row, overwritten - not a duplicate
			assertEquals(AnswerClassResultStatus.MATCHED, after.getStatus());
			assertEquals("compiled successfully", after.getDetails());
			assertEquals(AnswerClassResultSource.AUTOMATIC, after.getSource());
		} finally {
			tx.rollback();
		}
	}

	@Test
	void leavesTutorGradeInPlace() {
		Transaction tx = session.beginTransaction();
		try {
			AnswerClass whileLoop = DAOFactory.AnswerClassDAOIf(session).getAnswerClass(2);
			Submission submission = DAOFactory.SubmissionDAOIf(session).getSubmission(7);
			User tutor = DAOFactory.UserDAOIf(session).getUser(3);
			DAOFactory.AnswerClassResultDAOIf(session).storeResult(whileLoop, submission, AnswerClassResultStatus.NOT_MATCHED, "nicht in O(n)", AnswerClassResultSource.MANUAL, tutor);

			DaoResultSink sink = new DaoResultSink(session, submission, List.of(whileLoop));
			sink.store(new PipelineResult(whileLoop.getId(), AnswerClassResultStatus.PENDING, null, AnswerClassResultSource.MANUAL));
			sink.store(new PipelineResult(whileLoop.getId(), AnswerClassResultStatus.SKIPPED, "prerequisites not satisfied", AnswerClassResultSource.AUTOMATIC));

			AnswerClassResult stored = DAOFactory.AnswerClassResultDAOIf(session).getResult(whileLoop, submission);
			assertEquals(AnswerClassResultStatus.NOT_MATCHED, stored.getStatus());
			assertEquals("nicht in O(n)", stored.getDetails());
			assertEquals(AnswerClassResultSource.MANUAL, stored.getSource());
			assertEquals(tutor.getUid(), stored.getEvaluatedBy().getUid());
		} finally {
			tx.rollback();
		}
	}

	@Test
	void throwsForAnswerClassIdNotInTheGivenSet() {
		Transaction tx = session.beginTransaction();
		try {
			AnswerClass compileOk = DAOFactory.AnswerClassDAOIf(session).getAnswerClass(1);
			Submission submission = DAOFactory.SubmissionDAOIf(session).getSubmission(7);
			DaoResultSink sink = new DaoResultSink(session, submission, List.of(compileOk));

			assertThrows(IllegalStateException.class, () -> sink.store(new PipelineResult(9999, AnswerClassResultStatus.SKIPPED, "n/a", AnswerClassResultSource.AUTOMATIC)));
		} finally {
			tx.rollback();
		}
	}
}
