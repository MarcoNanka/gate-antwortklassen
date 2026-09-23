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

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;

/**
 * Which stored answer-class results a student may see after the deadline.
 * A student's own run is not stored and is never shown here. Batch results and a tutor's
 * grade on a manual answer class are.
 */
public final class StudentAnswerClassVisibility {
	private StudentAnswerClassVisibility() {}

	/**
	 * @return the status to show, or {@code null} when this row is not an official student-visible result
	 */
	public static AnswerClassResultStatus officialStatus(AnswerClassResult result) {
		if (result == null) {
			return null;
		}
		if (result.getSource() == AnswerClassResultSource.AUTOMATIC) {
			return result.getStatus();
		}
		if (result.getSource() == AnswerClassResultSource.MANUAL && result.getEvaluatedBy() != null) {
			return result.getStatus();
		}
		return null;
	}
}
