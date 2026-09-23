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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.tuclausthal.submissioninterface.answerclasses.AnswerClassToEvaluate;
import de.tuclausthal.submissioninterface.answerclasses.ArtifactType;
import de.tuclausthal.submissioninterface.answerclasses.CheckerOutcome;
import de.tuclausthal.submissioninterface.answerclasses.EvaluationContext;
import de.tuclausthal.submissioninterface.answerclasses.PipelineTrigger;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;
import de.tuclausthal.submissioninterface.testframework.executor.TestExecutorTestResult;
import de.tuclausthal.submissioninterface.testframework.tests.impl.JavaSyntaxTest;

class FunctionTestCheckerTest {
	@TempDir
	private Path tempDir;
	private EvaluationContext context;
	private final FunctionTestChecker checker = new FunctionTestChecker();

	@BeforeEach
	void setUp() throws Exception {
		Path studentSources = resourceDir("answerclasses/java/functiontest");
		Path classesDir = tempDir.resolve("classes");
		Files.createDirectories(classesDir);
		assertTrue(JavaSyntaxTest.compileJava(studentSources, null, classesDir, new TestExecutorTestResult()));

		Path dataPath = tempDir.resolve("data");
		Files.createDirectories(dataPath);
		Files.copy(Path.of("SecurityManager/NoExitSecurityManager.jar"), dataPath.resolve("NoExitSecurityManager.jar"));

		context = new EvaluationContext(PipelineTrigger.BATCH);
		context.putArtifact(ArtifactType.RUN, new RunArtifact(tempDir, classesDir, false, dataPath));
	}

	@Test
	void matchedWhenOutputEqualsExpected() {
		AnswerClassToEvaluate answerClass = AnswerClassToEvaluate.builder(1, JavaDetectionTypes.FUNCTION_TEST, 3).functionTestCode("System.out.println(Adder.add(2, 3));").functionTestExpectedOutput("5").build();

		CheckerOutcome outcome = checker.check(context, answerClass);

		assertTrue(outcome.getStatus() == AnswerClassResultStatus.MATCHED, outcome.getDetails());
		assertTrue(outcome.getDetails().contains("System.out.println(Adder.add(2, 3));"));
		assertTrue(outcome.getDetails().contains("Ausgabe:\n5"));
	}

	@Test
	void notMatchedWhenOutputDiffers() {
		AnswerClassToEvaluate answerClass = AnswerClassToEvaluate.builder(1, JavaDetectionTypes.FUNCTION_TEST, 3).functionTestCode("System.out.println(Adder.add(2, 3));").functionTestExpectedOutput("42").build();

		CheckerOutcome outcome = checker.check(context, answerClass);

		assertTrue(outcome.getStatus() == AnswerClassResultStatus.NOT_MATCHED);
		assertTrue(outcome.getDetails().contains("System.out.println(Adder.add(2, 3));"));
		assertTrue(outcome.getDetails().contains("42"));
		assertTrue(outcome.getDetails().contains("5"));
	}

	@Test
	void notMatchedWhenTestCodeMissing() {
		AnswerClassToEvaluate answerClass = AnswerClassToEvaluate.builder(1, JavaDetectionTypes.FUNCTION_TEST, 3).functionTestExpectedOutput("5").build();

		CheckerOutcome outcome = checker.check(context, answerClass);

		assertTrue(outcome.getStatus() == AnswerClassResultStatus.NOT_MATCHED);
	}

	@Test
	void notMatchedWhenTestCodeDoesNotCompile() {
		AnswerClassToEvaluate answerClass = AnswerClassToEvaluate.builder(1, JavaDetectionTypes.FUNCTION_TEST, 3).functionTestCode("this is not valid java;").functionTestExpectedOutput("5").build();

		CheckerOutcome outcome = checker.check(context, answerClass);

		assertTrue(outcome.getStatus() == AnswerClassResultStatus.NOT_MATCHED);
	}

	private static Path resourceDir(String relative) throws URISyntaxException {
		return Path.of(FunctionTestCheckerTest.class.getClassLoader().getResource(relative).toURI());
	}
}
