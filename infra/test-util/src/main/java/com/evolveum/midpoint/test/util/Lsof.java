/**
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.test.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * WARNING! Only works on Linux.
 *
 * @author semancik
 */
public class Lsof implements DebugDumpable {

	private static final Trace LOGGER = TraceManager.getTrace(Lsof.class);

	private int pid;
	private int toleranceUp = 2;
	private int toleranceDown = 10;

	private String lsofOutput;
	private int totalFds;
	private Map<String, Integer> typeMap;
	private Map<String, Integer> miscMap;
	private Map<String, String> nodeMap;

	private String baselineLsofOutput;
	private int baselineTotalFds;
	private Map<String, Integer> baselineTypeMap;
	private Map<String, Integer> baselineMiscMap;
	private Map<String, String> baselineNodeMap;

	public Lsof(int pid) {
		super();
		this.pid = pid;
	}

	public int getToleranceUp() {
		return toleranceUp;
	}

	public void setToleranceUp(int tolerance) {
		this.toleranceUp = tolerance;
	}

	public int getToleranceDown() {
		return toleranceDown;
	}

	public void setToleranceDown(int toleranceDown) {
		this.toleranceDown = toleranceDown;
	}

	public int rememberBaseline() throws NumberFormatException, IOException, InterruptedException {
		baselineTotalFds = count();
		baselineLsofOutput = lsofOutput;
		baselineTypeMap = typeMap;
		baselineMiscMap = miscMap;
		baselineNodeMap = nodeMap;

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Baseline LSOF output:\n{}", baselineLsofOutput);
		}

