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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.hibernate.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.tuclausthal.submissioninterface.BasicTest;
import de.tuclausthal.submissioninterface.GATEDBTest;
import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;
import de.tuclausthal.submissioninterface.persistence.datamodel.User;
import de.tuclausthal.submissioninterface.util.Util;

/**
 * End-to-end test for {@link AnswerClassRunner}: real DB fixtures (task 3 / submission 7 /
 * answer classes 1+2, see {@code test-data.sql}) plus a real (temporary) Java submission on
 * disk, run through the actual {@code JavaLanguageProvider}/checkers.
 */
@GATEDBTest
class AnswerClassRunnerTest extends BasicTest {
	private static final String HELLO_WORLD_NO_WHILE = "public class HelloWorld {\n" + "\tpublic static void main(String[] args) {\n" + "\t\tSystem.out.println(\"Hello World!\");\n" + "\t}\n" + "}\n";

	@Test
	void runsPipelineAndPersistsResults(@TempDir Path dataPath) throws Exception {
		Submission submission = DAOFactory.SubmissionDAOIf(session).getSubmission(7);
		writeSubmissionFile(dataPath, submission, "HelloWorld.java", HELLO_WORLD_NO_WHILE);

		Transaction tx = session.beginTransaction();
		try {
			List<PipelineResult> results = new AnswerClassRunner(session).run(PipelineTrigger.BATCH, submission, dataPath);

			assertEquals(2, results.size());
			assertEquals(1, results.get(0).getAnswerClassId());
			assertEquals(AnswerClassResultStatus.MATCHED, results.get(0).getStatus());
			assertEquals(AnswerClassResultSource.AUTOMATIC, results.get(0).getSource());

			assertEquals(2, results.get(1).getAnswerClassId());
			assertEquals(AnswerClassResultStatus.NOT_MATCHED, results.get(1).getStatus());
			assertEquals(AnswerClassResultSource.AUTOMATIC, results.get(1).getSource());

			AnswerClassResult persisted = DAOFactory.AnswerClassResultDAOIf(session).getResult(DAOFactory.AnswerClassDAOIf(session).getAnswerClass(1), submission);
			assertNotNull(persisted);
			assertEquals(AnswerClassResultStatus.MATCHED, persisted.getStatus());
		} finally {
			tx.rollback();
		}
	}

	@Test
	void batchKeepsTutorGradeOnManualAnswerClass(@TempDir Path dataPath) throws Exception {
		Submission submission = DAOFactory.SubmissionDAOIf(session).getSubmission(7);
		writeSubmissionFile(dataPath, submission, "HelloWorld.java", HELLO_WORLD_NO_WHILE);

		Transaction tx = session.beginTransaction();
		try {
			AnswerClass manual = DAOFactory.AnswerClassDAOIf(session).getAnswerClass(2);
			manual.setDetectionType(AnswerClassToEvaluate.MANUAL_DETECTION_TYPE);
			User tutor = DAOFactory.UserDAOIf(session).getUser(3);
			DAOFactory.AnswerClassResultDAOIf(session).storeResult(manual, submission, AnswerClassResultStatus.NOT_MATCHED, "nicht in O(n)", AnswerClassResultSource.MANUAL, tutor);

			List<PipelineResult> results = new AnswerClassRunner(session).run(PipelineTrigger.BATCH, submission, dataPath);

			assertEquals(1, results.size());
			assertEquals(1, results.get(0).getAnswerClassId());
			assertEquals(AnswerClassResultSource.AUTOMATIC, results.get(0).getSource());

			AnswerClassResult kept = DAOFactory.AnswerClassResultDAOIf(session).getResult(manual, submission);
			assertEquals(AnswerClassResultStatus.NOT_MATCHED, kept.getStatus());
			assertEquals("nicht in O(n)", kept.getDetails());
			assertEquals(AnswerClassResultSource.MANUAL, kept.getSource());
			assertEquals(tutor.getUid(), kept.getEvaluatedBy().getUid());
		} finally {
			tx.rollback();
		}
	}

	@Test
	void previewDoesNotOverwriteStoredResults(@TempDir Path dataPath) throws Exception {
		Submission submission = DAOFactory.SubmissionDAOIf(session).getSubmission(7);
		writeSubmissionFile(dataPath, submission, "HelloWorld.java", HELLO_WORLD_NO_WHILE);

		Transaction tx = session.beginTransaction();
		try {
			List<PipelineResult> results = new AnswerClassRunner(session).preview(submission, dataPath);

			assertEquals(2, results.size());
			assertEquals(AnswerClassResultStatus.MATCHED, results.get(0).getStatus());

			AnswerClassResult stored = DAOFactory.AnswerClassResultDAOIf(session).getResult(DAOFactory.AnswerClassDAOIf(session).getAnswerClass(1), submission);
			assertNotNull(stored);
			assertEquals(AnswerClassResultStatus.NOT_MATCHED, stored.getStatus());
			assertEquals(AnswerClassResultSource.STUDENT_PREVIEW, stored.getSource());
			assertNull(DAOFactory.AnswerClassResultDAOIf(session).getResult(DAOFactory.AnswerClassDAOIf(session).getAnswerClass(2), submission));
		} finally {
			tx.rollback();
		}
	}

	@Test
	void studentPreviewDoesNotOverwriteStoredResults(@TempDir Path dataPath) throws Exception {
		Submission submission = DAOFactory.SubmissionDAOIf(session).getSubmission(7);
		writeSubmissionFile(dataPath, submission, "HelloWorld.java", HELLO_WORLD_NO_WHILE);

		Transaction tx = session.beginTransaction();
		try {
			List<PipelineResult> results = new AnswerClassRunner(session).preview(PipelineTrigger.STUDENT, submission, dataPath);

			assertEquals(2, results.size());
			assertEquals(AnswerClassResultSource.STUDENT_PREVIEW, results.get(0).getSource());

			AnswerClassResult stored = DAOFactory.AnswerClassResultDAOIf(session).getResult(DAOFactory.AnswerClassDAOIf(session).getAnswerClass(1), submission);
			assertNotNull(stored);
			assertEquals(AnswerClassResultStatus.NOT_MATCHED, stored.getStatus());
			assertEquals(AnswerClassResultSource.STUDENT_PREVIEW, stored.getSource());
		} finally {
			tx.rollback();
		}
	}

	@Test
	void returnsNoResultsWhenSubmissionHasNoFiles(@TempDir Path dataPath) throws Exception {
		Submission submission = DAOFactory.SubmissionDAOIf(session).getSubmission(7);
		// dataPath is empty - no submission files were ever written for it

		Transaction tx = session.beginTransaction();
		try {
			List<PipelineResult> results = new AnswerClassRunner(session).run(PipelineTrigger.BATCH, submission, dataPath);
			assertTrue(results.isEmpty());
		} finally {
			tx.rollback();
		}
	}

	private static void writeSubmissionFile(Path dataPath, Submission submission, String fileName, String content) throws Exception {
		Path submissionPath = Util.constructPath(dataPath, submission);
		Files.createDirectories(submissionPath);
		Files.writeString(submissionPath.resolve(fileName), content);
	}
}
