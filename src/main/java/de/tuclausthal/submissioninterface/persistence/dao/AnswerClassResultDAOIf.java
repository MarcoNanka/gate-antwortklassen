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

package de.tuclausthal.submissioninterface.persistence.dao;

import java.util.List;

import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClass;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResult;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultSource;
import de.tuclausthal.submissioninterface.persistence.datamodel.AnswerClassResultStatus;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;
import de.tuclausthal.submissioninterface.persistence.datamodel.User;

/**
 * Data Access Object Interface for the AnswerClassResult-class
 */
public interface AnswerClassResultDAOIf {
	/**
	 * Creates or overwrites the result for the given answer class and submission.
	 */
	void storeResult(AnswerClass answerClass, Submission submission, AnswerClassResultStatus status, String details, AnswerClassResultSource source, User evaluatedBy);

	AnswerClassResult getResult(AnswerClass answerClass, Submission submission);

	/**
	 * @return all stored results for {@code submission}, ordered by answer class id ascending
	 *         (i.e. the same order {@code AnswerClassDAOIf#getAnswerClassesForTask} returns its
	 *         answer classes in) — used by the student/tutor result views to avoid one query
	 *         per answer class.
	 */
	List<AnswerClassResult> getResultsForSubmission(Submission submission);
}
