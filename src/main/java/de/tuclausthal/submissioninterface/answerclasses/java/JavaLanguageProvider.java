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

import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

import de.tuclausthal.submissioninterface.answerclasses.ArtifactType;
import de.tuclausthal.submissioninterface.answerclasses.EvaluationContext;
import de.tuclausthal.submissioninterface.answerclasses.LanguageProvider;
import de.tuclausthal.submissioninterface.testframework.executor.TestExecutorTestResult;
import de.tuclausthal.submissioninterface.testframework.tests.impl.JavaSyntaxTest;
import de.tuclausthal.submissioninterface.util.Util;

/**
 * Java {@link LanguageProvider}: student/administrative workspace, javac BUILD, JavaParser AST.
 *
 * <p><b>EXECUTION_ENV:</b> BUILD runs inside the {@code safe-docker} sandbox ({@link SafeDockerRunner})
 * whenever that script is installed; otherwise it falls back to host-side {@code javac} (e.g. local
 * dev/CI without the safe-docker deployment from {@code README.md}). RUN is not implemented yet —
 * it would use the same sandbox once it exists.</p>
 *
 * <p><b>Provider-Policy</b> (see {@code docs/antwortklassen-pipeline.puml}): AST is only produced
 * once BUILD is known to have succeeded — untrusted/uncompilable sources are not parsed for
 * AST-based checks. This is enforced here regardless of any answer-class-level prerequisite.</p>
 */
public class JavaLanguageProvider implements LanguageProvider {
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static final String STUDENT_DIR = "student";
	public static final String ADMINISTRATIVE_DIR = "administrative";
	public static final String CLASSES_DIR = "classes";
	private static final int COMPILE_TIMEOUT_SECONDS = 30;

	private final Path submissionPath;
	private final Path administrativeSourcePath;
	private Path tempDir;
	private boolean sandboxAvailable;

	/**
	 * @param submissionPath directory with the student submission files (required)
	 * @param administrativeSourcePath optional advisor/task context files; may be {@code null}
	 */
	public JavaLanguageProvider(Path submissionPath, Path administrativeSourcePath) {
		this.submissionPath = Objects.requireNonNull(submissionPath, "submissionPath");
		this.administrativeSourcePath = administrativeSourcePath;
	}

	public JavaLanguageProvider(Path submissionPath) {
		this(submissionPath, null);
	}

	@Override
	public void assembleWorkspace(EvaluationContext context) throws Exception {
		tempDir = Util.createTemporaryDirectory("answerclass-java");
		if (tempDir == null) {
			throw new IOException("Failed to create temporary workspace directory");
		}

		Path studentDir = tempDir.resolve(STUDENT_DIR);
		Files.createDirectories(studentDir);
		Util.recursiveCopy(submissionPath, studentDir);

		Path administrativeDir = tempDir.resolve(ADMINISTRATIVE_DIR);
		Files.createDirectories(administrativeDir);
		if (administrativeSourcePath != null && Files.isDirectory(administrativeSourcePath)) {
			Util.recursiveCopy(administrativeSourcePath, administrativeDir);
		}

		context.setWorkspaceRoot(tempDir);
	}

	@Override
	public void startExecutionEnv(EvaluationContext context) {
		sandboxAvailable = SafeDockerRunner.isAvailable();
		if (sandboxAvailable) {
			LOG.debug("safe-docker is available - BUILD will run sandboxed");
		} else {
			LOG.debug("safe-docker is not installed - falling back to host javac for BUILD");
		}
	}

	@Override
	public void cleanupExecutionEnv(EvaluationContext context) {
		sandboxAvailable = false;
		if (tempDir != null) {
			Util.recursiveDelete(tempDir);
			tempDir = null;
		}
		if (context != null) {
			context.setWorkspaceRoot(null);
		}
	}

	/** Package-private for tests: whether {@link #startExecutionEnv} detected a usable sandbox. */
	boolean isSandboxAvailable() {
		return sandboxAvailable;
	}

	@Override
	public boolean ensureArtifact(ArtifactType type, EvaluationContext context) {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(context, "context");
		if (context.hasArtifact(type)) {
			return true;
		}
		return switch (type) {
			case BUILD, SYNTAX -> produceBuild(context);
			case AST -> produceAst(context);
			case RUN, PROFILE -> false;
		};
	}

