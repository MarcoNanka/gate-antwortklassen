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
 * Pipeline view of one answer class, without the JPA entity.
 * Prerequisites are answer-class ids that must be MATCHED in this run.
 * Extra settings (function test, loop bounds) go through the builder.
 */
public final class AnswerClassToEvaluate {
	public static final String MANUAL_DETECTION_TYPE = "MANUAL";
	/** Default lower bound for WHILE_LOOP when nothing else was configured: "at least one". */
	public static final int DEFAULT_WHILE_LOOP_MIN = 1;
	/** Default lower bound for FOR_LOOP when nothing else was configured: "at least one". */
	public static final int DEFAULT_FOR_LOOP_MIN = 1;

	private final int id;
	private final String detectionType;
	private final int timesRunnableByStudents;
	private final List<Integer> prerequisiteIds;
	private final String functionTestCode;
	private final String functionTestExpectedOutput;
	private final int whileLoopMin;
	private final Integer whileLoopMax;
	private final int forLoopMin;
	private final Integer forLoopMax;

	private AnswerClassToEvaluate(Builder builder) {
		this.id = builder.id;
		this.detectionType = Objects.requireNonNull(builder.detectionType, "detectionType");
		this.timesRunnableByStudents = Math.max(0, builder.timesRunnableByStudents);
		this.prerequisiteIds = builder.prerequisiteIds == null ? List.of() : List.copyOf(builder.prerequisiteIds);
		this.functionTestCode = builder.functionTestCode;
		this.functionTestExpectedOutput = builder.functionTestExpectedOutput;
		this.whileLoopMin = builder.whileLoopMin;
		this.whileLoopMax = builder.whileLoopMax;
		this.forLoopMin = builder.forLoopMin;
		this.forLoopMax = builder.forLoopMax;
	}

	public AnswerClassToEvaluate(int id, String detectionType, int timesRunnableByStudents, List<Integer> prerequisiteIds) {
		this(builder(id, detectionType, timesRunnableByStudents).prerequisiteIds(prerequisiteIds));
	}

	public AnswerClassToEvaluate(int id, String detectionType, int timesRunnableByStudents) {
		this(id, detectionType, timesRunnableByStudents, Collections.emptyList());
	}

	public static Builder builder(int id, String detectionType, int timesRunnableByStudents) {
		return new Builder(id, detectionType, timesRunnableByStudents);
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

	/**
	 * Advisor-configured Java statements for {@code FUNCTION_TEST} answer classes; {@code null}
	 * for every other detectionType.
	 */
	public String getFunctionTestCode() {
		return functionTestCode;
	}

	/**
	 * Expected stdout (trimmed comparison) for {@code FUNCTION_TEST} answer classes; {@code null}
	 * for every other detectionType.
	 */
	public String getFunctionTestExpectedOutput() {
		return functionTestExpectedOutput;
	}

	/**
	 * Minimum number of {@code while} statements required for {@code WHILE_LOOP} answer classes.
	 * Defaults to {@value #DEFAULT_WHILE_LOOP_MIN} (i.e. the old "contains at least one"
	 * behavior) when not explicitly configured via {@link Builder#whileLoopRange}.
	 */
	public int getWhileLoopMin() {
		return whileLoopMin;
	}

	/**
	 * Maximum number of {@code while} statements allowed for {@code WHILE_LOOP} answer classes;
	 * {@code null} means unbounded.
	 */
	public Integer getWhileLoopMax() {
		return whileLoopMax;
	}

	/**
	 * Minimum number of classic {@code for} statements required for {@code FOR_LOOP} answer
	 * classes. Defaults to {@value #DEFAULT_FOR_LOOP_MIN} when not explicitly configured via
	 * {@link Builder#forLoopRange}.
	 */
	public int getForLoopMin() {
		return forLoopMin;
	}

	/**
	 * Maximum number of classic {@code for} statements allowed for {@code FOR_LOOP} answer
	 * classes; {@code null} means unbounded.
	 */
	public Integer getForLoopMax() {
		return forLoopMax;
	}

	public boolean isManual() {
		return MANUAL_DETECTION_TYPE.equals(detectionType);
	}

	/**
	 * Builder for answer classes whose checker needs detectionType-specific configuration.
	 */
	public static final class Builder {
		private final int id;
		private final String detectionType;
		private final int timesRunnableByStudents;
		private List<Integer> prerequisiteIds = List.of();
		private String functionTestCode;
		private String functionTestExpectedOutput;
		private int whileLoopMin = DEFAULT_WHILE_LOOP_MIN;
		private Integer whileLoopMax;
		private int forLoopMin = DEFAULT_FOR_LOOP_MIN;
		private Integer forLoopMax;

		private Builder(int id, String detectionType, int timesRunnableByStudents) {
			this.id = id;
			this.detectionType = Objects.requireNonNull(detectionType, "detectionType");
			this.timesRunnableByStudents = timesRunnableByStudents;
		}

		public Builder prerequisiteIds(List<Integer> prerequisiteIds) {
			this.prerequisiteIds = prerequisiteIds == null ? List.of() : List.copyOf(prerequisiteIds);
			return this;
		}

		public Builder functionTestCode(String functionTestCode) {
			this.functionTestCode = functionTestCode;
			return this;
		}

		public Builder functionTestExpectedOutput(String functionTestExpectedOutput) {
			this.functionTestExpectedOutput = functionTestExpectedOutput;
			return this;
		}

		/**
		 * @param min lower bound (inclusive); values below 0 are clamped to 0
		 * @param max upper bound (inclusive), or {@code null} for unbounded
		 */
		public Builder whileLoopRange(int min, Integer max) {
			this.whileLoopMin = Math.max(0, min);
			this.whileLoopMax = max;
			return this;
		}

		/**
		 * @param min lower bound (inclusive); values below 0 are clamped to 0
		 * @param max upper bound (inclusive), or {@code null} for unbounded
		 */
		public Builder forLoopRange(int min, Integer max) {
			this.forLoopMin = Math.max(0, min);
			this.forLoopMax = max;
			return this;
		}

		public AnswerClassToEvaluate build() {
			return new AnswerClassToEvaluate(this);
		}
	}
}
