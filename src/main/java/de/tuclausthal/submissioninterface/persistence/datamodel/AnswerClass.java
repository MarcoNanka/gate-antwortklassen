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
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Answer class configured by an advisor for a task.
 */
@Entity
@Table(name = "answerclasses")
public class AnswerClass implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	@ManyToOne(optional = false)
	@JoinColumn(name = "taskid", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Task task;
	@Column(nullable = false)
	private String title = "";
	@Column(columnDefinition = "TEXT")
	private String description;
	@Column(nullable = false, length = 50)
	private String detectionType = "";
	@Column(nullable = false)
	private int timesRunnableByStudents = 3;
	@Column(nullable = false)
	private boolean giveDetailsToStudents = true;
	/**
	 * Self-referencing many-to-many: this answer class is only evaluated once all of its
	 * prerequisites are MATCHED (see {@code AnswerClassPipeline} / {@code AnswerClassMapper}).
	 */
	@ManyToMany
	@JoinTable(name = "answerclass_prerequisites", joinColumns = @JoinColumn(name = "answerclassid"), inverseJoinColumns = @JoinColumn(name = "prerequisiteid"))
	@OrderBy("id asc")
	private Set<AnswerClass> prerequisites = new LinkedHashSet<>();
	/**
	 * Batch-queue flag, analogous to {@code Test#needsToRun}: {@code true} means the next
	 * {@code AnswerClassBatchRunner} pass should (re-)evaluate this answer class's task for all
	 * of its submissions with {@code PipelineTrigger.BATCH}. Defaults to {@code true} so newly
	 * created answer classes are picked up automatically; flipped to {@code false} once claimed
	 * (see {@code AnswerClassDAOIf#takeTaskForAnswerClassBatchTransacted}) and back to
	 * {@code true} by the advisor "erneut auswerten" action.
	 */
	@Column(nullable = false)
	private boolean needsToRun = true;

	protected AnswerClass() {}

	public AnswerClass(Task task, String title, String description, String detectionType, int timesRunnableByStudents, boolean giveDetailsToStudents) {
		this.task = task;
		this.title = title;
		this.description = description;
		this.detectionType = detectionType;
		setTimesRunnableByStudents(timesRunnableByStudents);
		this.giveDetailsToStudents = giveDetailsToStudents;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDetectionType() {
		return detectionType;
	}

	public void setDetectionType(String detectionType) {
		this.detectionType = detectionType;
	}

	public int getTimesRunnableByStudents() {
		return timesRunnableByStudents;
	}

	public void setTimesRunnableByStudents(int timesRunnableByStudents) {
		if (timesRunnableByStudents < 0) {
			timesRunnableByStudents = 0;
		}
		this.timesRunnableByStudents = timesRunnableByStudents;
	}

	public boolean isGiveDetailsToStudents() {
		return giveDetailsToStudents;
	}

	public void setGiveDetailsToStudents(boolean giveDetailsToStudents) {
		this.giveDetailsToStudents = giveDetailsToStudents;
	}

	public Set<AnswerClass> getPrerequisites() {
		return prerequisites;
	}

	public void setPrerequisites(Set<AnswerClass> prerequisites) {
		this.prerequisites = prerequisites == null ? new LinkedHashSet<>() : prerequisites;
	}

	public void addPrerequisite(AnswerClass prerequisite) {
		this.prerequisites.add(prerequisite);
	}

	public boolean isNeedsToRun() {
		return needsToRun;
	}

	public void setNeedsToRun(boolean needsToRun) {
		this.needsToRun = needsToRun;
	}

	@Override
	public String toString() {
		return MethodHandles.lookup().lookupClass().getSimpleName() + " (" + Integer.toHexString(hashCode()) + "): id:" + getId() + "; title:" + getTitle() + "; taskid:" + (getTask() == null ? "null" : getTask().getTaskid());
	}
}
