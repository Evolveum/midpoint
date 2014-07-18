/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.model.client.ModelClientUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.GetOperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.RetrieveOptionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SelectorQualifiedGetOptionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelService;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.WSConstants;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import com.evolveum.midpoint.util.JAXBUtil;
//import com.evolveum.midpoint.util.QNameUtil;

/**
 * @author semancik
 *
 */
public class Main {
	
	// Configuration
	public static final String ADM_USERNAME = "administrator";
	public static final String ADM_PASSWORD = "5ecr3t";
	private static final String DEFAULT_ENDPOINT_URL = "http://localhost.:8080/midpoint/model/model-3";
	
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
	
			Collection<ResourceType> resources = listResources(modelPort);
			System.out.println("Resources");
			dump(resources);

            Collection<UserType> users = listUsers(modelPort);
            System.out.println("Users");
            dump(users);

            Collection<TaskType> tasks = listTasks(modelPort);
            System.out.println("Tasks");
            dump(tasks);
            System.out.println("Next scheduled times: ");
            for (TaskType taskType : tasks) {
                System.out.println(" - " + getOrig(taskType.getName()) + ": " + taskType.getNextRunStartTimestamp());
            }

            String userGuybrushoid = createUserGuybrush(modelPort, sailorRole);
			System.out.println("Created user guybrush, OID: "+userGuybrushoid);
			
			String userLeChuckOid = createUserFromSystemResource(modelPort, "user-lechuck.xml");
			System.out.println("Created user lechuck, OID: "+userLeChuckOid);
			
			changeUserPassword(modelPort, userGuybrushoid, "MIGHTYpirate");
			System.out.println("Changed user password");

            changeUserGivenName(modelPort, userLeChuckOid, "CHUCK");
            System.out.println("Changed user given name");
			
			assignRoles(modelPort, userGuybrushoid, ROLE_PIRATE_OID, ROLE_CAPTAIN_OID);
			System.out.println("Assigned roles");
			
			unAssignRoles(modelPort, userGuybrushoid, ROLE_CAPTAIN_OID);
			System.out.println("Unassigned roles");
			
			Collection<RoleType> roles = listRequestableRoles(modelPort);
			System.out.println("Found requestable roles");
			System.out.println(roles);
			
			// Uncomment the following lines if you want to see what midPoint really did
			// ... because deleting the user will delete also all the traces (except logs and audit of course).
			deleteUser(modelPort, userGuybrushoid);
            deleteUser(modelPort, userLeChuckOid);
			System.out.println("Deleted user(s)");
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

    // TODO move to ModelClientUtil
    private static String getOrig(PolyStringType polyStringType) {
        if (polyStringType == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object o : polyStringType.getContent()) {
            if (o instanceof String) {
                sb.append(o);
            } else if (o instanceof Element) {
                Element e = (Element) o;
                if ("orig".equals(e.getLocalName())) {
                    return e.getTextContent();
                }
            } else if (o instanceof JAXBElement) {
                JAXBElement je = (JAXBElement) o;
                if ("orig".equals(je.getName().getLocalPart())) {
                    return (String) je.getValue();
                }
            }
        }
        return sb.toString();
    }

