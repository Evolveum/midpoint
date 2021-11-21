/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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
                    + "a length of {} characters", command, timeout / 1000.0, output.length());
        }
        return finished;
    }

    public static void setPrivateFilePermissions(String fileName) throws IOException {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
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
