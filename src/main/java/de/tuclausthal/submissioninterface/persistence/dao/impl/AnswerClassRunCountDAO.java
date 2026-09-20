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

import java.util.List;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.Session;

import de.tuclausthal.submissioninterface.persistence.dao.AnswerClassDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.AnswerClassRunCountDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassRunCount;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassRunCount_;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.persistence.datamodel.User;

/**
 * Data Access Object implementation for the AnswerClassRunCountDAOIf
 */
public class AnswerClassRunCountDAO extends AbstractDAO implements AnswerClassRunCountDAOIf {
	public AnswerClassRunCountDAO(Session session) {
		super(session);
	}

	@Override
	public boolean canSeeResultAndIncrementCounterTransaction(Task task, User user) {
		Session session = getSession();
		int limit = getTimesRunnableByStudents(task);
		AnswerClassRunCount runCount = getRunCountLocked(task, user);
		if (runCount == null) {
			runCount = new AnswerClassRunCount();
			runCount.setTask(task);
			runCount.setUser(user);
		}
		if (runCount.getTimesExecuted() >= limit) {
			return false;
		}
		runCount.setTimesExecuted(runCount.getTimesExecuted() + 1);
		session.persist(runCount);
		return true;
	}

	@Override
	public int canStillRunXTimes(Task task, User user) {
		int limit = getTimesRunnableByStudents(task);
		AnswerClassRunCount runCount = getRunCount(task, user, false);
		if (runCount == null) {
			return limit;
		}
		return Math.max(0, limit - runCount.getTimesExecuted());
	}

	private int getTimesRunnableByStudents(Task task) {
		AnswerClassDAOIf answerClassDAO = DAOFactory.AnswerClassDAOIf(getSession());
		List<AnswerClass> answerClasses = answerClassDAO.getAnswerClassesForTask(task);
		if (answerClasses.isEmpty()) {
			return 0;
		}
		// Shared budget for the task: after AnswerClassManager saves, all ACs carry the same
		// value. Use max() so briefly diverging legacy rows still pick a defined limit.
		return answerClasses.stream().mapToInt(AnswerClass::getTimesRunnableByStudents).max().orElse(0);
	}

	private AnswerClassRunCount getRunCountLocked(Task task, User user) {
		return getRunCount(task, user, true);
	}

	private AnswerClassRunCount getRunCount(Task task, User user, boolean locked) {
		Session session = getSession();
		CriteriaBuilder builder = session.getCriteriaBuilder();
		CriteriaQuery<AnswerClassRunCount> criteria = builder.createQuery(AnswerClassRunCount.class);
		Root<AnswerClassRunCount> root = criteria.from(AnswerClassRunCount.class);
		criteria.select(root);
		criteria.where(builder.and(builder.equal(root.get(AnswerClassRunCount_.task), task), builder.equal(root.get(AnswerClassRunCount_.user), user)));
		var query = session.createQuery(criteria);
		if (locked) {
			query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
		}
		return query.uniqueResult();
	}
}
