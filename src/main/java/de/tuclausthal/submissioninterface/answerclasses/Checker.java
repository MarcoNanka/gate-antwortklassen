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

package de.tuclausthal.submissioninterface.answerclasses;

import java.util.Set;

/**
 * Automatic detector for one {@code detectionType} (e.g. WHILE_LOOP).
 */
public interface Checker {
	/**
	 * Artifacts this checker needs from the {@link LanguageProvider}.
	 */
	Set<ArtifactType> requiredArtifacts();

	/**
	 * Run the check; artifacts are already available in the context.
	 *
	 * @param answerClass pipeline view of the answer class being checked - most checkers ignore
	 *        this, but e.g. {@code FunctionTestChecker} needs the advisor-configured test code
	 *        and expected output that live on it
	 */
	CheckerOutcome check(EvaluationContext context, AnswerClassToEvaluate answerClass);
}
