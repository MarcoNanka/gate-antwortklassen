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

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;

/**
 * Runs answer classes for all submissions after the deadline.
 * One task at a time, each submission in its own transaction.
 */
public final class AnswerClassBatchRunner {
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private AnswerClassBatchRunner() {}

	/**
	 * Drains the answer-class batch queue: repeatedly claims one due task (see
	 * {@code AnswerClassDAOIf#takeTaskForAnswerClassBatchTransacted()}) and runs the pipeline
	 * with {@link PipelineTrigger#BATCH} for every one of its submissions, each in its own
	 * transaction so that one failing submission does not affect the others.
	 *
	 * @param session Hibernate session to use (no transaction may be active when this is called)
	 * @param dataPath the GATE data root (i.e. {@code Configuration.getInstance().getDataPath()})
	 */
	public static void runBatch(Session session, Path dataPath) {
		Task task;
		while ((task = DAOFactory.AnswerClassDAOIf(session).takeTaskForAnswerClassBatchTransacted()) != null) {
			runForTask(session, dataPath, task);
		}
	}

	/**
	 * Runs the pipeline with {@link PipelineTrigger#BATCH} for every submission of {@code task}.
	 * Each submission is its own transaction. Callers own claiming/{@code needsToRun}.
	 */
	public static void runForTask(Session session, Path dataPath, Task task) {
		List<Submission> submissions = new ArrayList<>(task.getSubmissions());
		for (Submission submission : submissions) {
			Transaction tx = session.beginTransaction();
			try {
				new AnswerClassRunner(session).run(PipelineTrigger.BATCH, submission, dataPath);
				tx.commit();
			} catch (Exception e) {
				tx.rollback();
				LOG.error("Failed to run answer class batch for submission {} (task {})", submission.getSubmissionid(), task.getTaskid(), e);
			}
		}
	}
}
