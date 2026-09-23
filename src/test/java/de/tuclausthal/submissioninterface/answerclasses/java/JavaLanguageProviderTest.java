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

package de.tuclausthal.submissioninterface.answerclasses.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.tuclausthal.submissioninterface.answerclasses.AnswerClassPipeline;
import de.tuclausthal.submissioninterface.answerclasses.AnswerClassToEvaluate;
import de.tuclausthal.submissioninterface.answerclasses.ArtifactType;
import de.tuclausthal.submissioninterface.answerclasses.EvaluationContext;
import de.tuclausthal.submissioninterface.answerclasses.PipelineResult;
import de.tuclausthal.submissioninterface.answerclasses.PipelineTrigger;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;

class JavaLanguageProviderTest {
	@Test
	void assembleBuildAstAndCleanup() throws Exception {
		Path submission = resourceDir("answerclasses/java/hello");
		JavaLanguageProvider provider = new JavaLanguageProvider(submission);
		EvaluationContext context = new EvaluationContext(PipelineTrigger.STUDENT);

		provider.assembleWorkspace(context);
		Path workspace = context.getWorkspaceRoot();
		assertTrue(Files.isDirectory(workspace.resolve(JavaLanguageProvider.STUDENT_DIR)));
		assertTrue(Files.isRegularFile(workspace.resolve(JavaLanguageProvider.STUDENT_DIR).resolve("HelloWorld.java")));

		assertTrue(provider.ensureArtifact(ArtifactType.BUILD, context));
		BuildArtifact build = context.<BuildArtifact>getArtifact(ArtifactType.BUILD).orElseThrow();
		assertTrue(build.isSuccessful());
		assertTrue(provider.ensureArtifact(ArtifactType.BUILD, context)); // idempotent

		assertTrue(provider.ensureArtifact(ArtifactType.AST, context));
		AstArtifact ast = context.<AstArtifact>getArtifact(ArtifactType.AST).orElseThrow();
		assertEquals(1, ast.getCompilationUnits().size());

		provider.cleanupExecutionEnv(context);
		assertFalse(Files.exists(workspace));
	}

	@Test
	void buildFailsForBrokenSources() throws Exception {
		JavaLanguageProvider provider = new JavaLanguageProvider(resourceDir("answerclasses/java/broken"));
		EvaluationContext context = new EvaluationContext(PipelineTrigger.BATCH);
		provider.assembleWorkspace(context);
		try {
			assertTrue(provider.ensureArtifact(ArtifactType.BUILD, context));
			BuildArtifact build = context.<BuildArtifact>getArtifact(ArtifactType.BUILD).orElseThrow();
			assertFalse(build.isSuccessful());
			assertFalse(build.getDiagnostics().isBlank());
		} finally {
			provider.cleanupExecutionEnv(context);
		}
	}

	@Test
	void startExecutionEnvDetectsSandboxAvailability() throws Exception {
		JavaLanguageProvider provider = new JavaLanguageProvider(resourceDir("answerclasses/java/hello"));
		EvaluationContext context = new EvaluationContext(PipelineTrigger.STUDENT);
		provider.assembleWorkspace(context);
		try {
			assertFalse(provider.isSandboxAvailable()); // not started yet
			provider.startExecutionEnv(context);
			// this dev/CI environment does not have safe-docker installed - host javac fallback
			assertEquals(SafeDockerRunner.isAvailable(), provider.isSandboxAvailable());
			assertFalse(provider.isSandboxAvailable());
		} finally {
			provider.cleanupExecutionEnv(context);
			assertFalse(provider.isSandboxAvailable()); // reset on cleanup
		}
	}

	@Test
	void astRequiresSuccessfulBuildRegardlessOfAnswerClassPrerequisites() throws Exception {
		// Provider-Policy (docs/antwortklassen-pipeline.puml): AST is withheld once BUILD fails,
		// even if nobody asked for BUILD first and no answer-class-level prerequisite exists.
		JavaLanguageProvider provider = new JavaLanguageProvider(resourceDir("answerclasses/java/broken"));
		EvaluationContext context = new EvaluationContext(PipelineTrigger.BATCH);
		provider.assembleWorkspace(context);
		try {
			assertFalse(provider.ensureArtifact(ArtifactType.AST, context));
			assertFalse(context.hasArtifact(ArtifactType.AST));

			// BUILD was produced as a side effect of resolving the AST dependency, and cached
			assertTrue(context.hasArtifact(ArtifactType.BUILD));
			assertFalse(context.<BuildArtifact>getArtifact(ArtifactType.BUILD).orElseThrow().isSuccessful());
		} finally {
			provider.cleanupExecutionEnv(context);
		}
	}

