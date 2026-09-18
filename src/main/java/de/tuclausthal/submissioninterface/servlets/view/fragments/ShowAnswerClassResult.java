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

package de.tuclausthal.submissioninterface.servlets.view.fragments;

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;

/**
 * Shared rendering helper for answer-class results, used by
 * {@code ShowTaskStudentView}/{@code ShowAnswerClassResultsView} (student) and
 * {@code ShowSubmissionView} (tutor) — similar in spirit to {@code ShowDockerTestResult}/
 * {@code ShowJavaAdvancedIOTestResult}, just for the much simpler answer-class result shape.
 */
public final class ShowAnswerClassResult {
	private ShowAnswerClassResult() {}

	/**
	 * @return a short German label for a persisted {@code AnswerClassResultStatus}
	 */
	public static String statusLabel(AnswerClassResultStatus status) {
		if (status == null) {
			return "noch nicht geprüft";
		}
		return switch (status) {
			case MATCHED -> "erfüllt";
			case NOT_MATCHED -> "nicht erfüllt";
			case SKIPPED -> "nicht geprüft (Voraussetzungen nicht erfüllt)";
			case PENDING -> "wird noch manuell bewertet";
		};
	}
}
