/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.util;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author mederly
 */
public class SystemUtil {

	private static final Trace LOGGER = TraceManager.getTrace(SystemUtil.class);

	public static boolean executeCommand(String command, String input, StringBuilder output, long timeout) throws IOException {
		LOGGER.debug("Executing {}", command);
		boolean finished;
		try {
			Process process = Runtime.getRuntime().exec(command);

			Writer writer = new OutputStreamWriter(process.getOutputStream());
			writer.append(input);
			writer.close();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
			try {
				if (timeout > 0) {
					finished = process.waitFor(timeout, TimeUnit.MILLISECONDS);
					process.destroyForcibly();
				} else {
					process.waitFor();
					finished = true;
				}
			} catch (InterruptedException e) {
				throw new SystemException("Got interruptedException while waiting for external command execution", e);
			}
		} catch (IOException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute command {}", e, command);
			throw e;
		}
		if (finished) {
			LOGGER.debug("Finished executing {}; result has a length of {} characters", command, output.length());
		} else {
			LOGGER.warn("Timed out while waiting for {} to finish ({} seconds); result collected to this moment has "
					+ "a length of {} characters", command, timeout/1000.0, output.length());
		}
		return finished;
	}
	
	public static void setPrivateFilePermissions(String fileName) throws IOException {
		Set<PosixFilePermission> perms = new HashSet<>();
		perms.add(PosixFilePermission.OWNER_READ);
		perms.add(PosixFilePermission.OWNER_WRITE);
		perms.add(PosixFilePermission.GROUP_READ);
		perms.add(PosixFilePermission.GROUP_WRITE);
		try {
			Files.setPosixFilePermissions(Paths.get(fileName), perms);
		} catch (UnsupportedOperationException e) {
			// Windows. Sorry.
			LOGGER.trace("Cannot set permissions for file {}, this is obviously not a POSIX system", fileName);
		}
	}

	public static String getMyPid() {
		String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		if (jvmName != null) {
			int i = jvmName.indexOf('@');
			if (i > 0) {
				return jvmName.substring(0, i);
			} else {
				LOGGER.warn("Cannot determine current PID; jvmName = {}", jvmName);
				return null;
			}
		} else {
			LOGGER.warn("Cannot determine current PID; jvmName is null");
			return null;
		}
	}
}
