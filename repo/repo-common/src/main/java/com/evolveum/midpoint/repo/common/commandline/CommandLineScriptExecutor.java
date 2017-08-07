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

package com.evolveum.midpoint.repo.common.commandline;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang3.SystemUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by matus on 7/17/2017.
 */

public class CommandLineScriptExecutor {
    public static final int EXIT_SUCCESS = 0;
    public static final String QOTATION_MARK = "\"";
    public static final String REGEX_CODE_SPLITTER = "([^\"]\\S*|\".+?\")\\s*";// bash -c "echo Im not a number, im a ; echo free man"
    //-> [bash,-c,"echo Im not a number, im a ; echo free man"]
    public static final String VARIABLE_REPORT = "$report";

    private String generatedOutputFilePath;

    private static final Trace LOGGER = TraceManager.getTrace(CommandLineScriptExecutor.class);

    public CommandLineScriptExecutor(String code, String generatedOutputFilePath, Map<String, String> variables) throws IOException, InterruptedException {
        this.generatedOutputFilePath = modifyFilepathDependingOnOS(generatedOutputFilePath);
        LOGGER.debug("The shell code to be executed: {}", code);
        executeScript(code, variables);
    }

    public void executeScript(String code, Map<String, String> variables) throws IOException, InterruptedException {
        code = code.replaceAll("\n", " "); // Remove new lines, replace with space
        Matcher match = Pattern.compile(REGEX_CODE_SPLITTER).matcher(code);

        List<String> scriptParts = new ArrayList<String>();

        while (match.find()) {

            String processedCommand = match.group(1);
            if (processedCommand.startsWith(QOTATION_MARK) && processedCommand.endsWith(QOTATION_MARK)) {
                processedCommand = processedCommand.substring(1, processedCommand.length() - 1);
            }
            if (processedCommand.contains(VARIABLE_REPORT)) {
                processedCommand = processedCommand.replace(VARIABLE_REPORT, generatedOutputFilePath);
            }
            scriptParts.add(processedCommand);
        }

        LOGGER.debug("The constructed list of commands: {}", scriptParts);
        ProcessBuilder processBuilder = new ProcessBuilder(scriptParts);

        if (variables != null && !variables.isEmpty()) {
            Map<String, String> environmentVariables = processBuilder.environment();
            for (String variableName : variables.keySet()) {
                environmentVariables.put(variableName, variables.get(variableName));
            }
        }
        LOGGER.debug("Starting process ", processBuilder.command());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        Integer exitValue = process.waitFor();

        if (exitValue == null) {
            LOGGER.error("Unknown process error, process did not return an exit value.");
        } else {
            evaluateExitValue(exitValue, readOutput(process.getInputStream())); // input stream closed by finally block
        }

    }

    private String readOutput(InputStream inputStream) throws IOException {
        // LOGGER.debug("Evaluating output ");
        StringBuilder outputBuilder = new StringBuilder();
        BufferedReader bufferedProcessOutputReader = null;
        try {
            bufferedProcessOutputReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = bufferedProcessOutputReader.readLine()) != null) {
                outputBuilder.append(line + System.getProperty("line.separator"));
            }
        } finally {
            bufferedProcessOutputReader.close();
        }
        if (outputBuilder != null) {
            String outputString = outputBuilder.toString();
            return outputString;
        } else {
            String outputString = "The process did not return any printable output";
            return outputString;
        }
    }

    private void evaluateExitValue(Integer exitValue, String message) {
        if (exitValue != EXIT_SUCCESS) {
            LOGGER.warn("Process exited with an error, the exit value {}. Only a part of the script might have been executed, the output containing the error message: {}", exitValue, message);
        } else {
            LOGGER.debug("Script execution successful, the following output string was returned: {}", message);
        }
    }

    private String modifyFilepathDependingOnOS(String filepath) {
        StringBuilder pathEscapedSpaces = new StringBuilder();
        if (SystemUtils.IS_OS_LINUX) {
            pathEscapedSpaces.append("'").append(filepath).append("'");
        } else if (SystemUtils.IS_OS_WINDOWS) {
            filepath = filepath.replace("/", "\\");
            pathEscapedSpaces.append(QOTATION_MARK).append(filepath).append(QOTATION_MARK);
        } else {
            return filepath;
        }
        return pathEscapedSpaces.toString();
    }

}
