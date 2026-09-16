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

import java.util.EnumSet;
import java.util.Set;

import com.github.javaparser.ast.stmt.WhileStmt;

import de.tuclausthal.submissioninterface.answerclasses.ArtifactType;
import de.tuclausthal.submissioninterface.answerclasses.Checker;
import de.tuclausthal.submissioninterface.answerclasses.CheckerOutcome;
import de.tuclausthal.submissioninterface.answerclasses.EvaluationContext;

/**
 * MATCHED if any parsed compilation unit contains a {@code while} statement.
 */
public class WhileLoopChecker implements Checker {
	@Override
	public Set<ArtifactType> requiredArtifacts() {
		return EnumSet.of(ArtifactType.AST);
	}

	@Override
	public CheckerOutcome check(EvaluationContext context) {
		AstArtifact ast = context.<AstArtifact>getArtifact(ArtifactType.AST).orElseThrow(() -> new IllegalStateException("AST artifact missing"));
		boolean found = ast.getCompilationUnits().stream().anyMatch(unit -> !unit.findAll(WhileStmt.class).isEmpty());
		if (found) {
			return CheckerOutcome.matched("while loop present");
		}
		return CheckerOutcome.notMatched("no while loop found");
	}
}
