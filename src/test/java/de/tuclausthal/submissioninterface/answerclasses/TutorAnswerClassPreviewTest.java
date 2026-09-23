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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;

class TutorAnswerClassPreviewTest {
	@Test
	void availableWhileAnAutomaticAnswerClassHasNoBatchResult() {
		AnswerClass compileOk = answerClass(1, "COMPILE_OK");
		AnswerClass whileLoop = answerClass(2, "WHILE_LOOP");
		AnswerClassResult studentPreview = result(compileOk, AnswerClassResultSource.STUDENT_PREVIEW);

		assertTrue(TutorAnswerClassPreview.isAvailable(List.of(compileOk, whileLoop), Map.of(1, studentPreview)));
	}

	@Test
	void hiddenOnceEveryAutomaticAnswerClassHasABatchResult() {
		AnswerClass compileOk = answerClass(1, "COMPILE_OK");
		AnswerClass manual = answerClass(2, AnswerClassToEvaluate.MANUAL_DETECTION_TYPE);
		AnswerClassResult batch = result(compileOk, AnswerClassResultSource.AUTOMATIC);

		assertFalse(TutorAnswerClassPreview.isAvailable(List.of(compileOk, manual), Map.of(1, batch)));
	}

	@Test
	void snapshotMatchesOnlyTheSubmissionVersionItWasComputedFor() {
		ZonedDateTime evaluatedAt = ZonedDateTime.parse("2026-09-23T10:26:22+02:00");
		TutorAnswerClassPreview.Snapshot snapshot = new TutorAnswerClassPreview.Snapshot(7, evaluatedAt, Map.of(1, new PipelineResult(1, AnswerClassResultStatus.MATCHED, "ok", AnswerClassResultSource.MANUAL)));

		assertTrue(snapshot.matches(7, evaluatedAt));
		assertFalse(snapshot.matches(7, evaluatedAt.plusMinutes(5)));
		assertFalse(snapshot.matches(8, evaluatedAt));
	}

	@Test
	void hiddenWhenThereIsNoAutomaticAnswerClass() {
		AnswerClass manual = answerClass(1, AnswerClassToEvaluate.MANUAL_DETECTION_TYPE);

		assertFalse(TutorAnswerClassPreview.isAvailable(List.of(manual), Map.of()));
	}

	private static AnswerClass answerClass(int id, String detectionType) {
		AnswerClass answerClass = new AnswerClass(null, "title", "", detectionType, 0, false);
		answerClass.setId(id);
		return answerClass;
	}

	private static AnswerClassResult result(AnswerClass answerClass, AnswerClassResultSource source) {
		AnswerClassResult result = new AnswerClassResult();
		result.setAnswerClass(answerClass);
		result.setSource(source);
		return result;
	}
}
