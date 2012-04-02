/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.evolveum.midpoint.util.aspect.MidpointAspect;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
public class LogfileTestTailer {
	
	private static final String LOG_FILENAME = "target/test.log";
	private static final String MARKER = "_M_A_R_K_E_R_";
	
	public static final String LEVEL_ERROR = "ERROR";
	public static final String LEVEL_WARN = "WARN";
	public static final String LEVEL_INFO = "INFO";
	public static final String LEVEL_DEBUG = "DEBUG";
	public static final String LEVEL_TRACE = "TRACE";
	
	private static final Pattern pattern = Pattern.compile(".*\\[[^]]*\\]\\s+(\\w+)\\s+\\S+\\s+"+MARKER+"\\s+(\\w+).*");
	
	final static Trace LOGGER = TraceManager.getTrace(LogfileTestTailer.class);
	
	private FileReader fileReader;
	private BufferedReader reader;
	private boolean seenMarker;
	private Set<String> logMessages;
	
	public LogfileTestTailer() throws IOException {
		reset();
		File file = new File(LOG_FILENAME);
		fileReader = new FileReader(file);
		reader = new BufferedReader(fileReader);
		reader.skip(file.length());
	}
	
	public void close() throws IOException {
		reader.close();
		fileReader.close();
	}
	
	public void reset() {
		seenMarker = false;
		logMessages = new HashSet<String>();
	}

	public boolean isSeenMarker() {
		return seenMarker;
	}

	public void setSeenMarker(boolean seenMarker) {
		this.seenMarker = seenMarker;
	}

	public void tail() throws IOException {
		while (true) {
		    String line = reader.readLine();
		    if (line == null) {
		    	break;
		    }
		    processLogLine(line);
		}
	}

	private void processLogLine(String line) {
		Matcher matcher = pattern.matcher(line);
		while (matcher.find()) {
			seenMarker = true;
			String level = matcher.group(1);
			String subsystemName = matcher.group(2);
			recordLogMessage(level,subsystemName);
		}
	}
	
	private void recordLogMessage(String level, String subsystemName) {
		String key = constructKey(level, subsystemName);
		logMessages.add(key);
	}

	private String constructKey(String level, String subsystemName) {
		return level+":"+subsystemName;
	}
	
	public void assertLogged(String level, String subsystemName) {
		assert logMessages.contains(constructKey(level, subsystemName)) : level + " in " + subsystemName + " was not logged";
	}
	
	public void assertNotLogged(String level, String subsystemName) {
		assert !logMessages.contains(constructKey(level, subsystemName)) : level + " in " + subsystemName + " was logged (while not expecting it)";
	}

	/**
	 * Log all levels in all subsystems.
	 */
	public void log() {
		for (String subsystemName: MidpointAspect.SUBSYSTEMS) {
			logAllLevels(LOGGER, subsystemName);
		}
		logAllLevels(LOGGER, null);
	}
	
	private void logAllLevels(Trace logger, String subsystemName) {
		String message = MARKER+" "+subsystemName;
		String previousSubsystem = MidpointAspect.swapSubsystemMark(subsystemName);
		logger.trace(message);
		logger.debug(message);
		logger.info(message);
		logger.warn(message);
		logger.error(message);
		MidpointAspect.swapSubsystemMark(previousSubsystem);
	}
	
	public void logAndTail() throws IOException {
		log();
		// Some pause here?
		tail();
	}

}
