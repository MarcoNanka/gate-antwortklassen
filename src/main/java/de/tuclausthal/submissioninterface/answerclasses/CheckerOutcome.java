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

import java.util.Objects;

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;

/**
 * Outcome of an automatic checker (only MATCHED or NOT_MATCHED).
 */
public final class CheckerOutcome {
	private final AnswerClassResultStatus status;
	private final String details;

	public CheckerOutcome(AnswerClassResultStatus status, String details) {
		if (status != AnswerClassResultStatus.MATCHED && status != AnswerClassResultStatus.NOT_MATCHED) {
			throw new IllegalArgumentException("CheckerOutcome status must be MATCHED or NOT_MATCHED, got: " + status);
		}
		this.status = status;
		this.details = details;
	}

	public static CheckerOutcome matched(String details) {
		return new CheckerOutcome(AnswerClassResultStatus.MATCHED, details);
	}

	public static CheckerOutcome notMatched(String details) {
		return new CheckerOutcome(AnswerClassResultStatus.NOT_MATCHED, details);
	}

	public AnswerClassResultStatus getStatus() {
		return status;
	}

	public String getDetails() {
		return details;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CheckerOutcome other)) {
			return false;
		}
		return status == other.status && Objects.equals(details, other.details);
	}

	@Override
	public int hashCode() {
		return Objects.hash(status, details);
	}
}
