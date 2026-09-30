/*
 * Copyright 2009-2011, 2020-2021, 2025 Sven Strickroth <email@cs-ware.de>
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

package de.tuclausthal.submissioninterface.authfilter;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Future;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import de.tuclausthal.submissioninterface.answerclasses.PipelineResult;
import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.datamodel.User;
import de.tuclausthal.submissioninterface.servlets.RequestAdapter;
import de.tuclausthal.submissioninterface.testframework.executor.TestExecutorTestResult;

/**
 * Adapter for HTTP-session to have a better interface for our stored variables
 * @author Sven Strickroth
 */
public class SessionAdapter {
	private HttpSession session = null;
	private User user = null;

	public SessionAdapter(HttpServletRequest request) {
		if (request.isRequestedSessionIdFromURL()) {
			throw new RuntimeException("Got session ID from URL! Session fixation attacks possible!");
		}
		session = request.getSession(true);
		if (session.getAttribute("userID") != null) {
			user = DAOFactory.UserDAOIf(RequestAdapter.getSession(request)).getUser((Integer) session.getAttribute("userID"));
		}
	}

	public boolean isIPCorrect(String clientRemoteAddr) {
		return clientRemoteAddr.equals(session.getAttribute("ip"));
	}

	/**
	 * Binds the user to the session
	 * @param user
	 * @param remoteIP 
	 */
	public void setUser(User user, String remoteIP) {
		this.user = user;
		if (user != null) {
			session.setAttribute("userID", user.getUid());
			session.setAttribute("ip", remoteIP);
		} else {
			session.invalidate();
		}
	}

	/**
	 * Reads the user from the session
	 * @return the user or null if no user was stored to the session
	 */
	public User getUser() {
		return user;
	}

	public QueuedTest getQueuedTest() {
		return (QueuedTest) session.getAttribute("queuedTest");
	}

	public void setQueuedTest(QueuedTest queuedTest) {
		session.setAttribute("queuedTest", queuedTest);
	}

	public static class QueuedTest implements Serializable {
		private static final long serialVersionUID = 1L;

		public int testId;
		public ZonedDateTime submissionLastChanged;
		public transient Future<TestExecutorTestResult> testResult;

		public QueuedTest(int testId, ZonedDateTime submissionLastChanged, Future<TestExecutorTestResult> testResult) {
			this.testId = testId;
			this.submissionLastChanged = submissionLastChanged;
			this.testResult = testResult;
		}
	}

	/**
	 * One in-flight student answer-class preview run (see {@code PerformStudentAnswerClasses}
	 * and {@code answerclasses.AnswerClassExecutor}). Single slot per session, same "only one at
	 * a time" semantics as {@link QueuedTest}.
	 */
	public QueuedAnswerClassRun getQueuedAnswerClassRun() {
		return (QueuedAnswerClassRun) session.getAttribute("queuedAnswerClassRun");
	}

	public void setQueuedAnswerClassRun(QueuedAnswerClassRun queuedAnswerClassRun) {
		session.setAttribute("queuedAnswerClassRun", queuedAnswerClassRun);
	}

	public static class QueuedAnswerClassRun implements Serializable {
		private static final long serialVersionUID = 1L;

		public int taskId;
		public ZonedDateTime submissionLastChanged;
		public transient Future<List<PipelineResult>> resultFuture;

		public QueuedAnswerClassRun(int taskId, ZonedDateTime submissionLastChanged, Future<List<PipelineResult>> resultFuture) {
			this.taskId = taskId;
			this.submissionLastChanged = submissionLastChanged;
			this.resultFuture = resultFuture;
		}
	}
}
