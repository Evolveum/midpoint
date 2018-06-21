/*
 * Copyright (c) 2010-2018 Evolveum
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
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GetOperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OptionObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RetrieveOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelService;
import com.evolveum.prism.xml.ns._public.query_3.ObjectFactory;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.PropertyComplexValueFilterClauseType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.WSConstants;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.dom.DOMResult;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author semancik
 *
 * Prerequisites:
 * 1. OpenDJ resource, Pirate and Captain roles should exist.
 * 2. Users lechuck and guybrush should NOT exist.
 *
 */
public class Main {

	// Configuration
	public static final String ADM_USERNAME = "administrator";
	public static final String ADM_PASSWORD = "5ecr3t";
	private static final String DEFAULT_ENDPOINT_URL = "http://localhost:8080/midpoint/model/model-3";

	// Object OIDs
	private static final String ROLE_PIRATE_OID = "2de6a600-636f-11e4-9cc7-3c970e467874";
	private static final String ROLE_CAPTAIN_OID = "12345678-d34d-b33f-f00d-987987cccccc";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {

			ModelPortType modelPort = createModelPort(args);

			SystemConfigurationType configurationType = getConfiguration(modelPort);
			System.out.println("Got system configuration");
//			System.out.println(configurationType);

			UserType userAdministrator = searchUserByName(modelPort, "administrator");
			System.out.println("Got administrator user: "+userAdministrator.getOid());
//			System.out.println(userAdministrator);

			RoleType sailorRole = searchRoleByName(modelPort, "Sailor");
			System.out.println("Got Sailor role");
//			System.out.println(sailorRole);

			Collection<ResourceType> resources = listResources(modelPort);
			System.out.println("Resources ("+resources.size()+")");
//			dump(resources);

            Collection<UserType> users = listUsers(modelPort);
            System.out.println("Users ("+users.size()+")");
//            dump(users);

            Collection<TaskType> tasks = listTasks(modelPort);
            System.out.println("Tasks ("+tasks.size()+")");
//            dump(tasks);
//            System.out.println("Next scheduled times: ");
//            for (TaskType taskType : tasks) {
//                System.out.println(" - " + getOrig(taskType.getName()) + ": " + taskType.getNextRunStartTimestamp());
//            }

            String userGuybrushoid = createUserGuybrush(modelPort, sailorRole);
			System.out.println("Created user guybrush, OID: "+userGuybrushoid);

			UserType userGuybrush = getUser(modelPort, userGuybrushoid);
			System.out.println("Fetched user guybrush:");
//			System.out.println(userGuybrush);
			System.out.println("Users fullName: " + ModelClientUtil.getOrig(userGuybrush.getFullName()));

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
			System.out.println("Found "+roles.size()+" requestable roles");
//			System.out.println(roles);

            String seaSuperuserRole = createRoleFromSystemResource(modelPort, "role-sea-superuser.xml");
            System.out.println("Created role Sea Superuser, OID: " + seaSuperuserRole);

            assignRoles(modelPort, userLeChuckOid, seaSuperuserRole);
            System.out.println("Assigned role Sea Superuser to LeChuck");

            modifyRoleModifyInducement(modelPort, seaSuperuserRole);
            System.out.println("Modified role Sea Superuser - modified resource inducement");

            modifyRoleReplaceInducement(modelPort, seaSuperuserRole, 2, ROLE_CAPTAIN_OID);
            System.out.println("Modified role Sea Superuser - changed role inducement");

            reconcileUser(modelPort, userLeChuckOid);
            System.out.println("LeChuck reconciled.");

			// Uncomment the following lines if you want to see what midPoint really did
			// ... because deleting the user will delete also all the traces (except logs and audit of course).
			deleteUser(modelPort, userGuybrushoid);
            deleteUser(modelPort, userLeChuckOid);
            deleteRole(modelPort, seaSuperuserRole);
			System.out.println("Deleted user(s)");

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

    private static void dump(Collection<? extends ObjectType> objects) {
        System.out.println("Objects returned: " + objects.size());
        for (ObjectType objectType : objects) {
            System.out.println(" - " + ModelClientUtil.getOrig(objectType.getName()) + ": " + objectType);
        }
    }

    private static SystemConfigurationType getConfiguration(ModelPortType modelPort) throws FaultMessage {

		Holder<ObjectType> objectHolder = new Holder<>();
		Holder<OperationResultType> resultHolder = new Holder<>();
		SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();

		modelPort.getObject(ModelClientUtil.getTypeQName(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(), options,
                objectHolder, resultHolder);

		return (SystemConfigurationType) objectHolder.value;
	}

    private static UserType getUser(ModelPortType modelPort, String oid) throws FaultMessage {

		Holder<ObjectType> objectHolder = new Holder<>();
		Holder<OperationResultType> resultHolder = new Holder<>();
		SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();

		modelPort.getObject(ModelClientUtil.getTypeQName(UserType.class), oid, options,
				objectHolder, resultHolder);

		return (UserType) objectHolder.value;
	}

	private static Collection<ResourceType> listResources(ModelPortType modelPort) throws SAXException, IOException, FaultMessage {
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        Holder<ObjectListType> objectListHolder = new Holder<>();
		Holder<OperationResultType> resultHolder = new Holder<>();

		modelPort.searchObjects(ModelClientUtil.getTypeQName(ResourceType.class), null, options, objectListHolder, resultHolder);

		ObjectListType objectList = objectListHolder.value;
		return (Collection) objectList.getObject();
	}

    private static Collection<UserType> listUsers(ModelPortType modelPort) throws SAXException, IOException, FaultMessage {
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        Holder<ObjectListType> objectListHolder = new Holder<>();
        Holder<OperationResultType> resultHolder = new Holder<>();

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
        OptionObjectSelectorType selector = new OptionObjectSelectorType();
        selector.setPath(ModelClientUtil.createItemPathType("nextRunStartTimestamp"));
        getNextScheduledTimeOption.setSelector(selector);
        GetOperationOptionsType selectorOptions = new GetOperationOptionsType();
        selectorOptions.setRetrieve(RetrieveOptionType.INCLUDE);
        getNextScheduledTimeOption.setOptions(selectorOptions);

        // add newly created option to the list of operation options
        operationOptions.getOption().add(getNextScheduledTimeOption);

        Holder<ObjectListType> objectListHolder = new Holder<>();
        Holder<OperationResultType> resultHolder = new Holder<>();

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
			AssignmentType roleAssignment = ModelClientUtil.createRoleAssignment(role.getOid());
			user.getAssignment().add(roleAssignment);
		}

		return createUser(modelPort, user);
	}

	private static String createUserFromSystemResource(ModelPortType modelPort, String resourcePath) throws FileNotFoundException, JAXBException, FaultMessage {
		UserType user = ModelClientUtil.unmarshallResource(resourcePath);

		return createUser(modelPort, user);
	}

    private static String createRoleFromSystemResource(ModelPortType modelPort, String resourcePath) throws FileNotFoundException, JAXBException, FaultMessage {
        RoleType role = ModelClientUtil.unmarshallResource(resourcePath);

        return createRole(modelPort, role);
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

    private static String createRole(ModelPortType modelPort, RoleType roleType) throws FaultMessage {
        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(RoleType.class));
        deltaType.setChangeType(ChangeTypeType.ADD);
        deltaType.setObjectToAdd(roleType);

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

    private static void reconcileUser(ModelPortType modelPort, String oid) throws FaultMessage {
        Document doc = ModelClientUtil.getDocumnent();

        ObjectDeltaType userDelta = new ObjectDeltaType();
        userDelta.setOid(oid);
        userDelta.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        userDelta.setChangeType(ChangeTypeType.MODIFY);

        ObjectDeltaListType deltaList = new ObjectDeltaListType();
        deltaList.getDelta().add(userDelta);

        ModelExecuteOptionsType optionsType = new ModelExecuteOptionsType();
        optionsType.setReconcile(true);
        modelPort.executeChanges(deltaList, optionsType);
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
			assignmentDelta.getValue().add(ModelClientUtil.createRoleAssignment(roleOid));
		}

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(userOid);
        deltaType.getItemDelta().add(assignmentDelta);

        ObjectDeltaOperationListType objectDeltaOperationList = modelPort.executeChanges(ModelClientUtil.createDeltaList(deltaType), null);
        for (ObjectDeltaOperationType objectDeltaOperation : objectDeltaOperationList.getDeltaOperation()) {
            if (!OperationResultStatusType.SUCCESS.equals(objectDeltaOperation.getExecutionResult().getStatus())) {
                System.out.println("*** Operation result = " + objectDeltaOperation.getExecutionResult().getStatus() + ": " + objectDeltaOperation.getExecutionResult().getMessage());
            }
        }
	}

    private static void modifyRoleModifyInducement(ModelPortType modelPort, String roleOid) throws IOException, SAXException, FaultMessage {
        ItemDeltaType inducementDelta = new ItemDeltaType();
        inducementDelta.setModificationType(ModificationTypeType.ADD);
        inducementDelta.setPath(ModelClientUtil.createItemPathType("inducement[3]/construction/attribute"));
        inducementDelta.getValue().add(ModelClientUtil.parseElement("<value>\n" +
                "        <ref xmlns:ri=\"http://midpoint.evolveum.com/xml/ns/public/resource/instance-3\">ri:pager</ref>\n" +
                "        <outbound>\n" +
                "            <expression>\n" +
                "                <value>00-000-001</value>\n" +
                "                <value>00-000-003</value>\n" +
                "            </expression>\n" +
                "        </outbound>\n" +
                "    </value>"));

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(RoleType.class));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(roleOid);
        deltaType.getItemDelta().add(inducementDelta);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        ObjectDeltaOperationListType objectDeltaOperationList = modelPort.executeChanges(deltaListType, null);
        for (ObjectDeltaOperationType objectDeltaOperation : objectDeltaOperationList.getDeltaOperation()) {
            if (!OperationResultStatusType.SUCCESS.equals(objectDeltaOperation.getExecutionResult().getStatus())) {
                System.out.println("*** Operation result = " + objectDeltaOperation.getExecutionResult().getStatus() + ": " + objectDeltaOperation.getExecutionResult().getMessage());
            }
        }
    }


