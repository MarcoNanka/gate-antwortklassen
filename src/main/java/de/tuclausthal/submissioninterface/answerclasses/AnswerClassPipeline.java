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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;

/**
 * Language-neutral answer-class evaluation pipeline.
 * Run-limit checks belong in the web controller, not here.
 */
public class AnswerClassPipeline {

	/**
	 * Execute one pipeline run.
	 *
	 * @return unmodifiable list of stored results (same order as sink calls)
	 */
	public List<PipelineResult> run(PipelineTrigger trigger, boolean hasSubmissionFiles, List<AnswerClassToEvaluate> answerClasses, LanguageProvider languageProvider, CheckerRegistry checkerRegistry, ResultSink resultSink) throws Exception {
		return run(trigger, hasSubmissionFiles, answerClasses, languageProvider, checkerRegistry, resultSink, Map.of());
	}

	/**
	 * Execute one pipeline run.
	 *
	 * @param preservedManualStatus tutor grades already stored for manual answer classes
	 *        (answer-class id to status). Those classes are not written again; their status is
	 *        only kept so later classes can use it as a prerequisite. Empty for every run that
	 *        does not persist a batch.
	 * @return unmodifiable list of stored results (same order as sink calls)
	 */
	public List<PipelineResult> run(PipelineTrigger trigger, boolean hasSubmissionFiles, List<AnswerClassToEvaluate> answerClasses, LanguageProvider languageProvider, CheckerRegistry checkerRegistry, ResultSink resultSink, Map<Integer, AnswerClassResultStatus> preservedManualStatus) throws Exception {
		Objects.requireNonNull(trigger, "trigger");
		Objects.requireNonNull(answerClasses, "answerClasses");
		Objects.requireNonNull(languageProvider, "languageProvider");
		Objects.requireNonNull(checkerRegistry, "checkerRegistry");
		Objects.requireNonNull(resultSink, "resultSink");
		Objects.requireNonNull(preservedManualStatus, "preservedManualStatus");

		if (!hasSubmissionFiles) {
			return List.of();
		}

		EvaluationContext context = new EvaluationContext(trigger);
		List<PipelineResult> results = new ArrayList<>();
		Map<Integer, AnswerClassResultStatus> statusByAnswerClassId = new HashMap<>();
		// Prerequisites are checked only against results already produced in this run, so a
		// dependent created before its prerequisite (lower id) would otherwise be SKIPPED.
		List<AnswerClassToEvaluate> evaluationOrder = orderByPrerequisites(answerClasses);

		languageProvider.assembleWorkspace(context);
		languageProvider.startExecutionEnv(context);
		try {
			for (AnswerClassToEvaluate answerClass : evaluationOrder) {
				if (!isVisibleForTrigger(trigger, answerClass)) {
					continue;
				}

				// A tutor grade is the result. The batch must not replace it with PENDING or
				// SKIPPED, and a class that depends on this one has to see the tutor's status.
				if (answerClass.isManual()) {
					AnswerClassResultStatus preserved = preservedManualStatus.get(answerClass.getId());
					if (preserved != null) {
						statusByAnswerClassId.put(answerClass.getId(), preserved);
						continue;
					}
				}

				if (!prerequisitesSatisfied(answerClass, statusByAnswerClassId)) {
					store(results, statusByAnswerClassId, resultSink, answerClass.getId(), AnswerClassResultStatus.SKIPPED, "prerequisites not satisfied", sourceForTrigger(trigger));
					continue;
				}

				if (answerClass.isManual()) {
					store(results, statusByAnswerClassId, resultSink, answerClass.getId(), AnswerClassResultStatus.PENDING, null, AnswerClassResultSource.MANUAL);
					continue;
				}

				Checker checker = checkerRegistry.get(answerClass.getDetectionType());
				if (checker == null) {
					store(results, statusByAnswerClassId, resultSink, answerClass.getId(), AnswerClassResultStatus.SKIPPED, "unknown detectionType: " + answerClass.getDetectionType(), sourceForTrigger(trigger));
					continue;
				}

				boolean artifactsAvailable = true;
				for (ArtifactType artifactType : checker.requiredArtifacts()) {
					if (!context.hasArtifact(artifactType) && !languageProvider.ensureArtifact(artifactType, context)) {
						artifactsAvailable = false;
						break;
					}
					if (!context.hasArtifact(artifactType)) {
						artifactsAvailable = false;
						break;
					}
				}

				if (!artifactsAvailable) {
					store(results, statusByAnswerClassId, resultSink, answerClass.getId(), AnswerClassResultStatus.SKIPPED, "required artifacts unavailable", sourceForTrigger(trigger));
					continue;
				}

				CheckerOutcome outcome = checker.check(context, answerClass);
				store(results, statusByAnswerClassId, resultSink, answerClass.getId(), outcome.getStatus(), outcome.getDetails(), sourceForTrigger(trigger));
			}
		} finally {
			languageProvider.cleanupExecutionEnv(context);
		}

		return Collections.unmodifiableList(results);
	}

