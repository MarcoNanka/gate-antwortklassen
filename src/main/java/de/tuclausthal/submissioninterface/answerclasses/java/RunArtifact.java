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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tuclausthal.submissioninterface.testframework.executor.TestExecutorTestResult;
import de.tuclausthal.submissioninterface.testframework.tests.impl.JavaFunctionTest;
import de.tuclausthal.submissioninterface.testframework.tests.impl.JavaSyntaxTest;
import de.tuclausthal.submissioninterface.testframework.tests.impl.ProcessOutputGrabber;
import de.tuclausthal.submissioninterface.testframework.tests.impl.TimeoutThread;
import de.tuclausthal.submissioninterface.util.Util;

/**
 * Runs advisor test code against the compiled student classes.
 * Each function test gets its own run.
 * Tester.java is compiled on the host. The program runs in safe-docker when that
 * is installed, otherwise with NoExitSecurityManager.
 */
public final class RunArtifact {
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final Path workspaceRoot;
	private final Path classesDirectory;
	private final boolean sandboxAvailable;
	private final Path dataPath;

	RunArtifact(Path workspaceRoot, Path classesDirectory, boolean sandboxAvailable, Path dataPath) {
		this.workspaceRoot = Objects.requireNonNull(workspaceRoot, "workspaceRoot");
		this.classesDirectory = Objects.requireNonNull(classesDirectory, "classesDirectory");
		this.sandboxAvailable = sandboxAvailable;
		this.dataPath = dataPath;
	}

	/**
	 * @return {@code true} if a host-side run of untrusted code is possible, i.e.
	 *         {@code NoExitSecurityManager.jar} is deployed at {@code dataPath} (see
	 *         {@code README.md}). Irrelevant once {@code safe-docker} is available.
	 */
	static boolean isHostSandboxAvailable(Path dataPath) {
		return dataPath != null && Files.isRegularFile(dataPath.resolve(JavaFunctionTest.SECURITYMANAGER_JAR));
	}

	/**
	 * Compiles {@code testCode} (wrapped into a generated {@code Tester.main()}) against the
	 * already-compiled student classes and runs it, sandboxed if possible.
	 *
	 * @param testCode advisor-authored Java statements, e.g. {@code System.out.println(Hello.greet());}
	 * @param timeoutSeconds enforced by the sandbox (safe-docker) or a {@link TimeoutThread} (host)
	 */
	public Outcome execute(String testCode, int timeoutSeconds) throws Exception {
		Objects.requireNonNull(testCode, "testCode");
		Path callDir = Files.createTempDirectory(workspaceRoot, "run-");
		try {
			Path srcDir = callDir.resolve("src");
			Files.createDirectories(srcDir);
			Path testerClassesDir = callDir.resolve("classes");
			Files.createDirectories(testerClassesDir);

			writeTesterJava(srcDir, testCode);

			TestExecutorTestResult compileResult = new TestExecutorTestResult();
			boolean compiled = JavaSyntaxTest.compileJava(srcDir, List.of(classesDirectory), testerClassesDir, compileResult);
			if (!compiled) {
				return Outcome.compileError(compileResult.getTestOutput());
			}

			if (sandboxAvailable) {
				return runInSandbox(testerClassesDir, timeoutSeconds);
			}
			return runOnHost(testerClassesDir, timeoutSeconds);
		} finally {
			Util.recursiveDelete(callDir);
		}
	}

	private static void writeTesterJava(Path srcDir, String testCode) throws IOException {
		String source = "public class Tester {\n\tpublic static void main(String[] args) throws Exception {\n\t\t" + testCode + "\n\t}\n}\n";
		try (Writer writer = Files.newBufferedWriter(srcDir.resolve("Tester.java"))) {
			writer.write(source);
		}
	}

	/**
	 * Runs Tester inside safe-docker. The container isolates the student code.
	 */
	private Outcome runInSandbox(Path testerClassesDir, int timeoutSeconds) throws IOException {
		List<String> command = new ArrayList<>();
		command.add("java");
		command.add("-Djava.awt.headless=true");
		command.add("-Xmx128m");
		command.add("-cp");
		command.add(classesDirectory.toAbsolutePath() + File.pathSeparator + testerClassesDir.toAbsolutePath());
		command.add("Tester");

		SafeDockerRunner.Result result = SafeDockerRunner.run(workspaceRoot, timeoutSeconds, command);
		return Outcome.ran(result.isSuccessful(), result.getStdOut(), result.getStdErr(), result.isTimedOut());
	}

