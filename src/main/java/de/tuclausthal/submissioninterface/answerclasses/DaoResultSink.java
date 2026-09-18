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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.Session;

import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;

/**
 * Persists pipeline results through AnswerClassResultDAO.
 * The caller owns the Hibernate transaction. evaluatedBy stays empty here.
 */
public class DaoResultSink implements ResultSink {
	private final Session session;
	private final Submission submission;
	private final Map<Integer, AnswerClass> answerClassesById;

	/**
	 * @param session Hibernate session with an active transaction
	 * @param submission the submission the pipeline run evaluates
	 * @param answerClasses all answer classes that may appear in a {@link PipelineResult#getAnswerClassId()}
	 *                      (i.e. the same list that was passed to {@link AnswerClassMapper#map(List)})
	 */
	public DaoResultSink(Session session, Submission submission, List<AnswerClass> answerClasses) {
		this.session = Objects.requireNonNull(session, "session");
		this.submission = Objects.requireNonNull(submission, "submission");
		Objects.requireNonNull(answerClasses, "answerClasses");
		this.answerClassesById = answerClasses.stream().collect(Collectors.toMap(AnswerClass::getId, Function.identity()));
	}

	@Override
	public void store(PipelineResult result) {
		Objects.requireNonNull(result, "result");
		AnswerClass answerClass = answerClassesById.get(result.getAnswerClassId());
		if (answerClass == null) {
			throw new IllegalStateException("Unknown answer class id in pipeline result: " + result.getAnswerClassId());
		}
		DAOFactory.AnswerClassResultDAOIf(session).storeResult(answerClass, submission, result.getStatus(), result.getDetails(), result.getSource(), null);
	}
}