    private static void dump(Collection<? extends ObjectType> objects) {
        System.out.println("Objects returned: " + objects.size());
        for (ObjectType objectType : objects) {
            System.out.println(" - " + getOrig(objectType.getName()) + ": " + objectType);
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

    private static Collection<UserType> listUsers(ModelPortType modelPort) throws SAXException, IOException, FaultMessage {
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();

        // let's say we want to get first 3 users, sorted alphabetically by user name
        QueryType queryType = new QueryType();          // holds search query + paging options
        PagingType pagingType = new PagingType();
        pagingType.setMaxSize(3);
        pagingType.setOrderBy(ModelClientUtil.createItemPathType("name"));
        pagingType.setOrderDirection(OrderDirectionType.ASCENDING);
        queryType.setPaging(pagingType);

        modelPort.searchObjects(ModelClientUtil.getTypeQName(UserType.class), queryType, options, objectListHolder, resultHolder);

        ObjectListType objectList = objectListHolder.value;
        return (Collection) objectList.getObject();
    }

    private static Collection<TaskType> listTasks(ModelPortType modelPort) throws SAXException, IOException, FaultMessage {
        SelectorQualifiedGetOptionsType operationOptions = new SelectorQualifiedGetOptionsType();

        // Let's say we want to retrieve tasks' next scheduled time (because this may be a costly operation if
        // JDBC based quartz scheduler is used, the fetching of this attribute has to be explicitly requested)
        SelectorQualifiedGetOptionType getNextScheduledTimeOption = new SelectorQualifiedGetOptionType();

        // prepare a selector (described by path) + options (saying to retrieve that attribute)
        ObjectSelectorType selector = new ObjectSelectorType();
        selector.setPath(ModelClientUtil.createItemPathType("nextRunStartTimestamp"));
        getNextScheduledTimeOption.setSelector(selector);
        GetOperationOptionsType selectorOptions = new GetOperationOptionsType();
        selectorOptions.setRetrieve(RetrieveOptionType.INCLUDE);
        getNextScheduledTimeOption.setOptions(selectorOptions);

        // add newly created option to the list of operation options
        operationOptions.getOption().add(getNextScheduledTimeOption);

        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();

        modelPort.searchObjects(ModelClientUtil.getTypeQName(TaskType.class), null, operationOptions, objectListHolder, resultHolder);

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
		UserType user = unmarshallResource(resourcePath);
		
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
	
	private static <T> T unmarshallResource(String path) throws JAXBException, FileNotFoundException {
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
        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.ADD);
        deltaType.setObjectToAdd(userType);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
		ObjectDeltaOperationListType operationListType = modelPort.executeChanges(deltaListType, null);
		return ModelClientUtil.getOidFromDeltaOperationList(operationListType, deltaType);
	}
	
	private static void changeUserPassword(ModelPortType modelPort, String oid, String newPassword) throws FaultMessage {
		ItemDeltaType passwordDelta = new ItemDeltaType();
		passwordDelta.setModificationType(ModificationTypeType.REPLACE);
		passwordDelta.setPath(ModelClientUtil.createItemPathType("credentials/password/value"));
        passwordDelta.getValue().add(ModelClientUtil.createProtectedString(newPassword));

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(oid);
        deltaType.getItemDelta().add(passwordDelta);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        modelPort.executeChanges(deltaListType, null);
	}

    private static void changeUserGivenName(ModelPortType modelPort, String oid, String newValue) throws FaultMessage {
        Document doc = ModelClientUtil.getDocumnent();

        ObjectDeltaType userDelta = new ObjectDeltaType();
        userDelta.setOid(oid);
        userDelta.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        userDelta.setChangeType(ChangeTypeType.MODIFY);

        ItemDeltaType itemDelta = new ItemDeltaType();
        itemDelta.setModificationType(ModificationTypeType.REPLACE);
        itemDelta.setPath(ModelClientUtil.createItemPathType("givenName"));
        itemDelta.getValue().add(ModelClientUtil.createPolyStringType(newValue, doc));
        userDelta.getItemDelta().add(itemDelta);
        ObjectDeltaListType deltaList = new ObjectDeltaListType();
        deltaList.getDelta().add(userDelta);
        modelPort.executeChanges(deltaList, null);
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
        assignmentDelta.setPath(ModelClientUtil.createItemPathType("assignment"));
		for (String roleOid: roleOids) {
			assignmentDelta.getValue().add(createRoleAssignment(roleOid));
		}

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(userOid);
        deltaType.getItemDelta().add(assignmentDelta);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        ObjectDeltaOperationListType objectDeltaOperationList = modelPort.executeChanges(deltaListType, null);
        for (ObjectDeltaOperationType objectDeltaOperation : objectDeltaOperationList.getDeltaOperation()) {
            if (!OperationResultStatusType.SUCCESS.equals(objectDeltaOperation.getExecutionResult().getStatus())) {
                System.out.println("*** Operation result = " + objectDeltaOperation.getExecutionResult().getStatus() + ": " + objectDeltaOperation.getExecutionResult().getMessage());
            }
        }
	}

	private static AssignmentType createRoleAssignment(String roleOid) {
		AssignmentType roleAssignment = new AssignmentType();
		ObjectReferenceType roleRef = new ObjectReferenceType();
		roleRef.setOid(roleOid);
		roleRef.setType(ModelClientUtil.getTypeQName(RoleType.class));
		roleAssignment.setTargetRef(roleRef);
		return roleAssignment;
	}

	private static UserType searchUserByName(ModelPortType modelPort, String username) throws SAXException, IOException, FaultMessage, JAXBException {
		// WARNING: in a real case make sure that the username is properly escaped before putting it in XML
		SearchFilterType filter = ModelClientUtil.parseSearchFilterType(
				"<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-3' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-3' >" +
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
	
	private static RoleType searchRoleByName(ModelPortType modelPort, String roleName) throws SAXException, IOException, FaultMessage, JAXBException {
		// WARNING: in a real case make sure that the role name is properly escaped before putting it in XML
		SearchFilterType filter = ModelClientUtil.parseSearchFilterType(
				"<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-3' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-3' >" +
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

	private static Collection<RoleType> listRequestableRoles(ModelPortType modelPort) throws SAXException, IOException, FaultMessage, JAXBException {
		SearchFilterType filter = ModelClientUtil.parseSearchFilterType(
				"<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-3' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-3' >" +
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
	
	private static void deleteUser(ModelPortType modelPort, String oid) throws FaultMessage {
        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.DELETE);
        deltaType.setOid(oid);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);

        ModelExecuteOptionsType executeOptionsType = new ModelExecuteOptionsType();
        executeOptionsType.setRaw(true);
        modelPort.executeChanges(deltaListType, executeOptionsType);
	}
	
	public static ModelPortType createModelPort(String[] args) {
		String endpointUrl = DEFAULT_ENDPOINT_URL;
		
		if (args.length > 0) {
			endpointUrl = args[0];
		}

		System.out.println("Endpoint URL: "+endpointUrl);

        // uncomment this if you want to use Fiddler or any other proxy
        //ProxySelector.setDefault(new MyProxySelector("127.0.0.1", 8888));
		
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
        // enable the following to get client-side logging of outgoing requests and incoming responses
        //cxfEndpoint.getOutInterceptors().add(new LoggingOutInterceptor());
        //cxfEndpoint.getInInterceptors().add(new LoggingInInterceptor());

		return modelPort;
	}

}
