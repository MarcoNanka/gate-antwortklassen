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
		assertEquals("while loop present", results.get(1).getDetails());
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

	private static Path resourceDir(String relative) throws URISyntaxException {
		return Path.of(JavaLanguageProviderTest.class.getClassLoader().getResource(relative).toURI());
	}
}
