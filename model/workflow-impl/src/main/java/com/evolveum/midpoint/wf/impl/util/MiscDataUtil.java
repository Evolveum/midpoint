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

package com.evolveum.midpoint.wf.impl.util;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processes.common.StringHolder;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.task.IdentityLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */

@Component
public class MiscDataUtil {

    private static final transient Trace LOGGER = TraceManager.getTrace(MiscDataUtil.class);

    public static final String ROLE_PREFIX = "role";
    public static final String ORG_PREFIX = "org";

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private TaskManager taskManager;
    
    @Autowired
    private SecurityEnforcer securityEnforcer;

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    private ActivitiEngine activitiEngine;

    public PrismObject<UserType> getUserByOid(String oid, OperationResult result) {
        if (oid == null) {
            return null;
        }
        try {
            return repositoryService.getObject(UserType.class, oid, null, result);
        } catch (ObjectNotFoundException e) {
            // there should be a note in result by now
            LoggingUtils.logException(LOGGER, "Couldn't get user {} details because it couldn't be found", e, oid);
            return null;
        } catch (SchemaException e) {
            // there should be a note in result by now
            LoggingUtils.logException(LOGGER, "Couldn't get user {} details due to schema exception", e, oid);
            return null;
        }
    }


    // returns oid when user cannot be retrieved
    public String getUserNameByOid(String oid, OperationResult result) {
        try {
            PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, null, result);
            return user.asObjectable().getName().getOrig();
        } catch (ObjectNotFoundException e) {
            // there should be a note in result by now
            LoggingUtils.logException(LOGGER, "Couldn't get user {} details because it couldn't be found", e, oid);
            return oid;
        } catch (SchemaException e) {
            // there should be a note in result by now
            LoggingUtils.logException(LOGGER, "Couldn't get user {} details due to schema exception", e, oid);
            return oid;
        }
    }

    public PrismObject<UserType> getRequester(Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {
        String oid = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_REQUESTER_OID);
        return repositoryService.getObject(UserType.class, oid, null, result);
    }

    public PrismObject<? extends ObjectType> getObjectBefore(Map<String, Object> variables, PrismContext prismContext, OperationResult result) throws SchemaException, ObjectNotFoundException, JAXBException {
        String objectXml = (String) variables.get(PcpProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED);
        PrismObject<? extends ObjectType> object;
        if (objectXml != null) {
            object = prismContext.parseObject(objectXml, PrismContext.LANG_XML);
        } else {
            String oid = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_OID);
            if (oid == null) {
                return null;
            }
            //Validate.notNull(oid, "Object OID in process variables is null");
            object = repositoryService.getObject(ObjectType.class, oid, null, result);
        }

        if (object.asObjectable() instanceof UserType) {
            resolveAssignmentTargetReferences((PrismObject) object, result);
        }
        return object;
    }

    public ObjectDelta getObjectDelta(Map<String, Object> variables) throws JAXBException, SchemaException {
        return getObjectDelta(variables, false);
    }

    public ObjectDelta getObjectDelta(Map<String, Object> variables, boolean mayBeNull) throws JAXBException, SchemaException {
        ObjectDeltaType objectDeltaType = getObjectDeltaType(variables, mayBeNull);
        return objectDeltaType != null ? DeltaConvertor.createObjectDelta(objectDeltaType, prismContext) : null;
    }

    public ObjectDeltaType getObjectDeltaType(Map<String, Object> variables, boolean mayBeNull) throws JAXBException, SchemaException {
        StringHolder deltaXml = (StringHolder) variables.get(PcpProcessVariableNames.VARIABLE_MIDPOINT_DELTA);
        if (deltaXml == null) {
            if (mayBeNull) {
                return null;
            } else {
                throw new IllegalStateException("There's no delta in process variables");
            }
        }
        return prismContext.parseAtomicValue(deltaXml.getValue(), ObjectDeltaType.COMPLEX_TYPE, PrismContext.LANG_XML);
    }

    public PrismObject<? extends ObjectType> getObjectAfter(Map<String, Object> variables, ObjectDeltaType deltaType, PrismObject<? extends ObjectType> objectBefore, PrismContext prismContext, OperationResult result) throws JAXBException, SchemaException {

        ObjectDelta delta;
        if (deltaType != null) {
            delta = DeltaConvertor.createObjectDelta(deltaType, prismContext);
        } else {
            delta = getObjectDelta(variables, true);
        }

        if (delta == null) {
            return null;
        }

        PrismObject<? extends ObjectType> objectAfter = objectBefore.clone();
        delta.applyTo(objectAfter);

        if (objectAfter.asObjectable() instanceof UserType) {
            resolveAssignmentTargetReferences((PrismObject) objectAfter, result);
        }
        return objectAfter;
    }

    public static String serializeObjectToXml(PrismObject<? extends ObjectType> object) {
        return serializeObjectToXml(object, object.getPrismContext());
    }

    public static String serializeObjectToXml(PrismObject<? extends ObjectType> object, PrismContext prismContext) {
        try {
            return prismContext.serializeObjectToString(object, PrismContext.LANG_XML);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't serialize a PrismObject " + object + " into XML", e);
        }
    }

    public static String serializeContainerableToXml(Containerable containerable, PrismContext prismContext) {
        try {
            PrismContainerValue value = containerable.asPrismContainerValue();
            return prismContext.serializeContainerValueToString(value, value.getContainer().getElementName(), PrismContext.LANG_XML);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't serialize a Containerable " + containerable + " into XML", e);
        }
    }

    public static ObjectType deserializeObjectFromXml(String xml, PrismContext prismContext) {
        try {
            return (ObjectType) prismContext.parseObject(xml, PrismContext.LANG_XML).asObjectable();
        } catch (SchemaException e) {
            throw new SystemException("Couldn't deserialize a PrismObject from XML", e);
        }
    }

    public static PrismContainer deserializeContainerFromXml(String xml, PrismContext prismContext) {
        try {
            return prismContext.parseContainer(xml, (Class) null, PrismContext.LANG_XML);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't deserialize a Containerable from XML", e);
        }
    }

    public void resolveAssignmentTargetReferences(PrismObject<? extends UserType> object, OperationResult result) {
        for (AssignmentType assignmentType : object.asObjectable().getAssignment()) {
            if (assignmentType.getTarget() == null && assignmentType.getTargetRef() != null) {
                PrismObject<? extends ObjectType> target = null;
                try {
                    target = repositoryService.getObject(ObjectType.class, assignmentType.getTargetRef().getOid(), null, result);
                    assignmentType.setTarget(target.asObjectable());
                } catch (ObjectNotFoundException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't resolve assignment " + assignmentType, e);
                } catch (SchemaException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't resolve assignment " + assignmentType, e);
                }
            }
        }
    }

    /**
     * Retrieves focus object name from the model context.
     */
    public static String getFocusObjectName(ModelContext<? extends ObjectType> modelContext) {
        ModelElementContext<? extends ObjectType> fc = modelContext.getFocusContext();
        PrismObject<? extends ObjectType> prism = fc.getObjectNew() != null ? fc.getObjectNew() : fc.getObjectOld();
        if (prism == null) {
            throw new IllegalStateException("No object (new or old) in model context");
        }
        ObjectType object = prism.asObjectable();
        return object.getName() != null ? object.getName().getOrig() : null;
    }

    public Task getShadowTask(Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {
        String oid = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID);
        if (oid != null) {
            return taskManager.getTask(oid, result);
        } else {
            return null;
        }
    }

    public boolean isAuthorizedToSubmit(WorkItemType workItem) {
        return isAuthorizedToSubmit(workItem.getWorkItemId(),
                workItem.getAssigneeRef() != null ? workItem.getAssigneeRef().getOid() : null);
    }

    public boolean isAuthorizedToSubmit(String taskId, String assigneeOid) {
        MidPointPrincipal principal;
		try {
			principal = securityEnforcer.getPrincipal();
		} catch (SecurityViolationException e) {
			return false;
		}
        String currentUserOid = principal.getOid();
        if (currentUserOid == null) {
            return false;
        }
        LOGGER.trace("isAuthorizedToSubmit: principal = {}, assignee = {}", principal, assigneeOid);
        // 1) is this the task assignee?
        if (currentUserOid.equals(assigneeOid)) {       // assigneeOid may be null here
            return true;
        }
        // 2) is the current user allowed to approve any item?
        try {
			if (wfConfiguration.isAllowApproveOthersItems()
					&& securityEnforcer.isAuthorized(AuthorizationConstants.AUTZ_UI_WORK_ITEMS_APPROVE_OTHERS_ITEMS_URL, null, null, null, null, null)) {
                return true;
            }
		} catch (SchemaException e) {
			throw new SystemException(e.getMessage(), e);
		}
        // 3) is the current user in one of candidate users or groups?
        return isAmongCandidates(principal, taskId);
    }

    // principal != null, principal.getOid() != null, principal.getUser() != null
    private boolean isAmongCandidates(MidPointPrincipal principal, String taskId) {
        String currentUserOid = principal.getOid();
        List<IdentityLink> identityLinks;
        try {
            TaskService taskService = activitiEngine.getTaskService();
            identityLinks = taskService.getIdentityLinksForTask(taskId);
        } catch (ActivitiException e) {
            throw new SystemException("Couldn't determine user authorization, because the task candidate users and groups couldn't be retrieved: " + e.getMessage(), e);
        }
        for (IdentityLink identityLink : identityLinks) {
            if (identityLink.getUserId() != null && identityLink.getUserId().equals(currentUserOid)) {
                return true;
            }
            if (identityLink.getGroupId() != null) {
                if (isMemberOfActivitiGroup(principal.getUser(), identityLink.getGroupId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isAuthorizedToClaim(WorkItemType workItem) {
        return isAuthorizedToClaim(workItem.getWorkItemId());
    }

    public boolean isAuthorizedToClaim(String taskId) {
        MidPointPrincipal principal;
        try {
            principal = securityEnforcer.getPrincipal();
        } catch (SecurityViolationException e) {
            return false;
        }
        String currentUserOid = principal.getOid();
        if (currentUserOid == null) {
            return false;
        }
        return isAmongCandidates(principal, taskId);
    }


    // todo move to something activiti-related

    public static Map<String,FormProperty> formPropertiesAsMap(List<FormProperty> properties) {
        Map<String,FormProperty> retval = new HashMap<String,FormProperty>();
        for (FormProperty property : properties) {
            retval.put(property.getId(), property);
        }
        return retval;
    }

    // TODO: currently we check only the direct assignments, we need to implement more complex mechanism
    public boolean isMemberOfActivitiGroup(UserType userType, String activitiGroupId) {
        ObjectReferenceType abstractRoleRef = groupIdToObjectReference(activitiGroupId);
        for (AssignmentType assignmentType : userType.getAssignment()) {
            if (assignmentType.getTargetRef() != null &&
                    (RoleType.COMPLEX_TYPE.equals(assignmentType.getTargetRef().getType()) ||
                     OrgType.COMPLEX_TYPE.equals(assignmentType.getTargetRef().getType())) &&
                    abstractRoleRef.getOid().equals(assignmentType.getTargetRef().getOid())) {
                return true;
            }
        }
        return false;
    }

    // TODO: currently we check only the direct assignments, we need to implement more complex mechanism
    public List<String> getGroupsForUser(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        List<String> retval = new ArrayList<>();
        UserType userType = repositoryService.getObject(UserType.class, oid, null, result).asObjectable();
        for (AssignmentType assignmentType : userType.getAssignment()) {
            ObjectReferenceType ref = assignmentType.getTargetRef();
            if (ref != null) {
                String groupName = objectReferenceToGroupName(ref);
                if (groupName != null) {        // if the reference represents a group name (i.e. it is not e.g. an account ref)
                    retval.add(groupName);
                }
            }
        }
        return retval;
    }

    public PrismObject resolveObjectReferenceType(ObjectReferenceType ref, OperationResult result) {
        try {
            return repositoryService.getObject((Class) prismContext.getSchemaRegistry().getCompileTimeClass(ref.getType()), ref.getOid(), null, result);
        } catch (ObjectNotFoundException e) {
            // there should be a note in result by now
            LoggingUtils.logException(LOGGER, "Couldn't get reference {} details because it couldn't be found", e, ref);
            return null;
        } catch (SchemaException e) {
            // there should be a note in result by now
            LoggingUtils.logException(LOGGER, "Couldn't get reference {} details due to schema exception", e, ref);
            return null;
        }
    }

    public ObjectReferenceType groupIdToObjectReference(String groupId) {
        String parts[] = groupId.split(":");
        if (parts.length != 2) {
            throw new IllegalStateException("Invalid format of group id: " + groupId);
        }
        if (ROLE_PREFIX.equals(parts[0])) {
            return MiscSchemaUtil.createObjectReference(parts[1], RoleType.COMPLEX_TYPE);
        } else if (ORG_PREFIX.equals(parts[0])) {
            return MiscSchemaUtil.createObjectReference(parts[1], OrgType.COMPLEX_TYPE);
        } else {
            throw new IllegalStateException("Unknown kind of group id: " + parts[0] + " in: " + groupId);
        }
    }

    private String objectReferenceToGroupName(ObjectReferenceType ref) {
        if (RoleType.COMPLEX_TYPE.equals(ref.getType())) {
            return ROLE_PREFIX + ":" + ref.getOid();
        } else if (OrgType.COMPLEX_TYPE.equals(ref.getType())) {
            return ORG_PREFIX + ":" + ref.getOid();
        } else {
            return null;
        }
    }

}
