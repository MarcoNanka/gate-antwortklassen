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

/**
 * Language plugin: workspace layout, sandbox lifecycle, and lazy artifacts.
 * Chosen once per pipeline run (e.g. from the task language).
 */
public interface LanguageProvider {
	/**
	 * Build student/ + administrative/ workspace (always, once per run).
	 */
	void assembleWorkspace(EvaluationContext context) throws Exception;

	/**
	 * Start EXECUTION_ENV / sandbox (once per run, before the AC loop).
	 */
	void startExecutionEnv(EvaluationContext context) throws Exception;

	/**
	 * Tear down EXECUTION_ENV after the AC loop (also on failure).
	 */
	void cleanupExecutionEnv(EvaluationContext context);

	/**
	 * Ensure {@code type} (and its provider-internal dependencies) is available in the context cache.
	 * Must be idempotent: producing the same artifact at most once per run.
	 *
	 * @return {@code false} if the artifact cannot be produced
	 */
	boolean ensureArtifact(ArtifactType type, EvaluationContext context);
}
