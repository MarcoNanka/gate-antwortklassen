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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tuclausthal.submissioninterface.testframework.tests.impl.DockerTest;
import de.tuclausthal.submissioninterface.testframework.tests.impl.ProcessOutputGrabber;
import de.tuclausthal.submissioninterface.util.Util;

/**
 * Runs a command with the safe-docker script, the same way DockerTest does.
 * If the script is missing, callers fall back to a run on the host.
 */
public final class SafeDockerRunner {
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private SafeDockerRunner() {}

	/**
	 * @return {@code true} if the safe-docker script is installed on this host (see
	 *         {@code README.md}, section "Docker-based tests").
	 */
	public static boolean isAvailable() {
		return Files.isRegularFile(Path.of(DockerTest.SAFE_DOCKER_SCRIPT));
	}

	/**
	 * Runs {@code command} inside the sandbox. {@code cwd} is bind-mounted read-write as the
	 * container's working directory, at the identical absolute path inside and outside the
	 * container (see {@code safe-docker/safe-docker}: {@code --volume=$cwd:$cwd --workdir=$cwd}).
	 *
	 * @param cwd read-write working directory of the sandboxed process
	 * @param timeoutSeconds enforced by the safe-docker script itself (exit code 23 on timeout)
	 * @param command the command and its arguments to run inside the container
	 * @return the result of the sandboxed run; never {@code null}
	 * @throws IOException if the sandbox process could not even be started
	 */
	public static Result run(Path cwd, int timeoutSeconds, List<String> command) throws IOException {
		Objects.requireNonNull(cwd, "cwd");
		Objects.requireNonNull(command, "command");

		List<String> params = new ArrayList<>();
		params.add("sudo");
		params.add(DockerTest.SAFE_DOCKER_SCRIPT);
		params.add("--timeout=" + timeoutSeconds);
		params.add("--");
		for (String part : command) {
			params.add(Util.escapeCommandlineArguments(part));
		}

		ProcessBuilder pb = new ProcessBuilder(params);
		pb.directory(cwd.toFile());
		// only forward explicitly specified environment variables to the sandboxed process
		pb.environment().keySet().removeIf(key -> !("PATH".equalsIgnoreCase(key) || "USER".equalsIgnoreCase(key) || "LANG".equalsIgnoreCase(key)));
		LOG.debug("Executing sandboxed process: {} in {}", params, cwd);
		Process process = pb.start();

		ProcessOutputGrabber outputGrabber = new ProcessOutputGrabber(process);
		try {
			int exitValue = process.waitFor();
			outputGrabber.waitFor();
			// magic exit codes of the safe-docker script
			boolean timedOut = exitValue == 23;
			boolean outOfMemory = exitValue == 24;
			return new Result(exitValue == 0, exitValue, outputGrabber.getStdOutBuffer().toString(), outputGrabber.getStdErrBuffer().toString(), timedOut, outOfMemory);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			process.destroyForcibly();
			return new Result(false, -1, outputGrabber.getStdOutBuffer().toString(), outputGrabber.getStdErrBuffer().toString(), false, false);
		}
	}

	/**
	 * Result of one sandboxed process invocation.
	 */
	public static final class Result {
		private final boolean successful;
		private final int exitCode;
		private final String stdOut;
		private final String stdErr;
		private final boolean timedOut;
		private final boolean outOfMemory;

		Result(boolean successful, int exitCode, String stdOut, String stdErr, boolean timedOut, boolean outOfMemory) {
			this.successful = successful;
			this.exitCode = exitCode;
			this.stdOut = stdOut == null ? "" : stdOut;
			this.stdErr = stdErr == null ? "" : stdErr;
			this.timedOut = timedOut;
			this.outOfMemory = outOfMemory;
		}

		/** @return {@code true} if the sandboxed process exited with code 0 */
		public boolean isSuccessful() {
			return successful;
		}

		public int getExitCode() {
			return exitCode;
		}

		public String getStdOut() {
			return stdOut;
		}

		public String getStdErr() {
			return stdErr;
		}

		public boolean isTimedOut() {
			return timedOut;
		}

		public boolean isOutOfMemory() {
			return outOfMemory;
		}
	}
}
