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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Maps {@code detectionType} strings to {@link Checker} implementations.
 */
public class CheckerRegistry {
	private final Map<String, Checker> checkers = new HashMap<>();

	public void register(String detectionType, Checker checker) {
		Objects.requireNonNull(detectionType, "detectionType");
		Objects.requireNonNull(checker, "checker");
		if (detectionType.isEmpty()) {
			throw new IllegalArgumentException("detectionType must not be empty");
		}
		checkers.put(detectionType, checker);
	}

	/**
	 * @return the checker, or {@code null} if unknown
	 */
	public Checker get(String detectionType) {
		return checkers.get(detectionType);
	}

	public boolean isRegistered(String detectionType) {
		return checkers.containsKey(detectionType);
	}
}
