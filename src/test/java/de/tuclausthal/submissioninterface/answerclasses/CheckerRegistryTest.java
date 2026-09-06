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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class CheckerRegistryTest {
	@Test
	void registerAndResolve() {
		CheckerRegistry registry = new CheckerRegistry();
		Checker checker = new Checker() {
			@Override
			public Set<ArtifactType> requiredArtifacts() {
				return Set.of();
			}

			@Override
			public CheckerOutcome check(EvaluationContext context) {
				return CheckerOutcome.matched("ok");
			}
		};
		registry.register("WHILE_LOOP", checker);

		assertTrue(registry.isRegistered("WHILE_LOOP"));
		assertEquals(checker, registry.get("WHILE_LOOP"));
		assertNull(registry.get("UNKNOWN"));
	}
}
