/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.testing.model.client.sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.Holder;
import javax.xml.ws.BindingProvider;

import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.SelectorQualifiedGetOptionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.SelectorQualifiedGetOptionsType;
import com.evolveum.prism.xml.ns._public.query_2.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_2.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.RawType;
import com.ibm.wsdl.extensions.schema.SchemaConstants;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.frontend.ClientProxy;

import com.evolveum.midpoint.model.client.ModelClientUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
//import com.evolveum.midpoint.util.JAXBUtil;
//import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelService;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 * @author semancik
 *
 */
public class Main {
	
	// Configuration
	public static final String ADM_USERNAME = "administrator";
	public static final String ADM_PASSWORD = "5ecr3t";
	private static final String DEFAULT_ENDPOINT_URL = "http://localhost:8080/midpoint/model/model-1";
	
	// Object OIDs
	private static final String ROLE_PIRATE_OID = "12345678-d34d-b33f-f00d-987987987988";
	private static final String ROLE_CAPTAIN_OID = "12345678-d34d-b33f-f00d-987987cccccc";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			ModelPortType modelPort = createModelPort(args);
			
			SystemConfigurationType configurationType = getConfiguration(modelPort);
			System.out.println("Got system configuration");
			System.out.println(configurationType);
			
			UserType userAdministrator = searchUserByName(modelPort, "administrator");
			System.out.println("Got administrator user");
			System.out.println(userAdministrator);
			
			RoleType sailorRole = searchRoleByName(modelPort, "Sailor");
			System.out.println("Got Sailor role");
			System.out.println(sailorRole);
	
			Collection<ResourceType> resouces = listResources(modelPort);
			System.out.println("Resources");
			System.out.println(resouces);
			
			String userGuybrushoid = createUserGuybrush(modelPort, sailorRole);
			System.out.println("Created user guybrush, OID: "+userGuybrushoid);
			
			String userLeChuckOid = createUserFromSystemResource(modelPort, "user-lechuck.xml");
			System.out.println("Created user lechuck, OID: "+userLeChuckOid);
			
			changeUserPassword(modelPort, userGuybrushoid, "MIGHTYpirate");
			System.out.println("Created user password");
			
			assignRoles(modelPort, userGuybrushoid, ROLE_PIRATE_OID, ROLE_CAPTAIN_OID);
			System.out.println("Assigned roles");
			
			unAssignRoles(modelPort, userGuybrushoid, ROLE_CAPTAIN_OID);
			System.out.println("Unassigned roles");
			
			Collection<RoleType> roles = listRequestableRoles(modelPort);
			System.out.println("Found requestable roles");
			System.out.println(roles);
			
			// Uncomment the following line if you want to see what midPoint really did
			// ... because deleting the user will delete also all the traces (except logs and audit of course).
			deleteUser(modelPort, userGuybrushoid);
			System.out.println("Deleted user");
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static SystemConfigurationType getConfiguration(ModelPortType modelPort) throws FaultMessage {

		Holder<ObjectType> objectHolder = new Holder<ObjectType>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
		
		modelPort.getObject(ModelClientUtil.getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), options,
				objectHolder, resultHolder);
		
