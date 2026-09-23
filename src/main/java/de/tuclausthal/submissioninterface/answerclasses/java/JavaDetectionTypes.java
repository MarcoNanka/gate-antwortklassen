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

package de.tuclausthal.submissioninterface.answerclasses.java;

/**
 * detectionType strings for the built-in Java checkers.
 */
public final class JavaDetectionTypes {
	public static final String COMPILE_OK = "COMPILE_OK";
	public static final String WHILE_LOOP = "WHILE_LOOP";
	/**
	 * Counts classic {@code for(...)} statements (not for-each); see {@code ForLoopChecker}.
	 * Sibling of {@link #WHILE_LOOP} - combining "FOR_LOOP max=0" with "WHILE_LOOP min=1" lets
	 * advisors detect that a repetition was solved with while instead of for.
	 */
	public static final String FOR_LOOP = "FOR_LOOP";
	/**
	 * Advisor-configured input/output check (one test case per answer class); see
	 * {@code FunctionTestChecker} / {@code RunArtifact}.
	 */
	public static final String FUNCTION_TEST = "FUNCTION_TEST";

	private JavaDetectionTypes() {}

	public static boolean isKnown(String detectionType) {
		return COMPILE_OK.equals(detectionType) || WHILE_LOOP.equals(detectionType) || FOR_LOOP.equals(detectionType) || FUNCTION_TEST.equals(detectionType);
	}

	/**
	 * Advisor-facing label; unknown values are shown as stored.
	 */
	public static String displayName(String detectionType) {
		if (COMPILE_OK.equals(detectionType)) {
			return "Code kompiliert (javac)";
		}
		if (WHILE_LOOP.equals(detectionType)) {
			return "enthält while-Schleife";
		}
		if (FOR_LOOP.equals(detectionType)) {
			return "enthält for-Schleife";
		}
		if (FUNCTION_TEST.equals(detectionType)) {
			return "Funktionstest (Ein-/Ausgabe)";
		}
		return detectionType == null ? "" : detectionType;
	}
}
