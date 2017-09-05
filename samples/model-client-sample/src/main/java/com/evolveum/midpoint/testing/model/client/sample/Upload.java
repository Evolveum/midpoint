/*
 * Copyright (c) 2010-2015 Evolveum
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
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteChangesType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelService;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mederly
 *
 */
public class Upload {

	// Configuration
	public static final String ADM_USERNAME = "administrator";
	public static final String ADM_PASSWORD = "5ecr3t";
    private static final String DEFAULT_ENDPOINT_URL = "http://localhost:8080/midpoint/model/model-3";

    private static final String OPT_HELP = "h";
    public static final String OPT_FILE_TO_UPLOAD = "f";
    public static final String OPT_DIR_TO_UPLOAD = "d";
    public static final String OPT_URL = "url";
    public static final String OPT_USER = "u";
    public static final String OPT_PASSWORD = "p";
    public static final String OPT_FILE_FOR_RESULT = "fr";
    public static final String OPT_HIDE_RESULT = "hr";
    public static final String OPT_SHOW_RESULT = "sr";

    private static boolean error = false;               // TODO implement seriously

	public static void main(String[] args) {
		try {

            Options options = new Options();
            options.addOption(OPT_HELP, "help", false, "Print this help information");
            options.addOption(OPT_FILE_TO_UPLOAD, "file", true, "File to be uploaded (XML for the moment)");
            options.addOption(OPT_DIR_TO_UPLOAD, "dir", true, "Whole directory to be uploaded (XML files only for the moment)");
            options.addOption(OPT_URL, true, "Endpoint URL (default: " + DEFAULT_ENDPOINT_URL + ")");
            options.addOption(OPT_USER, "user", true, "User name (default: " + ADM_USERNAME + ")");
            options.addOption(OPT_PASSWORD, "password", true, "Password");
            options.addOption(OPT_FILE_FOR_RESULT, "file-for-result", true, "Name of the file to write operation result into");
            options.addOption(OPT_HIDE_RESULT, "hide-result", false, "Don't display detailed operation result (default: showing if not SUCCESS)");
            options.addOption(OPT_SHOW_RESULT, "show-result", false, "Always show detailed operation result (default: showing if not SUCCESS)");
            CommandLineParser parser = new GnuParser();
            CommandLine cmdline = parser.parse(options, args);

            if (cmdline.hasOption(OPT_HELP) || (!cmdline.hasOption(OPT_FILE_TO_UPLOAD) && !cmdline.hasOption(OPT_DIR_TO_UPLOAD))) {
                HelpFormatter helpFormatter = new HelpFormatter();
                helpFormatter.printHelp("upload", options);
                System.out.println("\nNote that currently it is possible to upload only one object per file (i.e. no <objects> tag), and the " +
                        "file must be written using fully specified XML namespaces (i.e. namespace-guessing algorithm allowing to write data " +
                        "without namespaces is not available).");
                System.exit(-1);
            }

            System.out.println("=================================================================");

            ModelPortType modelPort = createModelPort(cmdline);
            if (cmdline.hasOption(OPT_FILE_TO_UPLOAD)) {
                uploadFile(new File(cmdline.getOptionValue(OPT_FILE_TO_UPLOAD)), cmdline, modelPort);
            }
            if (cmdline.hasOption(OPT_DIR_TO_UPLOAD)) {
                uploadDir(cmdline.getOptionValue(OPT_DIR_TO_UPLOAD), cmdline, modelPort);
            }

        } catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

        if (error) {
            System.exit(-1);
        }
	}

