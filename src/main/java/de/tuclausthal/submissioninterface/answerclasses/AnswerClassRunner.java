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
import de.tuclausthal.submissioninterface.persistence.datamodel.ProgrammingLanguage;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.util.TaskPath;
import de.tuclausthal.submissioninterface.util.Util;

/**
 * Loads the answer classes of a submission, runs the pipeline and stores the results.
 * preview() does not write anything. The caller owns the Hibernate transaction.
 * Only Java is supported so far.
 */
public class AnswerClassRunner {
	private final Session session;

	public AnswerClassRunner(Session session) {
		this.session = Objects.requireNonNull(session, "session");
	}

	/**
	 * Loads the answer classes for {@code submission}'s task, assembles the workspace
	 * from disk, runs the pipeline, and persists every result via {@link DaoResultSink}.
	 *
	 * @param trigger who/what started this run (STUDENT/TUTOR/BATCH)
	 * @param submission the submission to evaluate
	 * @param dataPath the GATE data root (i.e. {@code Configuration.getInstance().getDataPath()})
	 * @return the stored results, in the same order as the answer classes were evaluated
	 */
	public List<PipelineResult> run(PipelineTrigger trigger, Submission submission, Path dataPath) throws Exception {
		return execute(trigger, submission, dataPath, true);
	}

	/**
	 * Tutor check for one submission. Nothing is written to {@code answerclassresults}.
	 */
	public List<PipelineResult> preview(Submission submission, Path dataPath) throws Exception {
		return preview(PipelineTrigger.TUTOR, submission, dataPath);
	}

	/**
	 * Pipeline run that is shown to the caller and not stored. Student runs use
	 * {@link PipelineTrigger#STUDENT}; tutor checks use {@link PipelineTrigger#TUTOR}.
	 * Batch results and tutor grades stay the only persisted outcomes.
	 */
	public List<PipelineResult> preview(PipelineTrigger trigger, Submission submission, Path dataPath) throws Exception {
		return execute(trigger, submission, dataPath, false);
	}

	private List<PipelineResult> execute(PipelineTrigger trigger, Submission submission, Path dataPath, boolean persist) throws Exception {
		Objects.requireNonNull(trigger, "trigger");
		Objects.requireNonNull(submission, "submission");
		Objects.requireNonNull(dataPath, "dataPath");

		Task task = submission.getTask();
		Path submissionPath = Util.constructPath(dataPath, submission);
		boolean hasSubmissionFiles = Util.directoryHasFiles(submissionPath);

		List<AnswerClass> answerClasses = DAOFactory.AnswerClassDAOIf(session).getAnswerClassesForTask(task);
		List<AnswerClassToEvaluate> answerClassesToEvaluate = AnswerClassMapper.map(answerClasses);

		ProgrammingLanguage language = DAOFactory.AnswerClassDAOIf(session).getProgrammingLanguageForTask(task);
		LanguageProvider languageProvider = chooseLanguageProvider(language, task, dataPath, submissionPath);
		CheckerRegistry checkerRegistry = chooseCheckerRegistry(language);
		ResultSink resultSink = persist ? new DaoResultSink(session, submission, answerClasses) : ignored -> {
			// Preview is shown once and must not overwrite answerclassresults.
		};

		return new AnswerClassPipeline().run(trigger, hasSubmissionFiles, answerClassesToEvaluate, languageProvider, checkerRegistry, resultSink);
	}

	/**
	 * Chooses the {@link LanguageProvider} for one pipeline run.
	 */
	private static LanguageProvider chooseLanguageProvider(ProgrammingLanguage language, Task task, Path dataPath, Path submissionPath) {
		Path administrativePath = Util.constructPath(dataPath, task, TaskPath.ADVISORFILES);
		return switch (language == null ? ProgrammingLanguage.JAVA : language) {
			case JAVA -> new JavaLanguageProvider(submissionPath, administrativePath, dataPath);
		};
	}

	/**
	 * Chooses the {@link CheckerRegistry} matching the language above.
	 */
	private static CheckerRegistry chooseCheckerRegistry(ProgrammingLanguage language) {
		return switch (language == null ? ProgrammingLanguage.JAVA : language) {
			case JAVA -> JavaCheckerRegistry.createDefault();
		};
	}
}
