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

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;

/**
 * Maps the JPA {@link AnswerClass} entity onto the pipeline-internal, persistence-free
 * {@link AnswerClassToEvaluate} DTO (see {@code docs/antwortklassen-pipeline.puml}).
 *
 * <p>Order: the pipeline evaluates answer classes in list order, so this mapper preserves
 * exactly the order of the given input list. Callers are expected to pass the list as
 * returned by {@code AnswerClassDAOIf#getAnswerClassesForTask}, which is already sorted by
 * ascending id.</p>
 *
 * <p>Prerequisites: {@link AnswerClass} does not (yet) persist prerequisite ids, so every
 * mapped {@link AnswerClassToEvaluate} currently has an empty prerequisite list. Once
 * prerequisites are modeled in the DB (see the "Optional" step in the antwortklassen rule),
 * this is the only place that needs to change.</p>
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
		return new AnswerClassToEvaluate(answerClass.getId(), answerClass.getDetectionType(), answerClass.getTimesRunnableByStudents(), List.of());
	}
}
