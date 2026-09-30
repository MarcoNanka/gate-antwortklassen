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
import java.util.concurrent.Future;

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;

/**
 * Whether a tutor may still run an answer-class check that is shown on the page and not stored.
 * Hidden once every automatic answer class of this submission already has a batch result.
 *
 * <p>A finished probe is put in the HTTP session together with the submission's
 * {@code lastModified} just long enough for the redirect after "Antwortklassen Probelauf
 * durchführen" to render it once; {@code ShowSubmission} removes it from the session as soon as
 * it reads it. Leaving the page (or simply reloading it) therefore shows an empty
 * "Probelauf"-column again, and a stale probe from before a new student submission never shows
 * up either, since {@link #matches(Submission)} would reject it anyway.</p>
 */
public final class TutorAnswerClassPreview {
	private TutorAnswerClassPreview() {}

	public static String sessionKey(int submissionId) {
		return "tutorAnswerClassPreview-" + submissionId;
	}

	/**
	 * Session key for the in-flight run (see {@link QueuedRun}), separate from
	 * {@link #sessionKey(int)} (the finished, one-time-viewable {@link Snapshot}) so the two
	 * states can never be confused with one another.
	 */
	public static String queuedSessionKey(int submissionId) {
		return "tutorAnswerClassPreviewQueued-" + submissionId;
	}

	/**
	 * One in-flight tutor "Probelauf" run, submitted to {@link AnswerClassExecutor} so it does
	 * not block the Tomcat request thread that handled the button click (see
	 * {@code PerformTutorAnswerClasses}). {@code submissionLastModifiedAtStart} lets the caller
	 * detect - once the run is done - whether the submission was changed in the meantime, the
	 * same way {@link Snapshot#matches} does for the finished result.
	 */
	public static final class QueuedRun implements Serializable {
		private static final long serialVersionUID = 1L;

		private final int submissionId;
		private final ZonedDateTime submissionLastModifiedAtStart;
		private final transient Future<List<PipelineResult>> resultFuture;

		public QueuedRun(int submissionId, ZonedDateTime submissionLastModifiedAtStart, Future<List<PipelineResult>> resultFuture) {
			this.submissionId = submissionId;
			this.submissionLastModifiedAtStart = submissionLastModifiedAtStart;
			this.resultFuture = resultFuture;
		}

		public int getSubmissionId() {
			return submissionId;
		}

		public ZonedDateTime getSubmissionLastModifiedAtStart() {
			return submissionLastModifiedAtStart;
		}

		public Future<List<PipelineResult>> getResultFuture() {
			return resultFuture;
		}
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