		return baselineTotalFds;
	}

	public int count() throws NumberFormatException, IOException, InterruptedException {
		lsofOutput = execLsof(pid);

//		if (LOGGER.isTraceEnabled()) {
//			LOGGER.trace("LSOF output:\n{}", lsofOutput);
//		}

		String[] lines = lsofOutput.split("\n");

		Pattern fdPattern = Pattern.compile("(\\d+)(\\S*)");
		Pattern namePatternJar = Pattern.compile("/.+\\.jar");
		Pattern namePatternFile = Pattern.compile("/.*");
		Pattern namePatternPipe = Pattern.compile("pipe");
		Pattern namePatternEventpoll = Pattern.compile("\\[eventpoll\\]");

		typeMap = new HashMap<>();
		miscMap = new HashMap<>();
		nodeMap = new HashMap<>();

		totalFds = 0;
		for (int lineNum = 1; lineNum < lines.length; lineNum++) {
			String line = lines[lineNum];
			String[] columns = line.split("\\s+");
			String pidCol = columns[1];
			if (Integer.parseInt(pidCol) != pid) {
				throw new IllegalStateException("Unexpected pid in line "+lineNum+", expected "+pid+"\n"+line);
			}

			String fd = columns[3];
			Matcher fdMatcher = fdPattern.matcher(fd);
//			if (!fdMatcher.matches()) {
//				LOGGER.trace("SKIP fd {}", fd);
//				continue;
//			}

			totalFds++;

			String type = columns[4];
			increment(typeMap, type);

			String node = columns[7];
			String nodeKey = node;
			if (!StringUtils.isNumeric(nodeKey)) {
				nodeKey = nodeKey + ":" + fd;
			}
			nodeMap.put(nodeKey, line);

			String name = columns[8];
			if (namePatternJar.matcher(name).matches()) {
				increment(miscMap, "jar");
			} else if (namePatternFile.matcher(name).matches()) {
				increment(miscMap, "file");
			} else if (namePatternPipe.matcher(name).matches()) {
				increment(miscMap, "pipe");
			} else if (namePatternEventpoll.matcher(name).matches()) {
				increment(miscMap, "eventpoll");
			} else if ("TCP".equals(node)) {
				increment(miscMap, "TCP");
			} else {
				increment(miscMap, "other");
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("lsof counts:\n{}", debugDump(1));
		}

		return totalFds;
	}

	private void increment(Map<String, Integer> map, String key) {
		Integer typeCount = map.get(key);
		if (typeCount == null) {
			typeCount = 0;
		}
		typeCount++;
		map.put(key, typeCount);
	}

	private String execLsof(int pid) throws IOException, InterruptedException {
		Process process = null;
		String output = null;
		try {
			process = Runtime.getRuntime().exec(new String[]{ "lsof", "-p", Integer.toString(pid) });
			InputStream inputStream = process.getInputStream();
			output = IOUtils.toString(inputStream, "UTF-8");
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new IllegalStateException("Lsof process ended with error ("+exitCode+")");
			}
		} finally {
			if (process != null) {
				try {
					process.getInputStream().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					process.getOutputStream().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					process.getErrorStream().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				process.destroy();
			}
		}
		return output;
	}

	public void assertStable() throws NumberFormatException, IOException, InterruptedException {
		count();
		if (!checkWithinTolerance(baselineTotalFds, totalFds)) {
			LOGGER.debug("FD situation UNSTABLE ({} -> {}):\n{}", baselineTotalFds, totalFds, debugDump(1));
			logFailDump();
			AssertJUnit.fail("Unexpected number of open FDs, expected: "+baselineTotalFds+", but was "+totalFds+" (tolerance +"+toleranceUp+"/-"+toleranceDown+")");
		} else {
			LOGGER.debug("FD situation stable (total {})", totalFds);
		}
	}

	public void assertFdIncrease(int increase) throws NumberFormatException, IOException, InterruptedException {
		count();
		if (!checkWithinTolerance(baselineTotalFds + increase,  totalFds)) {
			LOGGER.debug("Unexpected FD number increase {} ({} -> {}):\n{}", (totalFds - baselineTotalFds), baselineTotalFds, totalFds, debugDump(1));
			logFailDump();
			AssertJUnit.fail("Unexpected FD number increase, expected increase " + increase + " ("+ (baselineTotalFds + increase) +"), but was "
			  + (totalFds - baselineTotalFds) + " (" + totalFds + ")"+" (tolerance +"+toleranceUp+"/-"+toleranceDown+")");
		} else {
			LOGGER.debug("Expected increase of {} FDs (total {})", increase, totalFds);
		}
	}

	private boolean checkWithinTolerance(int expected, int was) {
		return (was <= (expected + toleranceUp)) && (was >= (expected - toleranceDown));
	}

	private void logFailDump() {
		LOGGER.debug("types:\n{}", diffMap(baselineTypeMap, typeMap));
		LOGGER.debug("misc:\n{}", diffMap(baselineMiscMap, miscMap));
		LOGGER.debug("nodes:\n{}", diffNodeMap());
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("LSOF output:\n{}", lsofOutput);
		}
	}

	private String diffNodeMap() {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> baselineEntry: baselineNodeMap.entrySet()) {
			if (nodeMap.get(baselineEntry.getKey()) == null) {
				sb.append("- ").append(baselineEntry.getValue()).append("\n");
			}
		}
		for (Entry<String, String> currentEntry: nodeMap.entrySet()) {
			if (baselineNodeMap.get(currentEntry.getKey()) == null) {
				sb.append("+ ").append(currentEntry.getValue()).append("\n");
			}
		}

		return sb.toString();
	}

	private String diffMap(Map<String, Integer> baselineMap, Map<String, Integer> currentMap) {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Integer> currentEntry: currentMap.entrySet()) {
			Integer currentValue = currentEntry.getValue();
			Integer baselineValue = baselineMap.get(currentEntry.getKey());
			diff(sb, currentEntry.getKey(), baselineValue, currentValue);
		}
		for (Entry<String, Integer> baselineEntry: baselineMap.entrySet()) {
			Integer currentValue = currentMap.get(baselineEntry.getKey());
			if (currentValue == null) {
				diff(sb, baselineEntry.getKey(), baselineEntry.getValue(), currentValue);
			}
		}
		return sb.toString();
	}

	private void diff(StringBuilder sb, String key, Integer baselineValue, Integer currentValue) {
		if (baselineValue == null) {
			baselineValue = 0;
		}
		if (currentValue == null) {
			currentValue = 0;
		}
		if (baselineValue.equals(currentValue)) {
			return;
		}
		sb.append(key).append(": ");
		int diff = currentValue - baselineValue;
		if (diff > 0) {
			sb.append("+").append(diff);
		} else {
			sb.append(diff);
		}
		sb.append("\n");
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("Lsof(pid=").append(pid).append(")\n");
		DebugUtil.debugDumpWithLabelLn(sb, "baselineTotalFds", baselineTotalFds, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "totalFds", totalFds, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "typeMap", typeMap, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "miscMap", miscMap, indent + 1);
		// Do not display output and nodemap, that is too much
		return sb.toString();
	}


}
