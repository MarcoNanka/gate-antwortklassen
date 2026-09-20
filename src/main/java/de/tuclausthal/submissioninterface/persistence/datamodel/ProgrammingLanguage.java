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

package de.tuclausthal.submissioninterface.persistence.datamodel;

/**
 * Programming language for answer-class evaluation (selects the {@code LanguageProvider}).
 *
 * <p>Stored on every {@link AnswerClass} of a task and kept in sync by the advisor form —
 * same pattern as the shared {@code timesRunnableByStudents} budget. Only {@link #JAVA} is
 * offered in the UI until further providers exist.</p>
 */
public enum ProgrammingLanguage {
	JAVA("Java");

	private final String displayName;

	ProgrammingLanguage(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Parses a form/DB value; unknown or blank values fall back to {@link #JAVA}.
	 */
	public static ProgrammingLanguage fromParam(String value) {
		if (value == null || value.isBlank()) {
			return JAVA;
		}
		try {
			return ProgrammingLanguage.valueOf(value.trim());
		} catch (IllegalArgumentException e) {
			return JAVA;
		}
	}
}
