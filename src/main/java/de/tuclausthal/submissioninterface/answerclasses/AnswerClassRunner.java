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

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.hibernate.Session;

import de.tuclausthal.submissioninterface.answerclasses.java.JavaCheckerRegistry;
import de.tuclausthal.submissioninterface.answerclasses.java.JavaLanguageProvider;
import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.util.TaskPath;
import de.tuclausthal.submissioninterface.util.Util;

/**
 * Runner/Service that connects the language-neutral {@link AnswerClassPipeline} to the real
 * world: it loads the answer classes and submission path from the DB/disk, picks a
 * {@link LanguageProvider} + {@link CheckerRegistry}, and stores results via a
 * {@link DaoResultSink}. This is the "Runner/Service" from the antwortklassen
 * persistence-binding step; see {@code docs/antwortklassen-pipeline.puml} for the target flow.
 *
 * <p><b>Transaction handling:</b> like {@code PerformStudentTest}/{@code TestCountDAO}, this
 * class does not open/commit its own transaction. The caller (later: a web controller or a
 * batch job) is expected to {@code session.beginTransaction()} before and
 * {@code commit()}/{@code rollback()} after calling {@link #run}. This keeps run-limit checks
 * (which belong in the web controller, not here) and result persistence in one atomic unit,
 * and makes this class trivially testable with a rollback.</p>
 *
 * <p><b>Language selection:</b> only Java exists right now, so {@link #chooseLanguageProvider}
 * always returns a {@link JavaLanguageProvider}. Once more languages are added, this is the
 * single seam to dispatch on a real task/language attribute instead.</p>
 */
public class AnswerClassRunner {
	private final Session session;

	public AnswerClassRunner(Session session) {
		this.session = Objects.requireNonNull(session, "session");
	}

	/**
	 * Loads the answer classes for {@code submission}'s task, assembles the Java workspace
	 * from disk, runs the pipeline, and persists every result via {@link DaoResultSink}.
	 *
	 * @param trigger who/what started this run (STUDENT/TUTOR/BATCH)
	 * @param submission the submission to evaluate
	 * @param dataPath the GATE data root (i.e. {@code Configuration.getInstance().getDataPath()})
	 * @return the stored results, in the same order as the answer classes were evaluated
	 */
	public List<PipelineResult> run(PipelineTrigger trigger, Submission submission, Path dataPath) throws Exception {
		Objects.requireNonNull(trigger, "trigger");
		Objects.requireNonNull(submission, "submission");
		Objects.requireNonNull(dataPath, "dataPath");

		Task task = submission.getTask();
		Path submissionPath = Util.constructPath(dataPath, submission);
		boolean hasSubmissionFiles = Util.directoryHasFiles(submissionPath);

		List<AnswerClass> answerClasses = DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(task);
		List<AnswerClassToEvaluate> answerClassesToEvaluate = AnswerClassMapper.map(answerClasses);

		LanguageProvider languageProvider = chooseLanguageProvider(task, dataPath, submissionPath);
		CheckerRegistry checkerRegistry = chooseCheckerRegistry(task);
		ResultSink resultSink = new DaoResultSink(session, submission, answerClasses);

		return new AnswerClassPipeline().run(trigger, hasSubmissionFiles, answerClassesToEvaluate, languageProvider, checkerRegistry, resultSink);
	}

	/**
	 * Chooses the {@link LanguageProvider} for one pipeline run. Java-only for now.
	 */
	private static LanguageProvider chooseLanguageProvider(Task task, Path dataPath, Path submissionPath) {
		Path administrativePath = Util.constructPath(dataPath, task, TaskPath.ADVISORFILES);
		return new JavaLanguageProvider(submissionPath, administrativePath);
	}

	/**
	 * Chooses the {@link CheckerRegistry} matching the {@link LanguageProvider} above.
	 */
	private static CheckerRegistry chooseCheckerRegistry(Task task) {
		return JavaCheckerRegistry.createDefault();
	}
}
