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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Pipeline view of one answer class (decoupled from the JPA entity for unit tests).
 * Prerequisites are answer-class ids that must have status {@code MATCHED} in this run.
 */
public final class AnswerClassToEvaluate {
	public static final String MANUAL_DETECTION_TYPE = "MANUAL";

	private final int id;
	private final String detectionType;
	private final int timesRunnableByStudents;
	private final List<Integer> prerequisiteIds;

	public AnswerClassToEvaluate(int id, String detectionType, int timesRunnableByStudents, List<Integer> prerequisiteIds) {
		this.id = id;
		this.detectionType = Objects.requireNonNull(detectionType, "detectionType");
		this.timesRunnableByStudents = Math.max(0, timesRunnableByStudents);
		this.prerequisiteIds = prerequisiteIds == null ? List.of() : List.copyOf(prerequisiteIds);
	}

	public AnswerClassToEvaluate(int id, String detectionType, int timesRunnableByStudents) {
		this(id, detectionType, timesRunnableByStudents, Collections.emptyList());
	}

	public int getId() {
		return id;
	}

	public String getDetectionType() {
		return detectionType;
	}

	public int getTimesRunnableByStudents() {
		return timesRunnableByStudents;
	}

	public List<Integer> getPrerequisiteIds() {
		return prerequisiteIds;
	}

	public boolean isManual() {
		return MANUAL_DETECTION_TYPE.equals(detectionType);
	}
}