	/**
	 * Host fallback when safe-docker is not installed.
	 * Uses NoExitSecurityManager, same idea as JavaFunctionTest.
	 */
	private Outcome runOnHost(Path testerClassesDir, int timeoutSeconds) throws Exception {
		if (!isHostSandboxAvailable(dataPath)) {
			LOG.warn("Neither safe-docker nor {} in dataPath are available; cannot run FUNCTION_TEST code safely.", JavaFunctionTest.SECURITYMANAGER_JAR);
			return Outcome.notAvailable();
		}
		Path securityManagerJar = dataPath.resolve(JavaFunctionTest.SECURITYMANAGER_JAR);

		Path policyFile = Files.createTempFile("answerclass-run", ".policy");
		try {
			try (BufferedWriter policyWriter = Files.newBufferedWriter(policyFile)) {
				// mirrors JavaAdvancedIOTest#populateJavaPolicyFile: the generated Tester class needs
				// permission to redirect stdio and call the (package-private/synthetic) student code
				policyWriter.write("grant codeBase \"file:" + toPolicyPath(testerClassesDir) + "\" {\n");
				policyWriter.write("\tpermission java.lang.RuntimePermission \"setIO\";\n");
				policyWriter.write("\tpermission java.lang.reflect.ReflectPermission \"suppressAccessChecks\";\n");
				policyWriter.write("};\n");
				// mirrors JavaFunctionTest#runJava's base grant
				policyWriter.write("grant {\n");
				policyWriter.write("\tpermission java.util.PropertyPermission \"*\", \"read\";\n");
				policyWriter.write("\tpermission java.io.FilePermission \"-\", \"read, write, delete\";\n");
				policyWriter.write("\tpermission java.lang.RuntimePermission \"accessDeclaredMembers\";\n");
				policyWriter.write("};\n");
			}

			List<String> params = new ArrayList<>();
			params.add("java");
			params.add("-Djava.awt.headless=true");
			params.add("-Xmx128m");
			params.add("-Xbootclasspath/a:" + securityManagerJar.toAbsolutePath());
			params.add("-Djava.security.manager=secmgr.NoExitSecurityManager");
			params.add("-Djava.security.policy=" + policyFile.toAbsolutePath());
			params.add("-cp");
			params.add(classesDirectory.toAbsolutePath() + File.pathSeparator + testerClassesDir.toAbsolutePath());
			params.add("Tester");

			ProcessBuilder pb = new ProcessBuilder(params);
			pb.directory(workspaceRoot.toFile());
			// only forward explicitly specified environment variables to the test process
			pb.environment().keySet().removeIf(key -> !("PATH".equalsIgnoreCase(key) || "USER".equalsIgnoreCase(key) || "JAVA_HOME".equalsIgnoreCase(key) || "LANG".equalsIgnoreCase(key)));
			LOG.debug("Executing host-side sandboxed process: {} in {}", params, workspaceRoot);
			Process process = pb.start();

			ProcessOutputGrabber outputGrabber = new ProcessOutputGrabber(process);
			TimeoutThread timeoutThread = new TimeoutThread(process, timeoutSeconds);
			timeoutThread.start();
			int exitValue;
			try {
				exitValue = process.waitFor();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				process.destroyForcibly();
				exitValue = -1;
			} finally {
				timeoutThread.interrupt();
			}
			boolean timedOut = timeoutThread.wasAborted();
			outputGrabber.waitFor();

			return Outcome.ran(exitValue == 0, outputGrabber.getStdOutBuffer().toString(), outputGrabber.getStdErrBuffer().toString(), timedOut);
		} finally {
			Files.deleteIfExists(policyFile);
		}
	}

	/** Windows-path escaping for policy file codeBase URLs, like {@code AbstractTest#mkPath}. */
	private static String toPolicyPath(Path path) {
		String absolute = path.toAbsolutePath().toString();
		if ("\\".equals(System.getProperty("file.separator"))) {
			return absolute.replace("\\", "\\\\");
		}
		return absolute;
	}

	/**
	 * Result of one {@link #execute} call.
	 */
	public static final class Outcome {
		private final boolean compileError;
		private final boolean available;
		private final String diagnostics;
		private final boolean exitedCleanly;
		private final String stdOut;
		private final String stdErr;
		private final boolean timedOut;

		private Outcome(boolean compileError, boolean available, String diagnostics, boolean exitedCleanly, String stdOut, String stdErr, boolean timedOut) {
			this.compileError = compileError;
			this.available = available;
			this.diagnostics = diagnostics == null ? "" : diagnostics;
			this.exitedCleanly = exitedCleanly;
			this.stdOut = stdOut == null ? "" : stdOut;
			this.stdErr = stdErr == null ? "" : stdErr;
			this.timedOut = timedOut;
		}

		static Outcome compileError(String diagnostics) {
			return new Outcome(true, true, diagnostics, false, "", "", false);
		}

		static Outcome notAvailable() {
			return new Outcome(false, false, "kein Sandbox für die Ausführung verfügbar (weder safe-docker noch " + JavaFunctionTest.SECURITYMANAGER_JAR + " im dataPath)", false, "", "", false);
		}

		static Outcome ran(boolean exitedCleanly, String stdOut, String stdErr, boolean timedOut) {
			return new Outcome(false, true, "", exitedCleanly, stdOut, stdErr, timedOut);
		}

		/** @return {@code true} if the generated {@code Tester.java} failed to compile */
		public boolean hasCompileError() {
			return compileError;
		}

		/** @return {@code true} if no sandbox (docker or host) was available to run the code at all */
		public boolean isAvailable() {
			return available;
		}

		/** Compile diagnostics, or the "no sandbox" message; empty once the program actually ran. */
		public String getDiagnostics() {
			return diagnostics;
		}

		public boolean isExitedCleanly() {
			return exitedCleanly;
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
	}
}