	Path getStudentDirectory(EvaluationContext context) {
		Path root = context.getWorkspaceRoot();
		if (root == null) {
			throw new IllegalStateException("Workspace not assembled");
		}
		return root.resolve(STUDENT_DIR);
	}

	private boolean produceBuild(EvaluationContext context) {
		try {
			Path root = context.getWorkspaceRoot();
			if (root == null) {
				return false;
			}
			Path studentDir = root.resolve(STUDENT_DIR);
			Path classesDir = root.resolve(CLASSES_DIR);
			Files.createDirectories(classesDir);

			BuildArtifact artifact = sandboxAvailable ? compileInSandbox(root, studentDir, classesDir) : compileOnHost(studentDir, classesDir);
			if (artifact == null) {
				return false;
			}
			context.putArtifact(ArtifactType.BUILD, artifact);
			context.putArtifact(ArtifactType.SYNTAX, artifact);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Host-side fallback compile (in-process {@code javac} via {@link ToolProvider}), used when
	 * {@code safe-docker} isn't installed (e.g. local dev/CI).
	 */
	private BuildArtifact compileOnHost(Path studentDir, Path classesDir) throws Exception {
		if (ToolProvider.getSystemJavaCompiler() == null) {
			return null;
		}
		TestExecutorTestResult compileResult = new TestExecutorTestResult();
		boolean ok = JavaSyntaxTest.compileJava(studentDir, null, classesDir, compileResult);
		return new BuildArtifact(ok, compileResult.getTestOutput(), classesDir);
	}

	/**
	 * Sandboxed compile: {@code javac} runs as an external process inside the safe-docker
	 * container (see {@link SafeDockerRunner}), with {@code root} (containing both
	 * {@value #STUDENT_DIR} and {@value #CLASSES_DIR}) bind-mounted read-write.
	 */
	private BuildArtifact compileInSandbox(Path root, Path studentDir, Path classesDir) throws IOException {
		List<String> javaFiles = new ArrayList<>();
		JavaSyntaxTest.getRecursivelyAllJavaFiles(studentDir, javaFiles);
		if (javaFiles.isEmpty()) {
			return new BuildArtifact(false, "no .java files found", classesDir);
		}

		List<String> command = new ArrayList<>();
		command.add("javac");
		command.add("-d");
		command.add(classesDir.toAbsolutePath().toString());
		command.addAll(javaFiles);

		SafeDockerRunner.Result result = SafeDockerRunner.run(root, COMPILE_TIMEOUT_SECONDS, command);
		String diagnostics = result.getStdErr().isBlank() ? result.getStdOut() : result.getStdErr();
		if (!result.isSuccessful() && result.isTimedOut()) {
			diagnostics = "javac timed out after " + COMPILE_TIMEOUT_SECONDS + "s";
		} else if (!result.isSuccessful() && result.isOutOfMemory()) {
			diagnostics = "javac ran out of memory";
		}
		return new BuildArtifact(result.isSuccessful(), diagnostics, classesDir);
	}

	private boolean produceAst(EvaluationContext context) {
		// Provider-Policy (docs/antwortklassen-pipeline.puml): AST is only trusted once the
		// sources are known to compile, independent of any answer-class-level prerequisite.
		if (!ensureArtifact(ArtifactType.BUILD, context)) {
			return false;
		}
		BuildArtifact build = context.<BuildArtifact>getArtifact(ArtifactType.BUILD).orElse(null);
		if (build == null || !build.isSuccessful()) {
			return false;
		}
		try {
			Path studentDir = getStudentDirectory(context);
			List<String> javaFiles = new ArrayList<>();
			JavaSyntaxTest.getRecursivelyAllJavaFiles(studentDir, javaFiles);

			JavaParser parser = new JavaParser();
			List<CompilationUnit> units = new ArrayList<>();
			for (String javaFile : javaFiles) {
				ParseResult<CompilationUnit> parseResult = parser.parse(Path.of(javaFile));
				parseResult.getResult().ifPresent(units::add);
			}
			context.putArtifact(ArtifactType.AST, new AstArtifact(units));
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