	@Test
	void astSucceedsOnceBuildSucceeds() throws Exception {
		JavaLanguageProvider provider = new JavaLanguageProvider(resourceDir("answerclasses/java/whileloop"));
		EvaluationContext context = new EvaluationContext(PipelineTrigger.BATCH);
		provider.assembleWorkspace(context);
		try {
			assertTrue(provider.ensureArtifact(ArtifactType.AST, context));
			assertTrue(context.hasArtifact(ArtifactType.BUILD));
			assertTrue(context.<BuildArtifact>getArtifact(ArtifactType.BUILD).orElseThrow().isSuccessful());
			assertEquals(1, context.<AstArtifact>getArtifact(ArtifactType.AST).orElseThrow().getCompilationUnits().size());
		} finally {
			provider.cleanupExecutionEnv(context);
		}
	}

	@Test
	void pipelineCompileAndWhileLoop() throws Exception {
		Path submission = resourceDir("answerclasses/java/whileloop");
		JavaLanguageProvider provider = new JavaLanguageProvider(submission);
		List<AnswerClassToEvaluate> answerClasses = List.of(new AnswerClassToEvaluate(1, JavaDetectionTypes.COMPILE_OK, 2, List.of()), new AnswerClassToEvaluate(2, JavaDetectionTypes.WHILE_LOOP, 2, List.of(1)));

		List<PipelineResult> results = new AnswerClassPipeline().run(PipelineTrigger.STUDENT, true, answerClasses, provider, JavaCheckerRegistry.createDefault(), result -> {
		});

		assertEquals(2, results.size());
		assertEquals(AnswerClassResultStatus.MATCHED, results.get(0).getStatus());
		assertEquals(AnswerClassResultSource.STUDENT_PREVIEW, results.get(0).getSource());
		assertEquals(AnswerClassResultStatus.MATCHED, results.get(1).getStatus());
		assertEquals("1 while-Schleife(n) gefunden (erwartet: >= 1)", results.get(1).getDetails());
	}

	@Test
	void pipelineCompileFailsSkipsWhilePrerequisite() throws Exception {
		Path submission = resourceDir("answerclasses/java/broken");
		JavaLanguageProvider provider = new JavaLanguageProvider(submission);
		List<AnswerClassToEvaluate> answerClasses = List.of(new AnswerClassToEvaluate(1, JavaDetectionTypes.COMPILE_OK, 2, List.of()), new AnswerClassToEvaluate(2, JavaDetectionTypes.WHILE_LOOP, 2, List.of(1)));

		List<PipelineResult> results = new AnswerClassPipeline().run(PipelineTrigger.BATCH, true, answerClasses, provider, JavaCheckerRegistry.createDefault(), result -> {
		});

		assertEquals(2, results.size());
		assertEquals(AnswerClassResultStatus.NOT_MATCHED, results.get(0).getStatus());
		assertEquals(AnswerClassResultStatus.SKIPPED, results.get(1).getStatus());
		assertEquals(AnswerClassResultSource.AUTOMATIC, results.get(1).getSource());
	}

	@Test
	void helloWorldHasNoWhile() throws Exception {
		Path submission = resourceDir("answerclasses/java/hello");
		JavaLanguageProvider provider = new JavaLanguageProvider(submission);
		List<AnswerClassToEvaluate> answerClasses = List.of(new AnswerClassToEvaluate(1, JavaDetectionTypes.COMPILE_OK, 1, List.of()), new AnswerClassToEvaluate(2, JavaDetectionTypes.WHILE_LOOP, 1, List.of()));

		List<PipelineResult> results = new AnswerClassPipeline().run(PipelineTrigger.TUTOR, true, answerClasses, provider, JavaCheckerRegistry.createDefault(), result -> {
		});

		assertEquals(AnswerClassResultStatus.MATCHED, results.get(0).getStatus());
		assertEquals(AnswerClassResultStatus.NOT_MATCHED, results.get(1).getStatus());
		assertEquals(AnswerClassResultSource.MANUAL, results.get(1).getSource());
	}

