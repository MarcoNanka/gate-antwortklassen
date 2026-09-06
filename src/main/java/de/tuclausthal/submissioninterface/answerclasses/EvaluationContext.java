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

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Per-run context: trigger, optional workspace root, and lazy artifact cache.
 */
public class EvaluationContext {
	private final PipelineTrigger trigger;
	private final Map<ArtifactType, Object> artifacts = new EnumMap<>(ArtifactType.class);
	private Path workspaceRoot;

	public EvaluationContext(PipelineTrigger trigger) {
		this.trigger = Objects.requireNonNull(trigger, "trigger");
	}

	public PipelineTrigger getTrigger() {
		return trigger;
	}

	public Path getWorkspaceRoot() {
		return workspaceRoot;
	}

	public void setWorkspaceRoot(Path workspaceRoot) {
		this.workspaceRoot = workspaceRoot;
	}

	public boolean hasArtifact(ArtifactType type) {
		return artifacts.containsKey(type);
	}

	public void putArtifact(ArtifactType type, Object value) {
		artifacts.put(Objects.requireNonNull(type, "type"), value);
	}

	@SuppressWarnings("unchecked")
	public <T> Optional<T> getArtifact(ArtifactType type) {
		Object value = artifacts.get(type);
		if (value == null) {
			return Optional.empty();
		}
		return Optional.of((T) value);
	}
}
