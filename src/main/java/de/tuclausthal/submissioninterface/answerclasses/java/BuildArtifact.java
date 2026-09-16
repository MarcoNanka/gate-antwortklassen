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

import java.nio.file.Path;
import java.util.Objects;

/**
 * Cached result of compiling the student sources (javac).
 */
public final class BuildArtifact {
	private final boolean successful;
	private final String diagnostics;
	private final Path classesDirectory;

	public BuildArtifact(boolean successful, String diagnostics, Path classesDirectory) {
		this.successful = successful;
		this.diagnostics = diagnostics == null ? "" : diagnostics;
		this.classesDirectory = Objects.requireNonNull(classesDirectory, "classesDirectory");
	}

	public boolean isSuccessful() {
		return successful;
	}

	public String getDiagnostics() {
		return diagnostics;
	}

	public Path getClassesDirectory() {
		return classesDirectory;
	}
}
