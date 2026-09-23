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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;

/**
 * Specifies the language-neutral answer-class pipeline (UML activity diagram).
 */
class AnswerClassPipelineTest {
	private AnswerClassPipeline pipeline;
	private List<PipelineResult> sinkResults;
	private ResultSink sink;

	@BeforeEach
	void setUp() {
		pipeline = new AnswerClassPipeline();
		sinkResults = new ArrayList<>();
		sink = sinkResults::add;
	}

	@Test
	void noFilesStopsWithoutResultsOrEnv() throws Exception {
		FakeLanguageProvider provider = new FakeLanguageProvider(ArtifactType.AST);
		CheckerRegistry registry = registryWithAstChecker("WHILE_LOOP", CheckerOutcome.matched("hit"));

		List<PipelineResult> results = pipeline.run(PipelineTrigger.STUDENT, false, List.of(new AnswerClassToEvaluate(1, "WHILE_LOOP", 3)), provider, registry, sink);

		assertTrue(results.isEmpty());
		assertTrue(sinkResults.isEmpty());
		assertEquals(0, provider.getAssembleCalls());
		assertEquals(0, provider.getStartEnvCalls());
		assertEquals(0, provider.getCleanupCalls());
	}

	@Test
	void studentHidesAnswerClassWithZeroRuns() throws Exception {
		FakeLanguageProvider provider = new FakeLanguageProvider(ArtifactType.AST);
		CheckerRegistry registry = registryWithAstChecker("WHILE_LOOP", CheckerOutcome.matched("hit"));

		List<PipelineResult> results = pipeline.run(PipelineTrigger.STUDENT, true, List.of(new AnswerClassToEvaluate(1, "WHILE_LOOP", 0)), provider, registry, sink);

		assertTrue(results.isEmpty());
		assertEquals(1, provider.getAssembleCalls());
		assertEquals(1, provider.getStartEnvCalls());
		assertEquals(1, provider.getCleanupCalls());
	}

	@Test
	void tutorSeesHiddenFromStudentAnswerClass() throws Exception {
		FakeLanguageProvider provider = new FakeLanguageProvider(ArtifactType.AST);
		CheckerRegistry registry = registryWithAstChecker("WHILE_LOOP", CheckerOutcome.matched("hit"));

		List<PipelineResult> results = pipeline.run(PipelineTrigger.TUTOR, true, List.of(new AnswerClassToEvaluate(1, "WHILE_LOOP", 0)), provider, registry, sink);

		assertEquals(1, results.size());
		assertEquals(AnswerClassResultStatus.MATCHED, results.get(0).getStatus());
		assertEquals(AnswerClassResultSource.MANUAL, results.get(0).getSource());
	}

	@Test
	void dependentListedBeforePrerequisiteStillRunsAfterItMatches() throws Exception {
		FakeLanguageProvider provider = new FakeLanguageProvider(ArtifactType.AST);
		CheckerRegistry registry = new CheckerRegistry();
		registry.register("COMPILE_OK", astChecker(CheckerOutcome.matched("compiles")));
		registry.register("WHILE_LOOP", astChecker(CheckerOutcome.matched("while found")));

		// created first, but depends on the compile answer class that was created later
		AnswerClassToEvaluate whileAc = new AnswerClassToEvaluate(1, "WHILE_LOOP", 3, List.of(2));
		AnswerClassToEvaluate compileAc = new AnswerClassToEvaluate(2, "COMPILE_OK", 3);

		List<PipelineResult> results = pipeline.run(PipelineTrigger.STUDENT, true, List.of(whileAc, compileAc), provider, registry, sink);

		assertEquals(2, results.size());
		assertEquals(2, results.get(0).getAnswerClassId());
		assertEquals(AnswerClassResultStatus.MATCHED, results.get(0).getStatus());
		assertEquals(1, results.get(1).getAnswerClassId());
		assertEquals(AnswerClassResultStatus.MATCHED, results.get(1).getStatus());
	}

	@Test
	void missingPrerequisiteStoresSkipped() throws Exception {
		FakeLanguageProvider provider = new FakeLanguageProvider(ArtifactType.AST);
		CheckerRegistry registry = registryWithAstChecker("UNIT_STYLE", CheckerOutcome.matched("ok"));

		AnswerClassToEvaluate compileAc = new AnswerClassToEvaluate(1, "COMPILE_OK", 3);
		AnswerClassToEvaluate unitAc = new AnswerClassToEvaluate(2, "UNIT_STYLE", 3, List.of(1));
		registry.register("COMPILE_OK", astChecker(CheckerOutcome.notMatched("does not compile")));

		List<PipelineResult> results = pipeline.run(PipelineTrigger.BATCH, true, List.of(compileAc, unitAc), provider, registry, sink);

		assertEquals(2, results.size());
		assertEquals(AnswerClassResultStatus.NOT_MATCHED, results.get(0).getStatus());
		assertEquals(AnswerClassResultStatus.SKIPPED, results.get(1).getStatus());
		assertEquals(AnswerClassResultSource.AUTOMATIC, results.get(1).getSource());
	}

