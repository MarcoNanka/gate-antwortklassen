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
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import de.tuclausthal.submissioninterface.testframework.executor.impl.LocalExecutor;

/**
 * Runs one answer-class check in the background, on the same thread pool as the normal tests.
 */
public final class AnswerClassExecutor {
	private AnswerClassExecutor() {}

	/**
	 * @param task performs one {@code AnswerClassRunner} preview run and returns its results;
	 * must open/close its own Hibernate {@code Session} since it runs on a different thread than
	 * the caller (see {@code TestTask#performTask} for the same requirement on the classic side)
	 * @return a {@link Future} that completes once the run is done (or failed)
	 */
	public static Future<List<PipelineResult>> execute(Callable<List<PipelineResult>> task) {
		return LocalExecutor.getInstance().executeGenericTask(task);
	}
}