	@Test
	void whileLoopExactlyOneMatchesWithMinMaxOne() throws Exception {
		Path submission = resourceDir("answerclasses/java/whileloop"); // WithWhile.java has exactly 1 while loop
		JavaLanguageProvider provider = new JavaLanguageProvider(submission);
		List<AnswerClassToEvaluate> answerClasses = List.of(AnswerClassToEvaluate.builder(1, JavaDetectionTypes.WHILE_LOOP, 1).whileLoopRange(1, 1).build());

		List<PipelineResult> results = new AnswerClassPipeline().run(PipelineTrigger.BATCH, true, answerClasses, provider, JavaCheckerRegistry.createDefault(), result -> {
		});

		assertEquals(AnswerClassResultStatus.MATCHED, results.get(0).getStatus());
		assertEquals("1 while-Schleife(n) gefunden (erwartet: genau 1)", results.get(0).getDetails());
	}

	@Test
	void whileLoopForbiddenNotMatchedWhenMaxZero() throws Exception {
		Path submission = resourceDir("answerclasses/java/whileloop"); // WithWhile.java has exactly 1 while loop
		JavaLanguageProvider provider = new JavaLanguageProvider(submission);
		List<AnswerClassToEvaluate> answerClasses = List.of(AnswerClassToEvaluate.builder(1, JavaDetectionTypes.WHILE_LOOP, 1).whileLoopRange(0, 0).build());

		List<PipelineResult> results = new AnswerClassPipeline().run(PipelineTrigger.BATCH, true, answerClasses, provider, JavaCheckerRegistry.createDefault(), result -> {
		});

		assertEquals(AnswerClassResultStatus.NOT_MATCHED, results.get(0).getStatus());
		assertEquals("1 while-Schleife(n) gefunden (erwartet: genau 0)", results.get(0).getDetails());
	}

	@Test
	void whileLoopForbiddenMatchesWhenNoWhileLoopPresent() throws Exception {
		Path submission = resourceDir("answerclasses/java/hello"); // no while loop at all
		JavaLanguageProvider provider = new JavaLanguageProvider(submission);
		List<AnswerClassToEvaluate> answerClasses = List.of(AnswerClassToEvaluate.builder(1, JavaDetectionTypes.WHILE_LOOP, 1).whileLoopRange(0, 0).build());

		List<PipelineResult> results = new AnswerClassPipeline().run(PipelineTrigger.BATCH, true, answerClasses, provider, JavaCheckerRegistry.createDefault(), result -> {
		});

		assertEquals(AnswerClassResultStatus.MATCHED, results.get(0).getStatus());
	}

	@Test
	void forLoopExactlyOneMatchesWithMinMaxOne() throws Exception {
		Path submission = resourceDir("answerclasses/java/forloop"); // WithFor.java has exactly 1 for loop
		JavaLanguageProvider provider = new JavaLanguageProvider(submission);
		List<AnswerClassToEvaluate> answerClasses = List.of(AnswerClassToEvaluate.builder(1, JavaDetectionTypes.FOR_LOOP, 1).forLoopRange(1, 1).build());

		List<PipelineResult> results = new AnswerClassPipeline().run(PipelineTrigger.BATCH, true, answerClasses, provider, JavaCheckerRegistry.createDefault(), result -> {
		});

		assertEquals(AnswerClassResultStatus.MATCHED, results.get(0).getStatus());
		assertEquals("1 for-Schleife(n) gefunden (erwartet: genau 1)", results.get(0).getDetails());
	}

	@Test
	void forLoopForbiddenNotMatchedWhenMaxZero() throws Exception {
		Path submission = resourceDir("answerclasses/java/forloop"); // WithFor.java has exactly 1 for loop
		JavaLanguageProvider provider = new JavaLanguageProvider(submission);
		List<AnswerClassToEvaluate> answerClasses = List.of(AnswerClassToEvaluate.builder(1, JavaDetectionTypes.FOR_LOOP, 1).forLoopRange(0, 0).build());

		List<PipelineResult> results = new AnswerClassPipeline().run(PipelineTrigger.BATCH, true, answerClasses, provider, JavaCheckerRegistry.createDefault(), result -> {
		});

		assertEquals(AnswerClassResultStatus.NOT_MATCHED, results.get(0).getStatus());
		assertEquals("1 for-Schleife(n) gefunden (erwartet: genau 0)", results.get(0).getDetails());
	}

