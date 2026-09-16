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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.tools.ToolProvider;

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
 * EXECUTION_ENV is a no-op for the compile/AST MVP (no student code execution yet).
 */
public class JavaLanguageProvider implements LanguageProvider {
	public static final String STUDENT_DIR = "student";
	public static final String ADMINISTRATIVE_DIR = "administrative";
	public static final String CLASSES_DIR = "classes";

	private final Path submissionPath;
	private final Path administrativeSourcePath;
	private Path tempDir;

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
		// MVP: compile/AST only — no sandbox process yet.
	}

	@Override
	public void cleanupExecutionEnv(EvaluationContext context) {
		if (tempDir != null) {
			Util.recursiveDelete(tempDir);
			tempDir = null;
		}
		if (context != null) {
			context.setWorkspaceRoot(null);
		}
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
		if (ToolProvider.getSystemJavaCompiler() == null) {
			return false;
		}
		try {
			Path root = context.getWorkspaceRoot();
			if (root == null) {
				return false;
			}
			Path studentDir = root.resolve(STUDENT_DIR);
			Path classesDir = root.resolve(CLASSES_DIR);
			Files.createDirectories(classesDir);

			TestExecutorTestResult compileResult = new TestExecutorTestResult();
			boolean ok = JavaSyntaxTest.compileJava(studentDir, null, classesDir, compileResult);
			BuildArtifact artifact = new BuildArtifact(ok, compileResult.getTestOutput(), classesDir);
			context.putArtifact(ArtifactType.BUILD, artifact);
			context.putArtifact(ArtifactType.SYNTAX, artifact);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean produceAst(EvaluationContext context) {
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