	@Test
	void manualDetectionTypeStoresPendingWithManualSource() throws Exception {
		FakeLanguageProvider provider = new FakeLanguageProvider();
		CheckerRegistry registry = new CheckerRegistry();

		List<PipelineResult> results = pipeline.run(PipelineTrigger.STUDENT, true, List.of(new AnswerClassToEvaluate(1, AnswerClassToEvaluate.MANUAL_DETECTION_TYPE, 2)), provider, registry, sink);

		assertEquals(1, results.size());
		assertEquals(AnswerClassResultStatus.PENDING, results.get(0).getStatus());
		assertEquals(AnswerClassResultSource.MANUAL, results.get(0).getSource());
		assertEquals(0, provider.getEnsureCalls(ArtifactType.AST));
	}

	@Test
	void missingArtifactStoresSkipped() throws Exception {
		FakeLanguageProvider provider = new FakeLanguageProvider(); // AST not available
		CheckerRegistry registry = registryWithAstChecker("WHILE_LOOP", CheckerOutcome.matched("hit"));

		List<PipelineResult> results = pipeline.run(PipelineTrigger.BATCH, true, List.of(new AnswerClassToEvaluate(1, "WHILE_LOOP", 3)), provider, registry, sink);

		assertEquals(1, results.size());
		assertEquals(AnswerClassResultStatus.SKIPPED, results.get(0).getStatus());
		assertEquals(1, provider.getEnsureCalls(ArtifactType.AST));
	}

	@Test
	void happyPathMatchedWithStudentPreviewSource() throws Exception {
		FakeLanguageProvider provider = new FakeLanguageProvider(ArtifactType.AST);
		CheckerRegistry registry = registryWithAstChecker("WHILE_LOOP", CheckerOutcome.matched("while found"));

		List<PipelineResult> results = pipeline.run(PipelineTrigger.STUDENT, true, List.of(new AnswerClassToEvaluate(1, "WHILE_LOOP", 3)), provider, registry, sink);

		assertEquals(1, results.size());
		assertEquals(1, results.get(0).getAnswerClassId());
		assertEquals(AnswerClassResultStatus.MATCHED, results.get(0).getStatus());
		assertEquals("while found", results.get(0).getDetails());
		assertEquals(AnswerClassResultSource.STUDENT_PREVIEW, results.get(0).getSource());
	}

	@Test
	void artifactProducedOnlyOnceWhenTwoCheckersNeedSameArtifact() throws Exception {
		FakeLanguageProvider provider = new FakeLanguageProvider(ArtifactType.AST);
		CheckerRegistry registry = new CheckerRegistry();
		registry.register("A", astChecker(CheckerOutcome.matched("a")));
		registry.register("B", astChecker(CheckerOutcome.notMatched("b")));

		pipeline.run(PipelineTrigger.BATCH, true, List.of(new AnswerClassToEvaluate(1, "A", 1), new AnswerClassToEvaluate(2, "B", 1)), provider, registry, sink);

		assertEquals(1, provider.getEnsureCalls(ArtifactType.AST));
		assertEquals(2, sinkResults.size());
	}

	@Test
	void unknownDetectionTypeStoresSkipped() throws Exception {
		FakeLanguageProvider provider = new FakeLanguageProvider(ArtifactType.AST);
		CheckerRegistry registry = new CheckerRegistry();

		List<PipelineResult> results = pipeline.run(PipelineTrigger.BATCH, true, List.of(new AnswerClassToEvaluate(1, "NOT_REGISTERED", 1)), provider, registry, sink);

		assertEquals(AnswerClassResultStatus.SKIPPED, results.get(0).getStatus());
	}

	@Test
	void cleanupRunsEvenWhenCheckerThrows() {
		FakeLanguageProvider provider = new FakeLanguageProvider(ArtifactType.AST);
		CheckerRegistry registry = new CheckerRegistry();
		registry.register("BOOM", new Checker() {
			@Override
			public Set<ArtifactType> requiredArtifacts() {
				return EnumSet.of(ArtifactType.AST);
			}

			@Override
			public CheckerOutcome check(EvaluationContext context, AnswerClassToEvaluate answerClass) {
				throw new IllegalStateException("boom");
			}
		});

		try {
			pipeline.run(PipelineTrigger.BATCH, true, List.of(new AnswerClassToEvaluate(1, "BOOM", 1)), provider, registry, sink);
		} catch (Exception expected) {
			assertEquals("boom", expected.getMessage());
		}
		assertEquals(1, provider.getCleanupCalls());
	}

	private static CheckerRegistry registryWithAstChecker(String detectionType, CheckerOutcome outcome) {
		CheckerRegistry registry = new CheckerRegistry();
		registry.register(detectionType, astChecker(outcome));
		return registry;
	}

	private static Checker astChecker(CheckerOutcome outcome) {
		return new Checker() {
			@Override
			public Set<ArtifactType> requiredArtifacts() {
				return EnumSet.of(ArtifactType.AST);
			}

			@Override
			public CheckerOutcome check(EvaluationContext context, AnswerClassToEvaluate answerClass) {
				return outcome;
			}
		};
	}
}
