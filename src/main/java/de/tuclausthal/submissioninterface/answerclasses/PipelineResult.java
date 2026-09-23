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

import java.io.Serializable;
import java.util.Objects;

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;

/**
 * One stored/emitted result from a pipeline run (DAO wiring comes later).
 */
public final class PipelineResult implements Serializable {
	private static final long serialVersionUID = 1L;

	private final int answerClassId;
	private final AnswerClassResultStatus status;
	private final String details;
	private final AnswerClassResultSource source;

	public PipelineResult(int answerClassId, AnswerClassResultStatus status, String details, AnswerClassResultSource source) {
		this.answerClassId = answerClassId;
		this.status = Objects.requireNonNull(status, "status");
		this.details = details;
		this.source = Objects.requireNonNull(source, "source");
	}

	public int getAnswerClassId() {
		return answerClassId;
	}

	public AnswerClassResultStatus getStatus() {
		return status;
	}

	public String getDetails() {
		return details;
	}

	public AnswerClassResultSource getSource() {
		return source;
	}
}
