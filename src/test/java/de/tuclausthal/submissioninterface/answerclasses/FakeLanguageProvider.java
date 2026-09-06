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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Test double for {@link LanguageProvider}: configurable artifact availability and call counts.
 */
final class FakeLanguageProvider implements LanguageProvider {
	private final Set<ArtifactType> availableArtifacts;
	private final Map<ArtifactType, Integer> ensureCalls = new EnumMap<>(ArtifactType.class);
	private int assembleCalls;
	private int startEnvCalls;
	private int cleanupCalls;

	FakeLanguageProvider(ArtifactType... available) {
		this.availableArtifacts = available.length == 0 ? EnumSet.noneOf(ArtifactType.class) : EnumSet.copyOf(Set.of(available));
	}

	@Override
	public void assembleWorkspace(EvaluationContext context) {
		assembleCalls++;
	}

	@Override
	public void startExecutionEnv(EvaluationContext context) {
		startEnvCalls++;
	}

	@Override
	public void cleanupExecutionEnv(EvaluationContext context) {
		cleanupCalls++;
	}

	@Override
	public boolean ensureArtifact(ArtifactType type, EvaluationContext context) {
		ensureCalls.merge(type, 1, Integer::sum);
		if (!availableArtifacts.contains(type)) {
			return false;
		}
		if (!context.hasArtifact(type)) {
			context.putArtifact(type, type.name());
		}
		return true;
	}

	int getAssembleCalls() {
		return assembleCalls;
	}

	int getStartEnvCalls() {
		return startEnvCalls;
	}

	int getCleanupCalls() {
		return cleanupCalls;
	}

	int getEnsureCalls(ArtifactType type) {
		return ensureCalls.getOrDefault(type, 0);
	}
}