		return (SystemConfigurationType) objectHolder.value;
	}
	
	private static Collection<ResourceType> listResources(ModelPortType modelPort) throws SAXException, IOException, FaultMessage {
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();

		modelPort.searchObjects(ModelClientUtil.getTypeQName(ResourceType.class), null, options, objectListHolder, resultHolder);
		
		ObjectListType objectList = objectListHolder.value;
		return (Collection) objectList.getObject();
	}

	private static String createUserGuybrush(ModelPortType modelPort, RoleType role) throws FaultMessage {
		Document doc = ModelClientUtil.getDocumnent();
		
		UserType user = new UserType();
		user.setName(ModelClientUtil.createPolyStringType("guybrush", doc));
		user.setFullName(ModelClientUtil.createPolyStringType("Guybrush Threepwood", doc));
		user.setGivenName(ModelClientUtil.createPolyStringType("Guybrush", doc));
		user.setFamilyName(ModelClientUtil.createPolyStringType("Threepwood", doc));
		user.setEmailAddress("guybrush@meleeisland.net");
		user.getOrganization().add(ModelClientUtil.createPolyStringType("Pirate Brethren International", doc));
		user.getOrganizationalUnit().add(ModelClientUtil.createPolyStringType("Pirate Wannabes", doc));
		user.setCredentials(ModelClientUtil.createPasswordCredentials("IwannaBEaPIRATE"));
		
		if (role != null) {
			// create user with a role assignment
			AssignmentType roleAssignment = createRoleAssignment(role.getOid());
			user.getAssignment().add(roleAssignment);
		}
		
		return createUser(modelPort, user);
	}

	private static String createUserFromSystemResource(ModelPortType modelPort, String resourcePath) throws FileNotFoundException, JAXBException, FaultMessage {
		UserType user = unmarshallResouce(resourcePath);
		
		return createUser(modelPort, user);
	}
	
	private static <T> T unmarshallFile(File file) throws JAXBException, FileNotFoundException {
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
			is = Main.class.getClassLoader().getResourceAsStream(path);
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

	private static String createUser(ModelPortType modelPort, UserType userType) throws FaultMessage {
        ObjectDeltaType.ObjectToAdd objectToAdd = new ObjectDeltaType.ObjectToAdd();
        objectToAdd.setAny(userType);

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.ADD);
        deltaType.setObjectToAdd(objectToAdd);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
		modelPort.executeChanges(deltaListType, null);
		
		throw new UnsupportedOperationException("Here we should return OID but first we have to change executeChanges method interface");
	}
	
	private static void changeUserPassword(ModelPortType modelPort, String oid, String newPassword) throws FaultMessage {
		ItemDeltaType passwordDelta = new ItemDeltaType();
		passwordDelta.setModificationType(ModificationTypeType.REPLACE);
		passwordDelta.setPath(ModelClientUtil.createItemPathType("credentials/password"));
        RawType newValue = new RawType();
        newValue.getContent().add(ModelClientUtil.toJaxbElement(ModelClientUtil.COMMON_VALUE, ModelClientUtil.createProtectedString(newPassword)));
        passwordDelta.setValue(newValue);

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(oid);
        deltaType.getItemDelta().add(passwordDelta);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        modelPort.executeChanges(deltaListType, null);
	}
	
	private static void assignRoles(ModelPortType modelPort, String userOid, String... roleOids) throws FaultMessage {
		modifyRoleAssignment(modelPort, userOid, true, roleOids);
	}
	
	private static void unAssignRoles(ModelPortType modelPort, String userOid, String... roleOids) throws FaultMessage {
		modifyRoleAssignment(modelPort, userOid, false, roleOids);
	}
	
	private static void modifyRoleAssignment(ModelPortType modelPort, String userOid, boolean isAdd, String... roleOids) throws FaultMessage {
		ItemDeltaType assignmentDelta = new ItemDeltaType();
		if (isAdd) {
			assignmentDelta.setModificationType(ModificationTypeType.ADD);
		} else {
			assignmentDelta.setModificationType(ModificationTypeType.DELETE);
		}
		RawType assignmentValue = new RawType();
		for (String roleOid: roleOids) {
			assignmentValue.getContent().add(ModelClientUtil.toJaxbElement(ModelClientUtil.COMMON_ASSIGNMENT, createRoleAssignment(roleOid)));
		}
		assignmentDelta.setValue(assignmentValue);

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(userOid);
        deltaType.getItemDelta().add(assignmentDelta);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        modelPort.executeChanges(deltaListType, null);
	}


	private static AssignmentType createRoleAssignment(String roleOid) {
		AssignmentType roleAssignment = new AssignmentType();
		ObjectReferenceType roleRef = new ObjectReferenceType();
		roleRef.setOid(roleOid);
		roleRef.setType(ModelClientUtil.getTypeQName(RoleType.class));
		roleAssignment.setTargetRef(roleRef);
		return roleAssignment;
	}

	private static UserType searchUserByName(ModelPortType modelPort, String username) throws SAXException, IOException, FaultMessage {
		// WARNING: in a real case make sure that the username is properly escaped before putting it in XML
		SearchFilterType filter = ModelClientUtil.parseSearchFilterType(
				"<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-2' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-2a' >" +
				  "<path>c:name</path>" +
				  "<value>" + username + "</value>" +
				"</equal>"
		);
		QueryType query = new QueryType();
		query.setFilter(filter);
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
		Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		
		modelPort.searchObjects(ModelClientUtil.getTypeQName(UserType.class), query, options, objectListHolder, resultHolder);
		
		ObjectListType objectList = objectListHolder.value;
		List<ObjectType> objects = objectList.getObject();
		if (objects.isEmpty()) {
			return null;
		}
		if (objects.size() == 1) {
			return (UserType) objects.get(0);
		}
		throw new IllegalStateException("Expected to find a single user with username '"+username+"' but found "+objects.size()+" users instead");
	}
	
	private static RoleType searchRoleByName(ModelPortType modelPort, String roleName) throws SAXException, IOException, FaultMessage {
		// WARNING: in a real case make sure that the username is properly escaped before putting it in XML
		SearchFilterType filter = ModelClientUtil.parseSearchFilterType(
				"<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-2' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-2a' >" +
				  "<path>c:name</path>" +
				  "<value>" + roleName + "</value>" +
				"</equal>"
		);
		QueryType query = new QueryType();
		query.setFilter(filter);
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
		Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		
		modelPort.searchObjects(ModelClientUtil.getTypeQName(RoleType.class), query, options, objectListHolder, resultHolder);
		
		ObjectListType objectList = objectListHolder.value;
		List<ObjectType> objects = objectList.getObject();
		if (objects.isEmpty()) {
			return null;
		}
		if (objects.size() == 1) {
			return (RoleType) objects.get(0);
		}
		throw new IllegalStateException("Expected to find a single role with name '"+roleName+"' but found "+objects.size()+" users instead");
	}

	private static Collection<RoleType> listRequestableRoles(ModelPortType modelPort) throws SAXException, IOException, FaultMessage {
		SearchFilterType filter = ModelClientUtil.parseSearchFilterType(
				"<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-2' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-2a' >" +
				  "<path>c:requestable</path>" +
				  "<value>true</value>" +
				"</equal>"
		);
		QueryType query = new QueryType();
		query.setFilter(filter);
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
		Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		
		modelPort.searchObjects(ModelClientUtil.getTypeQName(RoleType.class), query, options, objectListHolder, resultHolder);
		
		ObjectListType objectList = objectListHolder.value;
		return (Collection) objectList.getObject();
	}
	
	private static void deleteUser(ModelPortType modelPort, String userGuybrushoid) throws FaultMessage {
        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.DELETE);
        deltaType.setOid(userGuybrushoid);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        modelPort.executeChanges(deltaListType, null);
	}
	
	public static ModelPortType createModelPort(String[] args) {
		String endpointUrl = DEFAULT_ENDPOINT_URL;
		
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
		outProps.put(WSHandlerConstants.USER, ADM_USERNAME);
		outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
		outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordHandler.class.getName());
		
		WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
		cxfEndpoint.getOutInterceptors().add(wssOut);

		return modelPort;
	}

}
