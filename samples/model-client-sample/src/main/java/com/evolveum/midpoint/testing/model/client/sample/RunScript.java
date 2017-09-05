/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.testing.model.client.sample;

import com.evolveum.midpoint.model.client.ModelClientUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteScriptsOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.OutputFormatType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SingleScriptOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteScriptsResponseType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteScriptsType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelService;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author mederly
 *
 */
public class RunScript {

	// Configuration
	public static final String ADM_USERNAME = "administrator";
	public static final String ADM_PASSWORD = "5ecr3t";
    private static final String DEFAULT_ENDPOINT_URL = "http://localhost:8080/midpoint/model/model-3";

    private static final String PROPERTY_PREFIX = "[[!!";
    private static final String PROPERTY_SUFFIX = "!!]]";

    private static final String OPT_HELP = "h";
    public static final String OPT_SCRIPT = "s";
    public static final String OPT_URL = "url";
    public static final String OPT_USER = "u";
    public static final String OPT_PASSWORD = "p";
    public static final String OPT_FILE_FOR_DATA = "fd";
    public static final String OPT_FILE_FOR_CONSOLE = "fc";
    public static final String OPT_FILE_FOR_RESULT = "fr";
    public static final String OPT_HIDE_DATA = "hd";
    public static final String OPT_HIDE_SCRIPT = "hs";
    public static final String OPT_HIDE_CONSOLE = "hc";
    public static final String OPT_HIDE_RESULT = "hr";
    public static final String OPT_SHOW_RESULT = "sr";

