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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import de.tuclausthal.submissioninterface.testframework.tests.impl.DockerTest;

/**
 * Like {@code DockerTest}/{@code SelfTest}, actually invoking the sandbox requires a full
 * deployment (sudo, docker, the safe-docker image) that isn't available in dev/CI, so only the
 * (deterministic) availability check and the plain {@link SafeDockerRunner.Result} data holder
 * are unit-tested here.
 */
class SafeDockerRunnerTest {
	@Test
	void notAvailableWithoutInstalledScript() {
		// dev/CI environments don't have /usr/local/bin/safe-docker installed
		assertFalse(Files.isRegularFile(Path.of(DockerTest.SAFE_DOCKER_SCRIPT)), "This test assumes safe-docker is not installed on the test machine");
		assertFalse(SafeDockerRunner.isAvailable());
	}

	@Test
	void resultExposesGivenValues() {
		SafeDockerRunner.Result result = new SafeDockerRunner.Result(true, 0, "out", "err", false, false);
		assertTrue(result.isSuccessful());
		assertEquals(0, result.getExitCode());
		assertEquals("out", result.getStdOut());
		assertEquals("err", result.getStdErr());
		assertFalse(result.isTimedOut());
		assertFalse(result.isOutOfMemory());
	}

	@Test
	void resultTreatsNullOutputAsEmptyString() {
		SafeDockerRunner.Result result = new SafeDockerRunner.Result(false, 23, null, null, true, false);
		assertEquals("", result.getStdOut());
		assertEquals("", result.getStdErr());
		assertTrue(result.isTimedOut());
	}
}
