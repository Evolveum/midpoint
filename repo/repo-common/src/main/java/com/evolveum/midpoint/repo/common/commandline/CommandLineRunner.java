/**
 * Copyright (c) 2017 Evolveum
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CommandLineExecutionMethod;

/**
 * @author matus
 * @author semancik
 */
public class CommandLineRunner {
	
	private static final Trace LOGGER = TraceManager.getTrace(CommandLineRunner.class);
	
	private static final Pattern REGEX_CODE_SPLITTER = Pattern.compile("([^\"]\\S*|\".+?\")\\s*"); // bash -c "echo Im not a number, im a ; echo free man"
	private static final int EXIT_SUCCESS = 0;
	private static final String SHELL = "bash";
    
	private final String code;
	private final OperationResult result;
	private CommandLineExecutionMethod exectionMethod;
	private Map<String, String> env;
	
	private Boolean warningHasEmerged = false;

	public CommandLineRunner(String code, OperationResult result) {
		super();
		this.code = code;
		this.result = result;
	}

	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	public CommandLineExecutionMethod getExectionMethod() {
		return exectionMethod;
	}

	public void setExectionMethod(CommandLineExecutionMethod exectionMethod) {
		this.exectionMethod = exectionMethod;
	}

	public void execute() throws IOException, InterruptedException {
        

        ProcessBuilder processBuilder = new ProcessBuilder(produceCommand());

        if (env != null) {
        	processBuilder.environment().putAll(env);
        }
                
        LOGGER.debug("Starting process {}", processBuilder.command());

        Process process = processBuilder.start();
        Integer exitValue = process.waitFor();

        if (exitValue == null) {
            LOGGER.error("Unknown process error, process did not return an exit value.");
        } else {

            try (InputStream errorInputStream = process.getErrorStream();
                 InputStream processInputStream = process.getInputStream()) {
                if (errorInputStream == null) {
                    evaluateExitValue(exitValue, readOutput(processInputStream));
                } else {
                    evaluateExitValue(exitValue, readOutput(processInputStream, errorInputStream));
                }
            }
        }
        result.computeStatus();
    }

    
    private List<String> produceCommand() {
    	if (exectionMethod == null) {
    		return produceCommandExec();
    	}
    	switch (exectionMethod) {
    		case EXEC:
    			return produceCommandExec();
    		case SHELL:
    			return produceCommandShell();
    		default:
    			throw new IllegalArgumentException("Unknown exec method "+exectionMethod);
    	}
    	
    }
    
    private List<String> produceCommandExec() {
    	Matcher match = REGEX_CODE_SPLITTER.matcher(code);
        List<String> scriptParts = new ArrayList<>();
        while (match.find()) {
            String processedCommand = match.group(1);
            scriptParts.add(processedCommand);
        }
        LOGGER.debug("The constructed list of commands: {}", scriptParts);
        return scriptParts;
	}
    
    private List<String> produceCommandShell() {
        List<String> commands = new ArrayList<>();
        commands.add(SHELL);
        commands.add("-c");
        commands.add(code);
        LOGGER.debug("Constructed shell commands: {}", commands);
        return commands;
	}

	private String readOutput(InputStream processInputStream) throws IOException {
        return readOutput(processInputStream, null);
    }

    private String readOutput(InputStream processInputStream, InputStream errorStream) throws IOException {
        // LOGGER.debug("Evaluating output ");
        StringBuilder outputBuilder = new StringBuilder();
        try (BufferedReader bufferedProcessOutputReader = new BufferedReader(new InputStreamReader(processInputStream))) {
            String line = null;
            if (errorStream != null) {
                try (BufferedReader bufferedProcessErrorOutputReader = new BufferedReader(new InputStreamReader(errorStream))) {
                    outputBuilder.append(" Partial error while executing post report script: ").append(System.getProperty("line.separator"));
                    if (bufferedProcessErrorOutputReader.ready()) {
                        while ((line = bufferedProcessErrorOutputReader.readLine()) != null) {
                            outputBuilder.append(" * " + line + System.getProperty("line.separator"));
                        }
                        String aWarning = outputBuilder.toString();
                        LOGGER.warn(aWarning);

                        result.recordWarning(aWarning);
                        warningHasEmerged = true;
                    }
                }
            }
            outputBuilder = new StringBuilder();
            while ((line = bufferedProcessOutputReader.readLine()) != null) {
                outputBuilder.append(line + System.getProperty("line.separator"));
            }
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
        StringBuilder messageBuilder = new StringBuilder();
        if (exitValue != EXIT_SUCCESS) {
            messageBuilder.append("Process exited with an error, the exit value: ").append(exitValue)
                    .append(". Only a part of the script might have been executed, the output: ").append(System.getProperty("line.separator")).append(message);
            String warnMessage = messageBuilder.toString();
            LOGGER.warn(warnMessage);
            if (!warningHasEmerged) {
                result.recordWarning(warnMessage);
            }
        } else {
            LOGGER.debug("Script execution successful, the following output string was returned: {}", message);
            if (!warningHasEmerged) {
                result.recordSuccess();
            }
        }
    }
}
