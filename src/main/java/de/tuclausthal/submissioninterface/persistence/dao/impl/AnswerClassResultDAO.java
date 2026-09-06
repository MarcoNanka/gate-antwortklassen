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

package de.tuclausthal.submissioninterface.persistence.dao.impl;

import java.time.ZonedDateTime;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.query.Query;

import de.tuclausthal.submissioninterface.persistence.dao.AnswerClassResultDAOIf;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult_;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;
import de.tuclausthal.submissioninterface.persistence.datamodel.User;

/**
 * Data Access Object implementation for the AnswerClassResultDAOIf
 */
public class AnswerClassResultDAO extends AbstractDAO implements AnswerClassResultDAOIf {
	public AnswerClassResultDAO(Session session) {
		super(session);
	}

	@Override
	public void storeResult(AnswerClass answerClass, Submission submission, AnswerClassResultStatus status, String details, AnswerClassResultSource source, User evaluatedBy) {
		Session session = getSession();
		session.lock(answerClass, LockMode.PESSIMISTIC_WRITE);
		AnswerClassResult result = getResult(answerClass, submission, true);
		if (result == null) {
			result = new AnswerClassResult();
			result.setAnswerClass(answerClass);
			result.setSubmission(submission);
		}
		result.setStatus(status);
		result.setDetails(details == null ? "" : details);
		result.setSource(source);
		result.setEvaluatedBy(evaluatedBy);
		result.setCheckedAt(ZonedDateTime.now());
		session.persist(result);
	}

	private AnswerClassResult getResult(AnswerClass answerClass, Submission submission, boolean locked) {
		Session session = getSession();
		CriteriaBuilder builder = session.getCriteriaBuilder();
		CriteriaQuery<AnswerClassResult> criteria = builder.createQuery(AnswerClassResult.class);
		Root<AnswerClassResult> root = criteria.from(AnswerClassResult.class);
		criteria.select(root);
		criteria.where(builder.and(builder.equal(root.get(AnswerClassResult_.answerClass), answerClass), builder.equal(root.get(AnswerClassResult_.submission), submission)));
		Query<AnswerClassResult> query = session.createQuery(criteria);
		if (locked) {
			query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
		}
		return query.uniqueResult();
	}

	@Override
	public AnswerClassResult getResult(AnswerClass answerClass, Submission submission) {
		return getResult(answerClass, submission, false);
	}
}
