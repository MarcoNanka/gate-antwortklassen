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

import com.github.javaparser.ast.stmt.ForStmt;

import de.tuclausthal.submissioninterface.answerclasses.AnswerClassToEvaluate;
import de.tuclausthal.submissioninterface.answerclasses.ArtifactType;
import de.tuclausthal.submissioninterface.answerclasses.Checker;
import de.tuclausthal.submissioninterface.answerclasses.CheckerOutcome;
import de.tuclausthal.submissioninterface.answerclasses.EvaluationContext;

/**
 * MATCHED if the number of classic for-loops is inside [forLoopMin, forLoopMax].
 * For-each loops are not counted.
 */
public class ForLoopChecker implements Checker {
	@Override
	public Set<ArtifactType> requiredArtifacts() {
		return EnumSet.of(ArtifactType.AST);
	}

	@Override
	public CheckerOutcome check(EvaluationContext context, AnswerClassToEvaluate answerClass) {
		AstArtifact ast = context.<AstArtifact>getArtifact(ArtifactType.AST).orElseThrow(() -> new IllegalStateException("AST artifact missing"));
		int count = ast.getCompilationUnits().stream().mapToInt(unit -> unit.findAll(ForStmt.class).size()).sum();

		int min = answerClass.getForLoopMin();
		Integer max = answerClass.getForLoopMax();
		String expectedRange = max == null ? (">= " + min) : (min == max ? ("genau " + min) : (min + " bis " + max));
		String details = count + " for-Schleife(n) gefunden (erwartet: " + expectedRange + ")";

		boolean ok = count >= min && (max == null || count <= max);
		return ok ? CheckerOutcome.matched(details) : CheckerOutcome.notMatched(details);
	}
}
