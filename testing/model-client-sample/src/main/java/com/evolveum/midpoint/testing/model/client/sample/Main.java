/**
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.testing.model.client.sample;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.Holder;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.frontend.ClientProxy;

import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
//import com.evolveum.midpoint.util.JAXBUtil;
//import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelService;
import com.evolveum.prism.xml.ns._public.types_2.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;

/**
 * @author semancik
 *
 */
public class Main {
	
	public static final String NS_COMMON = "http://midpoint.evolveum.com/xml/ns/public/common/common-2a";
	private static final QName COMMON_PATH = new QName(NS_COMMON, "path");
	private static final QName COMMON_VALUE = new QName(NS_COMMON, "value");
	
	public static final String NS_TYPES = "http://prism.evolveum.com/xml/ns/public/types-2";
	private static final QName TYPES_POLYSTRING_ORIG = new QName(NS_TYPES, "orig");
	
	private static final DocumentBuilder domDocumentBuilder;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			ModelPortType modelPort = createModelPort(args);
			
			getConfiguration(modelPort);
			
			String oid = createUser(modelPort);
			
			changeUserPassword(modelPort, oid, "MIGHTYpirate");
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void getConfiguration(ModelPortType modelPort) throws FaultMessage {

		Holder<ObjectType> objectHolder = new Holder<ObjectType>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		OperationOptionsType options = new OperationOptionsType();
		
		modelPort.getObject(getTypeUri(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), options, 
				objectHolder, resultHolder);
		
		System.out.println("Got system configuration");
		System.out.println(objectHolder.value);
	}

	private static String createUser(ModelPortType modelPort) throws FaultMessage {
		Document doc = getDocumnent();
		
		UserType user = new UserType();
		user.setName(createPolyStringType("guybrush", doc));
		user.setFullName(createPolyStringType("Guybrush Threepwood", doc));
		user.setGivenName(createPolyStringType("Guybrush", doc));
		user.setFamilyName(createPolyStringType("Threepwood", doc));
		user.setEmailAddress("guybrush@meleeisland.net");
		user.getOrganizationalUnit().add(createPolyStringType("Pirate Wannabes", doc));
		user.setCredentials(createPasswordCredentials("IwannaBEaPIRATE"));
		
		Holder<String> oidHolder = new Holder<String>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		modelPort.addObject(user, oidHolder, resultHolder);
		
		return oidHolder.value;
	}

	private static void changeUserPassword(ModelPortType modelPort, String oid, String newPassword) throws FaultMessage {
		Document doc = getDocumnent();
		
		ObjectModificationType userDelta = new ObjectModificationType();
		userDelta.setOid(oid);
		
		ItemDeltaType passwordDelta = new ItemDeltaType();
		passwordDelta.setModificationType(ModificationTypeType.REPLACE);
		passwordDelta.setPath(createPathElement("credentials/password", doc));
		ItemDeltaType.Value passwordValue = new ItemDeltaType.Value();
		passwordValue.getAny().add(toJaxbElement(COMMON_VALUE, createProtectedString(newPassword)));
		passwordDelta.setValue(passwordValue);
		userDelta.getModification().add(passwordDelta);
		
		modelPort.modifyObject(getTypeUri(UserType.class), userDelta);
	}

	private static <T> JAXBElement<T> toJaxbElement(QName name, T value) {
		return new JAXBElement<T>(name, (Class<T>) value.getClass(), value);
	}

	private static Element createPathElement(String stringPath, Document doc) {
		String pathDeclaration = "declare default namespace '" + NS_COMMON + "'; " + stringPath;
		return createTextElement(COMMON_PATH, pathDeclaration, doc);
	}

	private static PolyStringType createPolyStringType(String string, Document doc) {
		PolyStringType polyStringType = new PolyStringType();
		Element origElement = createTextElement(TYPES_POLYSTRING_ORIG, string, doc);
		polyStringType.getContent().add(origElement);
		return polyStringType;
	}
	
	private static Element createTextElement(QName qname, String value, Document doc) {
		Element element = doc.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
		element.setTextContent(value);
		return element;
	}

	private static Document getDocumnent() {
		return domDocumentBuilder.newDocument();
	}

	private static String getTypeUri(Class<? extends ObjectType> type) {
//		QName typeQName = JAXBUtil.getTypeQName(type);
//		String typeUri = QNameUtil.qNameToUri(typeQName);
		String typeUri = NS_COMMON + "#" + type.getSimpleName();
		return typeUri;
	}

	private static CredentialsType createPasswordCredentials(String password) {
		CredentialsType credentialsType = new CredentialsType();
		credentialsType.setPassword(createPasswordType(password));
		return credentialsType;
	}
	
	private static PasswordType createPasswordType(String password) {
		PasswordType passwordType = new PasswordType();
		passwordType.setValue(createProtectedString(password));
		return passwordType;
	}

	private static ProtectedStringType createProtectedString(String clearValue) {
		ProtectedStringType protectedString = new ProtectedStringType();
		protectedString.setClearValue(clearValue);
		return protectedString;
	}

	private static ModelPortType createModelPort(String[] args) {
		String endpointUrl = "http://localhost:8080/midpoint/model/model-1";
		String username = "administrator";
		
		if (args.length > 0) {
			endpointUrl = args[0];
		}

		System.out.println("Endpoint URL: "+endpointUrl);
		
		ModelService modelService = new ModelService();
		ModelPortType modelPort = modelService.getModelPort();
		BindingProvider bp = (BindingProvider)modelPort;
		Map<String, Object> requestContext = bp.getRequestContext();
		requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
		
		org.apache.cxf.endpoint.Client client = ClientProxy.getClient(modelPort);
		org.apache.cxf.endpoint.Endpoint cxfEndpoint = client.getEndpoint();
		
		Map<String,Object> outProps = new HashMap<String,Object>();
		
		outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
		outProps.put(WSHandlerConstants.USER, username);
		outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
		outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordHandler.class.getName());
		
		WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
		cxfEndpoint.getOutInterceptors().add(wssOut);

		return modelPort;
	}

	static {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			domDocumentBuilder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new IllegalStateException("Error creating XML document " + ex.getMessage());
		}
	}
}
