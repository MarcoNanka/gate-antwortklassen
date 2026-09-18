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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;

/**
 * Pure unit tests for {@link AnswerClassMapper} — no DB/session needed, entities are
 * constructed in memory.
 */
class AnswerClassMapperTest {
	@Test
	void mapsFieldsAndPreservesOrder() {
		Task task = new Task();
		AnswerClass first = new AnswerClass(task, "Code compiles", "desc", "COMPILE_OK", 2, true);
		first.setId(1);
		AnswerClass second = new AnswerClass(task, "While loop", "desc", "WHILE_LOOP", 0, false);
		second.setId(2);

		List<AnswerClassToEvaluate> mapped = AnswerClassMapper.map(List.of(first, second));

		assertEquals(2, mapped.size());
		assertEquals(1, mapped.get(0).getId());
		assertEquals("COMPILE_OK", mapped.get(0).getDetectionType());
		assertEquals(2, mapped.get(0).getTimesRunnableByStudents());
		assertEquals(List.of(), mapped.get(0).getPrerequisiteIds());

		// order of the input list is preserved (== id ascending, as returned by the DAO)
		assertEquals(2, mapped.get(1).getId());
		assertEquals("WHILE_LOOP", mapped.get(1).getDetectionType());
		assertEquals(0, mapped.get(1).getTimesRunnableByStudents());
	}

	@Test
	void mapEmptyListReturnsEmptyList() {
		assertTrue(AnswerClassMapper.map(List.of()).isEmpty());
	}

	@Test
	void singleMapMatchesManualDetectionType() {
		Task task = new Task();
		AnswerClass answerClass = new AnswerClass(task, "Manual review", "desc", AnswerClassToEvaluate.MANUAL_DETECTION_TYPE, 1, true);
		answerClass.setId(5);

		AnswerClassToEvaluate mapped = AnswerClassMapper.map(answerClass);
		assertEquals(5, mapped.getId());
		assertTrue(mapped.isManual());
	}

	@Test
	void mapNullListThrows() {
		assertThrows(NullPointerException.class, () -> AnswerClassMapper.map((List<AnswerClass>) null));
	}
}
