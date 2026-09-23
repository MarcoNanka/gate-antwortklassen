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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;

/**
 * Maps an AnswerClass entity to AnswerClassToEvaluate.
 * Keeps the input order. Prerequisites come from the entity.
 */
public final class AnswerClassMapper {
	private AnswerClassMapper() {}

	/**
	 * @param answerClasses answer classes for one task, in the order they should be evaluated
	 * @return pipeline DTOs in the same order
	 */
	public static List<AnswerClassToEvaluate> map(List<AnswerClass> answerClasses) {
		Objects.requireNonNull(answerClasses, "answerClasses");
		List<AnswerClassToEvaluate> result = new ArrayList<>(answerClasses.size());
		for (AnswerClass answerClass : answerClasses) {
			result.add(map(answerClass));
		}
		return result;
	}

	public static AnswerClassToEvaluate map(AnswerClass answerClass) {
		Objects.requireNonNull(answerClass, "answerClass");
		List<Integer> prerequisiteIds = answerClass.getPrerequisites().stream().map(AnswerClass::getId).collect(Collectors.toList());
		return AnswerClassToEvaluate.builder(answerClass.getId(), answerClass.getDetectionType(), answerClass.getTimesRunnableByStudents())
				.prerequisiteIds(prerequisiteIds)
				.functionTestCode(answerClass.getFunctionTestCode())
				.functionTestExpectedOutput(answerClass.getFunctionTestExpectedOutput())
				.whileLoopRange(answerClass.getWhileLoopMin(), answerClass.getWhileLoopMax())
				.forLoopRange(answerClass.getForLoopMin(), answerClass.getForLoopMax())
				.build();
	}
}
