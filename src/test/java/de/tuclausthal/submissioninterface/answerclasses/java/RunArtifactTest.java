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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.tuclausthal.submissioninterface.testframework.executor.TestExecutorTestResult;
import de.tuclausthal.submissioninterface.testframework.tests.impl.JavaSyntaxTest;

/**
 * Exercises the RUN artifact's host-side fallback (this dev/CI environment has no safe-docker,
 * same assumption as {@code JavaLanguageProviderTest}). The sandboxed path is only exercised
 * indirectly (same {@code SafeDockerRunner} the BUILD tests already cover).
 */
class RunArtifactTest {
	@TempDir
	private Path tempDir;
	private Path classesDir;
	private Path dataPath;

	@BeforeEach
	void setUp() throws Exception {
		Path studentSources = resourceDir("answerclasses/java/functiontest");
		classesDir = tempDir.resolve("classes");
		Files.createDirectories(classesDir);
		assertTrue(JavaSyntaxTest.compileJava(studentSources, null, classesDir, new TestExecutorTestResult()));

		dataPath = tempDir.resolve("data");
		Files.createDirectories(dataPath);
		Files.copy(Path.of("SecurityManager/NoExitSecurityManager.jar"), dataPath.resolve("NoExitSecurityManager.jar"));
	}

	@Test
	void runsTestCodeAgainstCompiledStudentClasses() throws Exception {
		RunArtifact run = new RunArtifact(tempDir, classesDir, false, dataPath);

		RunArtifact.Outcome outcome = run.execute("System.out.println(Adder.add(2, 3));", 10);

		assertTrue(outcome.isAvailable());
		assertFalse(outcome.hasCompileError());
		assertTrue(outcome.isExitedCleanly());
		assertFalse(outcome.isTimedOut());
		assertEquals("5", outcome.getStdOut().trim());
	}

	@Test
	void reportsCompileErrorInTestCode() throws Exception {
		RunArtifact run = new RunArtifact(tempDir, classesDir, false, dataPath);

		RunArtifact.Outcome outcome = run.execute("this is not valid java;", 10);

		assertTrue(outcome.hasCompileError());
		assertFalse(outcome.getDiagnostics().isBlank());
	}

	@Test
	void notAvailableWithoutAnySandbox() throws Exception {
		Path emptyDataPath = tempDir.resolve("no-securitymanager-here");
		Files.createDirectories(emptyDataPath);
		RunArtifact run = new RunArtifact(tempDir, classesDir, false, emptyDataPath);

		RunArtifact.Outcome outcome = run.execute("System.out.println(1);", 10);

		assertFalse(outcome.isAvailable());
		assertFalse(outcome.getDiagnostics().isBlank());
	}

	@Test
	void isHostSandboxAvailableChecksForSecurityManagerJar() {
		assertTrue(RunArtifact.isHostSandboxAvailable(dataPath));
		assertFalse(RunArtifact.isHostSandboxAvailable(tempDir.resolve("does-not-exist")));
		assertFalse(RunArtifact.isHostSandboxAvailable(null));
	}

	private static Path resourceDir(String relative) throws URISyntaxException {
		return Path.of(RunArtifactTest.class.getClassLoader().getResource(relative).toURI());
	}
}
