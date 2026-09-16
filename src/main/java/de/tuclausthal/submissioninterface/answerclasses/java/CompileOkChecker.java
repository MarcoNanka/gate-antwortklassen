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

import de.tuclausthal.submissioninterface.answerclasses.ArtifactType;
import de.tuclausthal.submissioninterface.answerclasses.Checker;
import de.tuclausthal.submissioninterface.answerclasses.CheckerOutcome;
import de.tuclausthal.submissioninterface.answerclasses.EvaluationContext;

/**
 * MATCHED if the student submission compiles with javac.
 */
public class CompileOkChecker implements Checker {
	@Override
	public Set<ArtifactType> requiredArtifacts() {
		return EnumSet.of(ArtifactType.BUILD);
	}

	@Override
	public CheckerOutcome check(EvaluationContext context) {
		BuildArtifact build = context.<BuildArtifact>getArtifact(ArtifactType.BUILD).orElseThrow(() -> new IllegalStateException("BUILD artifact missing"));
		if (build.isSuccessful()) {
			return CheckerOutcome.matched("compiled successfully");
		}
		return CheckerOutcome.notMatched(build.getDiagnostics());
	}
}
