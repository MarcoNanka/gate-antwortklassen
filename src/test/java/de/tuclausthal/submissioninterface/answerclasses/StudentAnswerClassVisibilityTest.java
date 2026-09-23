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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;
import de.tuclausthal.submissioninterface.persistence.datamodel.User;

class StudentAnswerClassVisibilityTest {
	@Test
	void showsBatchResultsAndTutorGradesOnly() {
		assertNull(StudentAnswerClassVisibility.officialStatus(null));
		assertNull(StudentAnswerClassVisibility.officialStatus(result(AnswerClassResultSource.STUDENT_PREVIEW, false)));
		assertNull(StudentAnswerClassVisibility.officialStatus(result(AnswerClassResultSource.MANUAL, false)));
		assertEquals(AnswerClassResultStatus.MATCHED, StudentAnswerClassVisibility.officialStatus(result(AnswerClassResultSource.AUTOMATIC, false)));
		assertEquals(AnswerClassResultStatus.NOT_MATCHED, StudentAnswerClassVisibility.officialStatus(result(AnswerClassResultSource.MANUAL, true)));
	}

	private static AnswerClassResult result(AnswerClassResultSource source, boolean gradedByTutor) {
		AnswerClassResult result = new AnswerClassResult();
		result.setSource(source);
		result.setStatus(source == AnswerClassResultSource.MANUAL ? AnswerClassResultStatus.NOT_MATCHED : AnswerClassResultStatus.MATCHED);
		if (gradedByTutor) {
			result.setEvaluatedBy(new User());
		}
		return result;
	}
}