	static boolean isVisibleForTrigger(PipelineTrigger trigger, AnswerClassToEvaluate answerClass) {
		if (trigger == PipelineTrigger.STUDENT) {
			return answerClass.getTimesRunnableByStudents() > 0;
		}
		return true;
	}

	/**
	 * Kahn topological order: every prerequisite that is part of this run is evaluated before
	 * the answer class that depends on it. Relative order is kept when there is no dependency.
	 * A cycle (or a prerequisite outside this list) is left in the original order; those answer
	 * classes then SKIP as usual because the prerequisite is never MATCHED first.
	 */
	static List<AnswerClassToEvaluate> orderByPrerequisites(List<AnswerClassToEvaluate> answerClasses) {
		Map<Integer, AnswerClassToEvaluate> byId = new LinkedHashMap<>();
		for (AnswerClassToEvaluate answerClass : answerClasses) {
			byId.put(answerClass.getId(), answerClass);
		}
		Map<Integer, Integer> indegree = new HashMap<>();
		Map<Integer, List<Integer>> dependents = new HashMap<>();
		for (AnswerClassToEvaluate answerClass : answerClasses) {
			indegree.putIfAbsent(answerClass.getId(), 0);
			for (Integer prerequisiteId : answerClass.getPrerequisiteIds()) {
				if (!byId.containsKey(prerequisiteId) || prerequisiteId == answerClass.getId()) {
					continue;
				}
				dependents.computeIfAbsent(prerequisiteId, id -> new ArrayList<>()).add(answerClass.getId());
				indegree.merge(answerClass.getId(), 1, Integer::sum);
			}
		}
		Deque<Integer> ready = new ArrayDeque<>();
		for (AnswerClassToEvaluate answerClass : answerClasses) {
			if (indegree.get(answerClass.getId()) == 0) {
				ready.add(answerClass.getId());
			}
		}
		List<AnswerClassToEvaluate> ordered = new ArrayList<>(answerClasses.size());
		while (!ready.isEmpty()) {
			AnswerClassToEvaluate next = byId.get(ready.removeFirst());
			ordered.add(next);
			for (Integer dependentId : dependents.getOrDefault(next.getId(), List.of())) {
				if (indegree.merge(dependentId, -1, Integer::sum) == 0) {
					ready.addLast(dependentId);
				}
			}
		}
		if (ordered.size() < byId.size()) {
			for (AnswerClassToEvaluate answerClass : answerClasses) {
				if (ordered.stream().noneMatch(placed -> placed.getId() == answerClass.getId())) {
					ordered.add(answerClass);
				}
			}
		}
		return ordered;
	}

	static boolean prerequisitesSatisfied(AnswerClassToEvaluate answerClass, Map<Integer, AnswerClassResultStatus> statusByAnswerClassId) {
		for (Integer prerequisiteId : answerClass.getPrerequisiteIds()) {
			if (statusByAnswerClassId.get(prerequisiteId) != AnswerClassResultStatus.MATCHED) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Map pipeline trigger to persisted result source.
	 * TUTOR runs use {@link AnswerClassResultSource#MANUAL} (tutor path).
	 */
	static AnswerClassResultSource sourceForTrigger(PipelineTrigger trigger) {
		return switch (trigger) {
			case STUDENT -> AnswerClassResultSource.STUDENT_PREVIEW;
			case TUTOR -> AnswerClassResultSource.MANUAL;
			case BATCH -> AnswerClassResultSource.AUTOMATIC;
		};
	}

	private static void store(List<PipelineResult> results, Map<Integer, AnswerClassResultStatus> statusByAnswerClassId, ResultSink resultSink, int answerClassId, AnswerClassResultStatus status, String details, AnswerClassResultSource source) {
		PipelineResult result = new PipelineResult(answerClassId, status, details, source);
		statusByAnswerClassId.put(answerClassId, status);
		resultSink.store(result);
		results.add(result);
	}
}
