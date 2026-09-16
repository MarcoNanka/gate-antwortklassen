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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.github.javaparser.ast.CompilationUnit;

/**
 * Cached JavaParser ASTs for the student sources.
 */
public final class AstArtifact {
	private final List<CompilationUnit> compilationUnits;

	public AstArtifact(List<CompilationUnit> compilationUnits) {
		this.compilationUnits = List.copyOf(Objects.requireNonNull(compilationUnits, "compilationUnits"));
	}

	public List<CompilationUnit> getCompilationUnits() {
		return Collections.unmodifiableList(compilationUnits);
	}
}