    // removes inducement with a given ID and replaces it with a new one
    private static void modifyRoleReplaceInducement(ModelPortType modelPort, String roleOid, int oldId, String newInducementOid) throws FaultMessage, IOException, SAXException {

        ItemDeltaType inducementDeleteDelta = new ItemDeltaType();
        inducementDeleteDelta.setModificationType(ModificationTypeType.DELETE);
        inducementDeleteDelta.setPath(ModelClientUtil.createItemPathType("inducement"));
        inducementDeleteDelta.getValue().add(ModelClientUtil.parseElement("<value><id>"+oldId+"</id></value>"));

        ItemDeltaType inducementAddDelta = new ItemDeltaType();
        inducementAddDelta.setModificationType(ModificationTypeType.ADD);
        inducementAddDelta.setPath(ModelClientUtil.createItemPathType("inducement"));
        inducementAddDelta.getValue().add(ModelClientUtil.createRoleAssignment(newInducementOid));

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(RoleType.class));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(roleOid);
        deltaType.getItemDelta().add(inducementDeleteDelta);
        deltaType.getItemDelta().add(inducementAddDelta);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
        ObjectDeltaOperationListType objectDeltaOperationList = modelPort.executeChanges(deltaListType, null);
        for (ObjectDeltaOperationType objectDeltaOperation : objectDeltaOperationList.getDeltaOperation()) {
            if (!OperationResultStatusType.SUCCESS.equals(objectDeltaOperation.getExecutionResult().getStatus())) {
                System.out.println("*** Operation result = " + objectDeltaOperation.getExecutionResult().getStatus() + ": " + objectDeltaOperation.getExecutionResult().getMessage());
            }
        }
    }

    private static QueryType createUserQuery1(String username) throws JAXBException, SAXException, IOException {
        // WARNING: in a real case make sure that the username is properly escaped before putting it in XML
        SearchFilterType filter = ModelClientUtil.parseSearchFilterType(
                "<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-3' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-3' >" +
                        "<path>c:name</path>" +
                        "<value>" + username + "</value>" +
                        "</equal>"
        );
        QueryType query = new QueryType();
        query.setFilter(filter);
        return query;
    }

    private static QueryType createUserQuery2(String username) throws JAXBException {
        QueryType query = new QueryType();

        SearchFilterType filter = new SearchFilterType();

        PropertyComplexValueFilterClauseType fc = new PropertyComplexValueFilterClauseType();
        ItemPathType path = new ItemPathType();
        path.setValue("declare namespace c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\"; c:name");
        fc.setPath(path);
        fc.getValue().add(username);

        ObjectFactory factory = new ObjectFactory();
        JAXBElement<PropertyComplexValueFilterClauseType> equal = factory.createEqual(fc);

        JAXBContext jaxbContext = JAXBContext.newInstance("com.evolveum.midpoint.xml.ns._public.common.api_types_3:" +
                "com.evolveum.midpoint.xml.ns._public.common.common_3:" +
                "com.evolveum.prism.xml.ns._public.annotation_3:" +
                "com.evolveum.prism.xml.ns._public.query_3:" +
                "com.evolveum.prism.xml.ns._public.types_3:");
        Marshaller marshaller = jaxbContext.createMarshaller();
        DOMResult result = new DOMResult();
        marshaller.marshal(equal, result);
        filter.setFilterClause(((Document) result.getNode()).getDocumentElement());

        query.setFilter(filter);
        return query;
    }

	private static UserType searchUserByName(ModelPortType modelPort, String username) throws SAXException, IOException, FaultMessage, JAXBException {

        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
		Holder<ObjectListType> objectListHolder = new Holder<>();
		Holder<OperationResultType> resultHolder = new Holder<>();

		modelPort.searchObjects(ModelClientUtil.getTypeQName(UserType.class), createUserQuery1(username), options, objectListHolder, resultHolder);

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
		Holder<ObjectListType> objectListHolder = new Holder<>();
		Holder<OperationResultType> resultHolder = new Holder<>();

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
		Holder<ObjectListType> objectListHolder = new Holder<>();
		Holder<OperationResultType> resultHolder = new Holder<>();

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

    private static void deleteRole(ModelPortType modelPort, String oid) throws FaultMessage {
        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(RoleType.class));
        deltaType.setChangeType(ChangeTypeType.DELETE);
        deltaType.setOid(oid);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);

        ModelExecuteOptionsType executeOptionsType = new ModelExecuteOptionsType();
        executeOptionsType.setRaw(true);
        modelPort.executeChanges(deltaListType, executeOptionsType);
    }

    private static void deleteTask(ModelPortType modelPort, String oid) throws FaultMessage {
        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(TaskType.class));
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

		Map<String,Object> outProps = new HashMap<>();

		outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
		outProps.put(WSHandlerConstants.USER, ADM_USERNAME);
		outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
		outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordHandler.class.getName());

		WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
		cxfEndpoint.getOutInterceptors().add(wssOut);
        // enable the following to get client-side logging of outgoing requests and incoming responses
        cxfEndpoint.getOutInterceptors().add(new LoggingOutInterceptor());
        cxfEndpoint.getInInterceptors().add(new LoggingInInterceptor());

		return modelPort;
	}

}
