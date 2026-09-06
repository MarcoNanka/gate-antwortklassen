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
 * Who / what started an answer-class pipeline run.
 * <ul>
 * <li>{@link #STUDENT} — student requests answer classes (web button)</li>
 * <li>{@link #TUTOR} — tutor-side run / manual evaluation path</li>
 * <li>{@link #BATCH} — automatic run after deadline (cron)</li>
 * </ul>
 */
public enum PipelineTrigger {
	STUDENT,
	TUTOR,
	BATCH;
}
