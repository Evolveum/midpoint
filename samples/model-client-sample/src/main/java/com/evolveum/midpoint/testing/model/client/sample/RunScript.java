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
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ExecuteScriptsOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OutputFormatType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.SingleScriptOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ExecuteScripts;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ExecuteScriptsResponse;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelService;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.ModificationTypeType;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
	private static final String DEFAULT_ENDPOINT_URL = "http://localhost:8080/midpoint/model/model-1";

    private static final String PROPERTY_PREFIX = "[[!!";
    private static final String PROPERTY_SUFFIX = "!!]]";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {

            Options options = new Options();
            options.addOption("h", "help", false, "Print this help information");
            options.addOption("s", "script", true, "Script file (XML for the moment)");
            options.addOption("url", true, "Endpoint URL (default: " + DEFAULT_ENDPOINT_URL + ")");
            options.addOption("u", "user", true, "User name (default: " + ADM_USERNAME + ")");
            options.addOption("p", "password", true, "Password");
            options.addOption(
                    OptionBuilder.withArgName("property=value")
                        .hasArgs(2)
                        .withValueSeparator()
                        .withDescription("use value for given property")
                        .create("D"));
            CommandLineParser parser = new GnuParser();
            CommandLine cmdline = parser.parse(options, args);

            if (!cmdline.hasOption("s") || cmdline.hasOption("h")) {
                HelpFormatter helpFormatter = new HelpFormatter();
                helpFormatter.printHelp("runscript", options);
                System.exit(0);
            }

            ExecuteScripts request = new ExecuteScripts();
            String script = readXmlFile(cmdline.getOptionValue("s"));
            script = replaceParameters(script, cmdline.getOptionProperties("D"));
            request.setMslScripts(script);          // todo fix this hack
            ExecuteScriptsOptionsType optionsType = new ExecuteScriptsOptionsType();
            optionsType.setOutputFormat(OutputFormatType.MSL);      // todo fix this hack
            request.setOptions(optionsType);

            System.out.println("Script to execute: " + script);
            System.out.println("=================================================================");

            ModelPortType modelPort = createModelPort(cmdline);

            ExecuteScriptsResponse response = modelPort.executeScripts(request);

            for (SingleScriptOutputType output : response.getOutputs().getOutput()) {
                System.out.println("Data: " + output.getMslData());
                System.out.println("Console output: " + output.getTextOutput());
            }
        } catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
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
		JAXBContext jc = ModelClientUtil.instantiateJaxbContext();
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
	
	private static <T> T unmarshallResouce(String path) throws JAXBException, FileNotFoundException {
		JAXBContext jc = ModelClientUtil.instantiateJaxbContext();
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
		String endpointUrl = cmdLine.getOptionValue("url", DEFAULT_ENDPOINT_URL);
        String user = cmdLine.getOptionValue("u", ADM_USERNAME);
		ClientPasswordHandler.setPassword(cmdLine.getOptionValue("p", ADM_PASSWORD));
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
