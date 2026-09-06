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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.Session;

import de.tuclausthal.submissioninterface.persistence.dao.AnswerClassDAOIf;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass_;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;

/**
 * Data Access Object implementation for the AnswerClassDAOIf
 */
public class AnswerClassDAO extends AbstractDAO implements AnswerClassDAOIf {
	public AnswerClassDAO(Session session) {
		super(session);
	}

	@Override
	public AnswerClass createAnswerClass(Task task, String title, String description, String detectionType, int timesRunnableByStudents, boolean giveDetailsToStudents) {
		Session session = getSession();
		AnswerClass answerClass = new AnswerClass(task, title, description, detectionType, timesRunnableByStudents, giveDetailsToStudents);
		session.persist(answerClass);
		return answerClass;
	}

	@Override
	public AnswerClass getAnswerClass(int id) {
		return getSession().get(AnswerClass.class, id);
	}

	@Override
	public List<AnswerClass> getAnswerClassesForTask(Task task) {
		Session session = getSession();
		CriteriaBuilder builder = session.getCriteriaBuilder();
		CriteriaQuery<AnswerClass> criteria = builder.createQuery(AnswerClass.class);
		Root<AnswerClass> root = criteria.from(AnswerClass.class);
		criteria.select(root);
		criteria.where(builder.equal(root.get(AnswerClass_.task), task));
		criteria.orderBy(builder.asc(root.get(AnswerClass_.id)));
		return session.createQuery(criteria).list();
	}

	@Override
	public void deleteAnswerClass(AnswerClass answerClass) {
		getSession().remove(answerClass);
	}
}