	@Test
	void whileLoopUsedInsteadOfForLoopIsDetected() throws Exception {
		// WithWhile.java solves the repetition with a while loop and contains no for loop at all
		// -> combining WHILE_LOOP min=1 with FOR_LOOP max=0 detects "solved via while, not for".
		Path submission = resourceDir("answerclasses/java/whileloop");
		JavaLanguageProvider provider = new JavaLanguageProvider(submission);
		List<AnswerClassToEvaluate> answerClasses = List.of(AnswerClassToEvaluate.builder(1, JavaDetectionTypes.WHILE_LOOP, 1).whileLoopRange(1, null).build(), AnswerClassToEvaluate.builder(2, JavaDetectionTypes.FOR_LOOP, 1).forLoopRange(0, 0).build());

		List<PipelineResult> results = new AnswerClassPipeline().run(PipelineTrigger.BATCH, true, answerClasses, provider, JavaCheckerRegistry.createDefault(), result -> {
		});

		assertEquals(2, results.size());
		assertEquals(AnswerClassResultStatus.MATCHED, results.get(0).getStatus());
		assertEquals(AnswerClassResultStatus.MATCHED, results.get(1).getStatus());
	}

	@Test
	void runArtifactUnavailableWithoutSandboxOrDataPath() throws Exception {
		// no safe-docker in this dev/CI environment, and no dataPath given -> no
		// NoExitSecurityManager.jar can be found either, so RUN must stay unavailable
		JavaLanguageProvider provider = new JavaLanguageProvider(resourceDir("answerclasses/java/functiontest"));
		EvaluationContext context = new EvaluationContext(PipelineTrigger.BATCH);
		provider.assembleWorkspace(context);
		try {
			assertFalse(provider.ensureArtifact(ArtifactType.RUN, context));
			assertFalse(context.hasArtifact(ArtifactType.RUN));
		} finally {
			provider.cleanupExecutionEnv(context);
		}
	}

	@Test
	void pipelineFunctionTestMatchesWithHostFallbackSandbox() throws Exception {
		Path submission = resourceDir("answerclasses/java/functiontest");
		Path dataPath = Files.createTempDirectory("functiontest-datapath");
		try {
			Files.copy(Path.of("SecurityManager/NoExitSecurityManager.jar"), dataPath.resolve("NoExitSecurityManager.jar"));
			JavaLanguageProvider provider = new JavaLanguageProvider(submission, null, dataPath);
			List<AnswerClassToEvaluate> answerClasses = List.of(AnswerClassToEvaluate.builder(1, JavaDetectionTypes.FUNCTION_TEST, 3).functionTestCode("System.out.println(Adder.add(2, 3));").functionTestExpectedOutput("5").build());

			List<PipelineResult> results = new AnswerClassPipeline().run(PipelineTrigger.BATCH, true, answerClasses, provider, JavaCheckerRegistry.createDefault(), result -> {
			});

			assertEquals(1, results.size());
			assertEquals(AnswerClassResultStatus.MATCHED, results.get(0).getStatus());
		} finally {
			de.tuclausthal.submissioninterface.util.Util.recursiveDelete(dataPath);
		}
	}

	@Test
	void pipelineSkipsWhileLoopOnBrokenBuildWithoutExplicitPrerequisite() throws Exception {
		// Same broken-build scenario as pipelineCompileFailsSkipsWhilePrerequisite, but WITHOUT
		// any AC-level prerequisiteIds - the provider policy alone must still cause SKIPPED.
		Path submission = resourceDir("answerclasses/java/broken");
		JavaLanguageProvider provider = new JavaLanguageProvider(submission);
		List<AnswerClassToEvaluate> answerClasses = List.of(new AnswerClassToEvaluate(2, JavaDetectionTypes.WHILE_LOOP, 2, List.of()));

		List<PipelineResult> results = new AnswerClassPipeline().run(PipelineTrigger.BATCH, true, answerClasses, provider, JavaCheckerRegistry.createDefault(), result -> {
		});

		assertEquals(1, results.size());
		assertEquals(AnswerClassResultStatus.SKIPPED, results.get(0).getStatus());
	}

	private static Path resourceDir(String relative) throws URISyntaxException {
		return Path.of(JavaLanguageProviderTest.class.getClassLoader().getResource(relative).toURI());
	}
}
