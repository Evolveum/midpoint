/**
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.infra.wsutil;

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

/**
 * @author semancik
 *
 */
public abstract class AbstractWebServiceClient<P,S extends Service> {

	private Options options = new Options();
	private CommandLine commandLine;
	private boolean verbose = false;

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	protected abstract S createService() throws Exception;

	protected abstract Class<P> getPortClass();

	protected abstract String getDefaultUsername();

	protected String getPasswordType() {
		if (commandLine.hasOption('P')) {
			String optionValue = commandLine.getOptionValue('P');
			if ("text".equals(optionValue)) {
				return WSConstants.PW_TEXT;
			} else if ("digest".equals(optionValue)) {
				return WSConstants.PW_DIGEST;
			} else {
				throw new IllegalArgumentException("Unknown password type "+optionValue);
			}
		} else {
			return WSConstants.PW_TEXT;
		}
	}

	protected abstract String getDefaultPassword();

	protected abstract String getDefaultEndpointUrl();

	protected abstract int invoke(P port);

	public void main(String[] args) {
		try {
			init(args);
			P port = createPort();
			int exitCode = invoke(port);
			System.exit(exitCode);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	protected void init(String[] args) throws ParseException {
		options.addOption("a", "authentication", true, "Authentication type ("+WSHandlerConstants.USERNAME_TOKEN+", none)");
		options.addOption("u", "user", true, "Username");
		options.addOption("p", "password", true, "Password");
		options.addOption("P", "password-type", true, "Password type (text or digest)");
		options.addOption("e", "endpoint", true, "Endpoint URL");
		options.addOption("v", "verbose", false, "Verbose mode");
		options.addOption("m", "messages", false, "Log SOAP messages");
		options.addOption("h", "help", false, "Usage help");
		extendOptions(options);
		parseCommandLine(args);
	}

	private void parseCommandLine(String[] args) throws ParseException {
		CommandLineParser cliParser = new GnuParser();
		commandLine = cliParser.parse(options, args, true);
		if (commandLine.hasOption('h')) {
			printHelp();
			System.exit(0);
		}
		if (commandLine.hasOption('v')) {
			verbose = true;
		}
	}

	protected Options getOptions() {
		return options;
	}

	public CommandLine getCommandLine() {
		return commandLine;
	}

	protected void extendOptions(Options options) {
		// nothing here. meant to be overridden
	}

	protected P createPort() throws Exception {

		String password = getDefaultPassword();
		String username = getDefaultUsername();
		String endpointUrl = getDefaultEndpointUrl();

		if (commandLine.hasOption('p')) {
			password = commandLine.getOptionValue('p');
		}
		if (commandLine.hasOption('u')) {
			username = commandLine.getOptionValue('u');
		}
		if (commandLine.hasOption('e')) {
			endpointUrl = commandLine.getOptionValue('e');
		}

		if (verbose) {
			System.out.println("Username: "+username);
			System.out.println("Password: <not shown>");
			System.out.println("Endpoint URL: "+endpointUrl);
		}

        // uncomment this if you want to use Fiddler or any other proxy
        //ProxySelector.setDefault(new MyProxySelector("127.0.0.1", 8888));

		P modelPort = createService().getPort(getPortClass());
		BindingProvider bp = (BindingProvider)modelPort;
		Map<String, Object> requestContext = bp.getRequestContext();
		requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

		org.apache.cxf.endpoint.Client client = ClientProxy.getClient(modelPort);
		org.apache.cxf.endpoint.Endpoint cxfEndpoint = client.getEndpoint();

		Map<String,Object> wssProps = new HashMap<String,Object>();

		if (!commandLine.hasOption('a') ||
				(commandLine.hasOption('a') && WSHandlerConstants.USERNAME_TOKEN.equals(commandLine.getOptionValue('a')))) {

			wssProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
			wssProps.put(WSHandlerConstants.USER, username);
			wssProps.put(WSHandlerConstants.PASSWORD_TYPE, getPasswordType());
			wssProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordHandler.class.getName());
			ClientPasswordHandler.setPassword(password);

			WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(wssProps);
			cxfEndpoint.getOutInterceptors().add(wssOut);
		} else if (commandLine.hasOption('a') && "none".equals(commandLine.getOptionValue('a'))) {
			// Nothing to do
		} else {
			throw new IllegalArgumentException("Unknown authentication mechanism '"+commandLine.getOptionValue('a')+"'");
		}

		if (commandLine.hasOption('m')) {
			cxfEndpoint.getInInterceptors().add(new LoggingInInterceptor());
			cxfEndpoint.getOutInterceptors().add(new LoggingOutInterceptor());
		}

		return modelPort;
	}

	protected void printHelp() {
		final String commandLineSyntax = System.getProperty("sun.java.command");
		final HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.printHelp(commandLineSyntax, options);
	}
}
