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

package de.tuclausthal.submissioninterface.persistence.datamodel;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Persisted evaluation result for one answer class and one submission (overwrite on re-run).
 */
@Entity
@Table(name = "answerclassresults", uniqueConstraints = { @UniqueConstraint(columnNames = { "answerclassid", "submissionid" }) })
public class AnswerClassResult implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	@ManyToOne(optional = false)
	@JoinColumn(name = "answerclassid", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private AnswerClass answerClass;
	@ManyToOne(optional = false)
	@JoinColumn(name = "submissionid", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Submission submission;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AnswerClassResultStatus status;
	@Column(nullable = false, length = 16777215)
	private String details = "";
	@Column(nullable = false)
	private ZonedDateTime checkedAt = ZonedDateTime.now();
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AnswerClassResultSource source;
	@ManyToOne(optional = true)
	@JoinColumn(name = "evaluatedBy")
	private User evaluatedBy;

	// for Hibernate / DAO
	public AnswerClassResult() {}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public AnswerClass getAnswerClass() {
		return answerClass;
	}

	public void setAnswerClass(AnswerClass answerClass) {
		this.answerClass = answerClass;
	}

	public Submission getSubmission() {
		return submission;
	}

	public void setSubmission(Submission submission) {
		this.submission = submission;
	}

	public AnswerClassResultStatus getStatus() {
		return status;
	}

	public void setStatus(AnswerClassResultStatus status) {
		this.status = status;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public ZonedDateTime getCheckedAt() {
		return checkedAt;
	}

	public void setCheckedAt(ZonedDateTime checkedAt) {
		this.checkedAt = checkedAt;
	}

	public AnswerClassResultSource getSource() {
		return source;
	}

	public void setSource(AnswerClassResultSource source) {
		this.source = source;
	}

	public User getEvaluatedBy() {
		return evaluatedBy;
	}

	public void setEvaluatedBy(User evaluatedBy) {
		this.evaluatedBy = evaluatedBy;
	}

	@Override
	public String toString() {
		return MethodHandles.lookup().lookupClass().getSimpleName() + " (" + Integer.toHexString(hashCode()) + "): id:" + getId() + "; status:" + getStatus() + "; answerclassid:" + (getAnswerClass() == null ? "null" : getAnswerClass().getId()) + "; submissionid:" + (getSubmission() == null ? "null" : getSubmission().getSubmissionid());
	}
}
