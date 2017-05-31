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
package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.model.api.ModelPort;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteChangesResponseType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteChangesType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteScriptsResponseType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteScriptsType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.FindShadowOwnerResponseType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.FindShadowOwnerType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.GetObjectResponseType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.GetObjectType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ImportFromResourceResponseType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ImportFromResourceType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.NotifyChangeResponseType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.NotifyChangeType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.SearchObjectsResponseType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.SearchObjectsType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.TestResourceResponseType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.TestResourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Holder;
import javax.xml.ws.Provider;
import javax.xml.ws.soap.SOAPFaultException;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 
 * @author mederly
 * 
 */
@Service
public class ModelWebServiceRaw implements Provider<DOMSource> {

	private static final Trace LOGGER = TraceManager.getTrace(ModelWebServiceRaw.class);

    public static final String NS_SOAP11_ENV = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String NS_SOAP11_ENV_PREFIX = "SOAP-ENV";
    public static final QName SOAP11_FAULT = new QName(NS_SOAP11_ENV, "Fault");
    public static final QName SOAP11_FAULTCODE = new QName("", "faultcode");
    public static final String SOAP11_FAULTCODE_SERVER = NS_SOAP11_ENV_PREFIX + ":Server";
    public static final QName SOAP11_FAULTSTRING = new QName("", "faultstring");
    public static final QName SOAP11_FAULTACTOR = new QName("", "faultactor");
    public static final QName SOAP11_FAULT_DETAIL = new QName("", "detail");
    public static final String ACTOR = "TODO";

    @Autowired
	private ModelWebService ws;
	
	@Autowired
	private PrismContext prismContext;
	
    @Override
    public DOMSource invoke(DOMSource request) {
        try {
            return invokeAllowingFaults(request);
        } catch (FaultMessage faultMessage) {
            try {
                SOAPFactory factory = SOAPFactory.newInstance();
                SOAPFault soapFault = factory.createFault();
                soapFault.setFaultCode(SOAP11_FAULTCODE_SERVER);           // todo here is a constant until we have a mechanism to determine the correct value (client / server)
                soapFault.setFaultString(faultMessage.getMessage());
                Detail detail = soapFault.addDetail();
                serializeFaultMessage(detail, faultMessage);
                // fault actor?
                // stack trace of the outer exception (FaultMessage) is unimportant, because it is always created at one place
                // todo consider providing stack trace of the inner exception
                //Detail detail = soapFault.addDetail();
                //detail.setTextContent(getStackTraceAsString(faultMessage));
                throw new SOAPFaultException(soapFault);
            } catch (SOAPException e) {
                throw new RuntimeException("SOAP Exception: " + e.getMessage(), e);
            }
        }
    }