    private static void uploadDir(String dirname, CommandLine cmdline, ModelPortType modelPort) {
        File[] files = new File(dirname).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toUpperCase().endsWith(".XML");
            }
        });
        for (File file : files) {
            uploadFile(file, cmdline, modelPort);
        }
    }

    private static void uploadFile(File file, CommandLine cmdline, ModelPortType modelPort) {
        System.out.println("Uploading file " + file);
        Object content;
        try {
            content = unmarshalFile(file);
        } catch (JAXBException|FileNotFoundException e) {
            System.err.println("Cannot read " + file + ": " + e.getMessage());
            e.printStackTrace();
            error = true;
            return;
        }
        if (content == null) {
            System.err.println("Nothing to be uploaded.");
            error = true;
            return;
        }
        if (!(content instanceof ObjectType)) {
            System.err.println("Expected an ObjectType to be uploaded; found " + content.getClass());
            error = true;
            return;
        }

        ObjectType objectType = (ObjectType) content;
        ObjectDeltaType objectDeltaType = new ObjectDeltaType();
        objectDeltaType.setChangeType(ChangeTypeType.ADD);
        objectDeltaType.setObjectType(ModelClientUtil.getTypeQName(objectType.getClass()));
        objectDeltaType.setObjectToAdd(objectType);

        ObjectDeltaListType objectDeltaListType = new ObjectDeltaListType();
        objectDeltaListType.getDelta().add(objectDeltaType);

        ModelExecuteOptionsType optionsType = new ModelExecuteOptionsType();
        optionsType.setIsImport(true);
        optionsType.setReevaluateSearchFilters(true);
        optionsType.setRaw(true);
        optionsType.setOverwrite(true);

        ExecuteChangesType executeChangesType = new ExecuteChangesType();
        executeChangesType.setDeltaList(objectDeltaListType);
        executeChangesType.setOptions(optionsType);

        ObjectDeltaOperationListType response;
        try {
            response = modelPort.executeChanges(objectDeltaListType, optionsType);
        } catch (Exception e) {
            System.err.println("Got exception when trying to execute the changes " + e.getMessage());
            error = true;
            return;
        }

        System.out.println("-----------------------------------------------------------------");

        if (response == null) {
            System.err.println("Unexpected empty response");
            error = true;
            return;
        }
        OperationResultType overallResult = new OperationResultType();
        boolean allSuccess = true;
        for (ObjectDeltaOperationType objectDeltaOperation : response.getDeltaOperation()) {
            if (objectDeltaOperation.getObjectDelta() != null) {
                ObjectDeltaType delta = objectDeltaOperation.getObjectDelta();
                System.out.print(delta.getChangeType() + " delta with OID " + delta.getOid() + " ");
            }
            OperationResultType resultType = objectDeltaOperation.getExecutionResult();
            System.out.println("resulted in " + getResultStatus(resultType));
            if (resultType == null || resultType.getStatus() != OperationResultStatusType.SUCCESS) {
                allSuccess = false;
                error = true;
            }
            overallResult.getPartialResults().add(resultType);
        }

        if (!cmdline.hasOption(OPT_HIDE_RESULT) && (cmdline.hasOption(OPT_SHOW_RESULT) || !allSuccess)) {
            System.out.println("\n\n" + marshalResult(overallResult));
        }
        if (cmdline.hasOption(OPT_FILE_FOR_RESULT)) {
            String filename = cmdline.getOptionValue(OPT_FILE_FOR_RESULT);
            try {
                FileWriter fileWriter = new FileWriter(filename, true);
                IOUtils.write(marshalResult(overallResult), fileWriter);
            } catch (IOException e) {
                System.err.println("Couldn't write operation result to file " + filename + ": " + e.getMessage());
                e.printStackTrace();
                error = true;
            }
        }

    }

    private static String getResultStatus(OperationResultType result) {
        if (result == null) {
            return "(null)";
        } else {
            return result.getStatus() + (result.getMessage() != null ? (": " + result.getMessage()) : "");
        }
    }

    private static String marshalResult(OperationResultType result) {
        if (result == null) {
            return "";
        } else {
            try {
                return marshalObject(new JAXBElement<>(new QName("result"), OperationResultType.class, result));
            } catch (JAXBException|FileNotFoundException e) {
                error = true;
                return("Couldn't marshall result: " + e.getMessage());
            }
        }
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

    private static Object unmarshalFile(File file) throws JAXBException, FileNotFoundException {
        JAXBContext jc = ModelClientUtil.instantiateJaxbContext();
        Unmarshaller unmarshaller = jc.createUnmarshaller();

        Object o = unmarshaller.unmarshal(new FileInputStream(file));
        if (o instanceof JAXBElement) {
            return ((JAXBElement) o).getValue();
        } else {
            return o;
        }
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
