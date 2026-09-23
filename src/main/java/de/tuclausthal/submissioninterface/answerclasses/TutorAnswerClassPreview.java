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

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;

/**
 * Whether a tutor may still run an answer-class check that is shown on the page and not stored.
 * Hidden once every automatic answer class of this submission already has a batch result.
 *
 * <p>A finished probe is kept in the HTTP session only together with the submission's
 * {@code lastModified}. A later page view shows it while the files are unchanged and drops it
 * as soon as the student submission was modified.</p>
 */
public final class TutorAnswerClassPreview {
	private TutorAnswerClassPreview() {}

	public static String sessionKey(int submissionId) {
		return "tutorAnswerClassPreview-" + submissionId;
	}

	/**
	 * Probe results for one submission version. Not written to the database.
	 */
	public static final class Snapshot implements Serializable {
		private static final long serialVersionUID = 1L;

		private final int submissionId;
		private final ZonedDateTime submissionLastModified;
		private final Map<Integer, PipelineResult> results;

		public Snapshot(int submissionId, ZonedDateTime submissionLastModified, Map<Integer, PipelineResult> results) {
			this.submissionId = submissionId;
			this.submissionLastModified = submissionLastModified;
			this.results = Map.copyOf(results);
		}

		public Map<Integer, PipelineResult> getResults() {
			return results;
		}

		/**
		 * @return {@code true} when this probe was computed for the submission as it is now
		 */
		public boolean matches(Submission submission) {
			return submission != null && matches(submission.getSubmissionid(), submission.getLastModified());
		}

		boolean matches(int submissionId, ZonedDateTime lastModified) {
			return this.submissionId == submissionId && Objects.equals(submissionLastModified, lastModified);
		}
	}

	public static boolean isAvailable(List<AnswerClass> answerClasses, Map<Integer, AnswerClassResult> resultsByAnswerClassId) {
		for (AnswerClass answerClass : answerClasses) {
			if (AnswerClassToEvaluate.MANUAL_DETECTION_TYPE.equals(answerClass.getDetectionType())) {
				continue;
			}
			AnswerClassResult stored = resultsByAnswerClassId.get(answerClass.getId());
			if (stored == null || stored.getSource() != AnswerClassResultSource.AUTOMATIC) {
				return true;
			}
		}
		return false;
	}
}