    /**
	 * @param args
	 */
	public static void main(String[] args) {
		try {

            Options options = new Options();
            options.addOption(OPT_HELP, "help", false, "Print this help information");
            options.addOption(OPT_SCRIPT, "script", true, "Script file (XML for the moment)");
            options.addOption(OPT_URL, true, "Endpoint URL (default: " + DEFAULT_ENDPOINT_URL + ")");
            options.addOption(OPT_USER, "user", true, "User name (default: " + ADM_USERNAME + ")");
            options.addOption(OPT_PASSWORD, "password", true, "Password");
            options.addOption(OPT_FILE_FOR_DATA, "file-for-data", true, "Name of the file to write resulting XML data into");
            options.addOption(OPT_FILE_FOR_CONSOLE, "file-for-console", true, "Name of the file to write resulting console output into");
            options.addOption(OPT_FILE_FOR_RESULT, "file-for-result", true, "Name of the file to write operation result into");
            options.addOption(OPT_HIDE_DATA, "hide-data", false, "Don't display data output");
            options.addOption(OPT_HIDE_SCRIPT, "hide-script", false, "Don't display input script");
            options.addOption(OPT_HIDE_CONSOLE, "hide-console", false, "Don't display console output");
            options.addOption(OPT_HIDE_RESULT, "hide-result", false, "Don't display detailed operation result (default: showing if not SUCCESS)");
            options.addOption(OPT_SHOW_RESULT, "show-result", false, "Always show detailed operation result (default: showing if not SUCCESS)");
            options.addOption(
                    OptionBuilder.withArgName("property=value")
                        .hasArgs(2)
                        .withValueSeparator()
                        .withDescription("use value for given property")
                        .create("D"));
            CommandLineParser parser = new GnuParser();
            CommandLine cmdline = parser.parse(options, args);

            if (!cmdline.hasOption(OPT_SCRIPT) || cmdline.hasOption("h")) {
                HelpFormatter helpFormatter = new HelpFormatter();
                helpFormatter.printHelp("runscript", options);
                System.exit(0);
            }

            ExecuteScriptsType request = new ExecuteScriptsType();
            String script = readXmlFile(cmdline.getOptionValue(OPT_SCRIPT));
            script = replaceParameters(script, cmdline.getOptionProperties("D"));
            request.setMslScripts(script);          // todo fix this hack
            ExecuteScriptsOptionsType optionsType = new ExecuteScriptsOptionsType();
            optionsType.setOutputFormat(OutputFormatType.MSL);      // todo fix this hack
            request.setOptions(optionsType);

            if (!cmdline.hasOption(OPT_HIDE_SCRIPT)) {
                System.out.println("\nScript to execute:\n" + script);
            }
            System.out.println("=================================================================");

            ModelPortType modelPort = createModelPort(cmdline);

            ExecuteScriptsResponseType response = modelPort.executeScripts(request);

            System.out.println("=================================================================");

            for (SingleScriptOutputType output : response.getOutputs().getOutput()) {
                if (!cmdline.hasOption(OPT_HIDE_DATA)) {
                    System.out.println("Data:\n" + output.getMslData());
                    System.out.println("-----------------------------------------------------------------");
                }
                if (cmdline.hasOption(OPT_FILE_FOR_DATA)) {
                    IOUtils.write(output.getMslData(), new FileOutputStream(cmdline.getOptionValue(OPT_FILE_FOR_DATA)), "UTF-8");
                }
                if (!cmdline.hasOption(OPT_HIDE_CONSOLE)) {
                    System.out.println("Console output:\n" + output.getTextOutput());
                }
                if (cmdline.hasOption(OPT_HIDE_CONSOLE)) {
                    IOUtils.write(output.getMslData(), new FileWriter(cmdline.getOptionValue(OPT_FILE_FOR_CONSOLE)));
                }
            }

            System.out.println("=================================================================");
            System.out.println("Operation result: " + getResultStatus(response.getResult()));
            if (!cmdline.hasOption(OPT_HIDE_RESULT) && (cmdline.hasOption(OPT_SHOW_RESULT) || response.getResult() == null || response.getResult().getStatus() != OperationResultStatusType.SUCCESS)) {
                System.out.println("\n\n" + marshalResult(response.getResult()));
            }
            if (cmdline.hasOption(OPT_FILE_FOR_RESULT)) {
                IOUtils.write(marshalResult(response.getResult()), new FileWriter(cmdline.getOptionValue(OPT_FILE_FOR_RESULT)));
            }

        } catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

    private static String getResultStatus(OperationResultType result) {
        if (result == null) {
            return "(null)";
        } else {
            return result.getStatus() + (result.getMessage() != null ? (": " + result.getMessage()) : "");
        }
    }

    private static String marshalResult(OperationResultType result) throws JAXBException, FileNotFoundException {
        if (result == null) {
            return "";
        } else {
            return marshalObject(new JAXBElement<>(new QName("result"), OperationResultType.class, result));
        }
    }

    private static String replaceParameters(String script, Properties properties) {
        for (Map.Entry entry : properties.entrySet()) {
            String key = PROPERTY_PREFIX + entry.getKey() + PROPERTY_SUFFIX;
            if (!script.contains(key)) {
                throw new IllegalStateException("Property " + key + " is not present in the script");
            }
            script = script.replace(key, (String) entry.getValue());
        }
        int i = script.indexOf(PROPERTY_PREFIX);
        if (i != -1) {
            int j = script.indexOf(PROPERTY_SUFFIX, i);
            if (j == -1) {
                j = script.length();
            }
            throw new IllegalStateException("Unresolved property " + script.substring(i, j+PROPERTY_SUFFIX.length()));
        }
        return script;
    }

    // brutally hacked - reads XML file in correct encoding
    private static String readXmlFile(String filename) throws IOException {
        String encoding = determineEncoding(filename);
        FileInputStream fis = new FileInputStream(filename);
        String data = IOUtils.toString(fis, encoding);
        fis.close();
        return data;
    }

    private static String determineEncoding(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        for (;;) {
            int c = br.read();
            if (c == -1) {
                throw new IllegalStateException("No XML declaration found in file " + filename);
            }
            sb.append((char) c);
            if (c == '>') {
                break;
            }
        }
        br.close();
        String xmldecl = sb.toString();
        if (!xmldecl.startsWith("<?xml")) {
            throw new IllegalStateException("No XML declaration found in file " + filename);
        }
        int encodingPos = xmldecl.indexOf("encoding");
        if (encodingPos == -1) {
            return System.getProperty("file.encoding");
        }

        int from1 = xmldecl.indexOf("\'", encodingPos);
        int from2 = xmldecl.indexOf("\"", encodingPos);
        if (from1 == -1 && from2 == -1) {
            throw new IllegalStateException("Incorrect encoding information in XML declaration: " + xmldecl);
        }
        int from, to;
        if (from1 != -1 && from1 < from2) {
            from = from1;
            to = xmldecl.indexOf("\'", from1+1);
        } else {
            from = from2;
            to = xmldecl.indexOf("\"", from2+1);
        }
        if (to == -1) {
            throw new IllegalStateException("Incorrect encoding information in XML declaration: " + xmldecl);
        }
        return xmldecl.substring(from+1, to);
    }

    private static <T> T unmarshalFile(File file) throws JAXBException, FileNotFoundException {
		JAXBContext jc = getJaxbContext();
		Unmarshaller unmarshaller = jc.createUnmarshaller();

		InputStream is = null;
		JAXBElement<T> element = null;
		try {
			is = new FileInputStream(file);
			element = (JAXBElement<T>) unmarshaller.unmarshal(is);
		} finally {
			if (is != null) {
				IOUtils.closeQuietly(is);
			}
		}
		if (element == null) {
			return null;
		}
		return element.getValue();
	}

    private static JAXBContext jaxbContext = null;

    private static JAXBContext getJaxbContext() throws JAXBException {
        if (jaxbContext == null) {
            jaxbContext = ModelClientUtil.instantiateJaxbContext();
        }
        return jaxbContext;
    }

    private static String marshalObject(Object object) throws JAXBException, FileNotFoundException {
        JAXBContext jc = getJaxbContext();
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        StringWriter sw = new StringWriter();
        marshaller.marshal(object, sw);
        return sw.toString();
    }


    private static <T> T unmarshallResouce(String path) throws JAXBException, FileNotFoundException {
		JAXBContext jc = getJaxbContext();
		Unmarshaller unmarshaller = jc.createUnmarshaller();

		InputStream is = null;
		JAXBElement<T> element = null;
		try {
			is = RunScript.class.getClassLoader().getResourceAsStream(path);
			if (is == null) {
				throw new FileNotFoundException("System resource "+path+" was not found");
			}
			element = (JAXBElement<T>) unmarshaller.unmarshal(is);
		} finally {
			if (is != null) {
				IOUtils.closeQuietly(is);
			}
		}
		if (element == null) {
			return null;
		}
		return element.getValue();
	}

	public static ModelPortType createModelPort(CommandLine cmdLine) {
		String endpointUrl = cmdLine.getOptionValue(OPT_URL, DEFAULT_ENDPOINT_URL);
        String user = cmdLine.getOptionValue(OPT_USER, ADM_USERNAME);
		ClientPasswordHandler.setPassword(cmdLine.getOptionValue(OPT_PASSWORD, ADM_PASSWORD));
		System.out.println("Endpoint URL: " + endpointUrl);

		ModelService modelService = new ModelService();
		ModelPortType modelPort = modelService.getModelPort();
		BindingProvider bp = (BindingProvider)modelPort;
		Map<String, Object> requestContext = bp.getRequestContext();
		requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

		org.apache.cxf.endpoint.Client client = ClientProxy.getClient(modelPort);
		org.apache.cxf.endpoint.Endpoint cxfEndpoint = client.getEndpoint();

		Map<String,Object> outProps = new HashMap<>();

		outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
		outProps.put(WSHandlerConstants.USER, user);
		outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
		outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordHandler.class.getName());

		WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
		cxfEndpoint.getOutInterceptors().add(wssOut);

		return modelPort;
	}

}
