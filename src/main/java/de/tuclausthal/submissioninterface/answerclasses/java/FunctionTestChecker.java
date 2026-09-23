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

import de.tuclausthal.submissioninterface.answerclasses.AnswerClassToEvaluate;
import de.tuclausthal.submissioninterface.answerclasses.ArtifactType;
import de.tuclausthal.submissioninterface.answerclasses.Checker;
import de.tuclausthal.submissioninterface.answerclasses.CheckerOutcome;
import de.tuclausthal.submissioninterface.answerclasses.EvaluationContext;

/**
 * MATCHED if running the advisor's test code ({@link AnswerClassToEvaluate#getFunctionTestCode()})
 * against the student submission produces exactly {@link AnswerClassToEvaluate#getFunctionTestExpectedOutput()}
 * on stdout (compared trimmed, like the classic {@code JavaAdvancedIOTest}). One test case per
 * answer class; see {@link RunArtifact} for how/where the code actually executes.
 */
public class FunctionTestChecker implements Checker {
	private static final int RUN_TIMEOUT_SECONDS = 15;

	@Override
	public Set<ArtifactType> requiredArtifacts() {
		return EnumSet.of(ArtifactType.RUN);
	}

	@Override
	public CheckerOutcome check(EvaluationContext context, AnswerClassToEvaluate answerClass) {
		String testCode = answerClass.getFunctionTestCode();
		if (testCode == null || testCode.isBlank()) {
			return CheckerOutcome.notMatched("kein Testcode für diese Antwortklasse konfiguriert");
		}

		RunArtifact run = context.<RunArtifact>getArtifact(ArtifactType.RUN).orElseThrow(() -> new IllegalStateException("RUN artifact missing"));
		RunArtifact.Outcome outcome;
		try {
			outcome = run.execute(testCode, RUN_TIMEOUT_SECONDS);
		} catch (Exception e) {
			return CheckerOutcome.notMatched("Ausführung fehlgeschlagen: " + e.getMessage());
		}

		if (!outcome.isAvailable()) {
			return CheckerOutcome.notMatched(outcome.getDiagnostics());
		}
		if (outcome.hasCompileError()) {
			return CheckerOutcome.notMatched("Testcode kompiliert nicht:\n" + outcome.getDiagnostics());
		}
		if (outcome.isTimedOut()) {
			return CheckerOutcome.notMatched("Zeitüberschreitung bei der Ausführung (> " + RUN_TIMEOUT_SECONDS + "s)");
		}

		String got = outcome.getStdOut().trim();
		if (!outcome.isExitedCleanly()) {
			return CheckerOutcome.notMatched("Programm wurde nicht sauber beendet.\nstderr:\n" + outcome.getStdErr() + "\nstdout:\n" + got);
		}

		String expected = answerClass.getFunctionTestExpectedOutput() == null ? "" : answerClass.getFunctionTestExpectedOutput().trim();
		String input = "Eingabe:\n" + testCode.trim();
		if (got.equals(expected)) {
			return CheckerOutcome.matched(input + "\n\nAusgabe:\n" + got);
		}
		return CheckerOutcome.notMatched(input + "\n\nerwartet:\n" + expected + "\n\nerhalten:\n" + got);
	}
}