	public DOMSource invokeAllowingFaults(DOMSource request) throws FaultMessage {
        Node rootNode = request.getNode();
        Element rootElement;
        if (rootNode instanceof Document) {
            rootElement = ((Document) rootNode).getDocumentElement();
        } else if (rootNode instanceof Element) {
            rootElement = (Element) rootNode;
        } else {
            throw ws.createIllegalArgumentFault("Unexpected DOM node type: " + rootNode);
        }

        Object requestObject;
        try {
            requestObject = prismContext.parserFor(rootElement).parseRealValue();
        } catch (SchemaException e) {
            throw ws.createIllegalArgumentFault("Couldn't parse SOAP request body because of schema exception: " + e.getMessage());
        }

        Node response;
        Holder<OperationResultType> operationResultTypeHolder = new Holder<>();
        try {
            PrismSerializer<Element> serializer = prismContext.domSerializer()
                    .options(SerializationOptions.createSerializeReferenceNames());
            if (requestObject instanceof GetObjectType) {
                GetObjectType g = (GetObjectType) requestObject;
                Holder<ObjectType> objectTypeHolder = new Holder<>();
                ws.getObject(g.getObjectType(), g.getOid(), g.getOptions(), objectTypeHolder, operationResultTypeHolder);
                GetObjectResponseType gr = new GetObjectResponseType();
                gr.setObject(objectTypeHolder.value);
                gr.setResult(operationResultTypeHolder.value);
                response = serializer.serializeAnyData(gr, ModelPort.GET_OBJECT_RESPONSE);
            } else if (requestObject instanceof SearchObjectsType) {
                SearchObjectsType s = (SearchObjectsType) requestObject;
                Holder<ObjectListType> objectListTypeHolder = new Holder<>();
                ws.searchObjects(s.getObjectType(), s.getQuery(), s.getOptions(), objectListTypeHolder, operationResultTypeHolder);
                SearchObjectsResponseType sr = new SearchObjectsResponseType();
                sr.setObjectList(objectListTypeHolder.value);
                sr.setResult(operationResultTypeHolder.value);
                response = serializer.serializeAnyData(sr, ModelPort.SEARCH_OBJECTS_RESPONSE);
            } else if (requestObject instanceof ExecuteChangesType) {
                ExecuteChangesType e = (ExecuteChangesType) requestObject;
                ObjectDeltaOperationListType objectDeltaOperationListType = ws.executeChanges(e.getDeltaList(), e.getOptions());
                ExecuteChangesResponseType er = new ExecuteChangesResponseType();
                er.setDeltaOperationList(objectDeltaOperationListType);
                response = serializer.serializeAnyData(er, ModelPort.EXECUTE_CHANGES_RESPONSE);
            } else if (requestObject instanceof FindShadowOwnerType) {
                FindShadowOwnerType f = (FindShadowOwnerType) requestObject;
                Holder<UserType> userTypeHolder = new Holder<>();
                ws.findShadowOwner(f.getShadowOid(), userTypeHolder, operationResultTypeHolder);
                FindShadowOwnerResponseType fsr = new FindShadowOwnerResponseType();
                fsr.setUser(userTypeHolder.value);
                fsr.setResult(operationResultTypeHolder.value);
                response = serializer.serializeAnyData(fsr, ModelPort.FIND_SHADOW_OWNER_RESPONSE);
            } else if (requestObject instanceof TestResourceType) {
                TestResourceType tr = (TestResourceType) requestObject;
                OperationResultType operationResultType = ws.testResource(tr.getResourceOid());
                TestResourceResponseType trr = new TestResourceResponseType();
                trr.setResult(operationResultType);
                response = serializer.serializeAnyData(trr, ModelPort.TEST_RESOURCE_RESPONSE);
            } else if (requestObject instanceof ExecuteScriptsType) {
                ExecuteScriptsType es = (ExecuteScriptsType) requestObject;
                ExecuteScriptsResponseType esr = ws.executeScripts(es);
                response = serializer.serializeAnyData(esr, ModelPort.EXECUTE_SCRIPTS_RESPONSE);
            } else if (requestObject instanceof ImportFromResourceType) {
                ImportFromResourceType ifr = (ImportFromResourceType) requestObject;
                TaskType taskType = ws.importFromResource(ifr.getResourceOid(), ifr.getObjectClass());
                ImportFromResourceResponseType ifrr = new ImportFromResourceResponseType();
                ifrr.setTask(taskType);
                response = serializer.serializeAnyData(ifrr, ModelPort.IMPORT_FROM_RESOURCE_RESPONSE);
            } else if (requestObject instanceof NotifyChangeType) {
                NotifyChangeType nc = (NotifyChangeType) requestObject;
                TaskType taskType = ws.notifyChange(nc.getChangeDescription());
                NotifyChangeResponseType ncr = new NotifyChangeResponseType();
                ncr.setTask(taskType);
                response = serializer.serializeAnyData(ncr, ModelPort.NOTIFY_CHANGE_RESPONSE);
            } else {
                throw ws.createIllegalArgumentFault("Unsupported request type: " + requestObject);
            }
        } catch (SchemaException e) {
        	throwFault(e, operationResultTypeHolder.value);
        	// not reached
        	return null;
        }

        // brutal hack for MID-2001 (serializing and parsing eliminates the problem!)
        //String serialized = DOMUtil.printDom(response).toString();
        //LOGGER.trace("WEB SERVICE RESPONSE:\n{}", serialized);
        //response = DOMUtil.parseDocument(serialized);

        return new DOMSource(response);
    }
	
	private void serializeFaultMessage(Detail detail, FaultMessage faultMessage) {
		MiscSchemaUtil.serializeFaultMessage(detail, faultMessage, prismContext, LOGGER);
	}

    private void throwFault(Exception ex, OperationResultType resultType) throws FaultMessage {
		if (resultType != null) {
            ws.throwFault(ex, OperationResult.createOperationResult(resultType));
		} else {
            ws.throwFault(ex, null);
        }
	}
}
