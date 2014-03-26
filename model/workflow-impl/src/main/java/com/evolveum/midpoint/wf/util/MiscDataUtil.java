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

package com.evolveum.midpoint.wf.util;

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
import com.evolveum.midpoint.wf.WfConfiguration;
import com.evolveum.midpoint.wf.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.processes.common.StringHolder;
import com.evolveum.midpoint.wf.processors.primary.PcpProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WorkItemType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;

import org.activiti.engine.form.FormProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */

@Component
public class MiscDataUtil {

    private static final transient Trace LOGGER = TraceManager.getTrace(MiscDataUtil.class);

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private TaskManager taskManager;
    
    @Autowired
    private SecurityEnforcer securityEnforcer;

    @Autowired
    private WfConfiguration wfConfiguration;

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
        String objectXml = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED);
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
        StringHolder deltaXml = (StringHolder) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_DELTA);
        if (deltaXml == null) {
            if (mayBeNull) {
                return null;
            } else {
                throw new IllegalStateException("There's no delta in process variables");
            }
        }
        return prismContext.parsePrismPropertyRealValue(deltaXml.getValue(), ObjectDeltaType.COMPLEX_TYPE, PrismContext.LANG_XML);
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

    public boolean isCurrentUserAuthorizedToSubmit(WorkItemType workItem) {
        return isAuthorizedToSubmit(workItem.getAssigneeRef().getOid());
    }

    public boolean isAuthorizedToSubmit(String assigneeOid) {
        MidPointPrincipal principal;
		try {
			principal = securityEnforcer.getPrincipal();
		} catch (SecurityViolationException e) {
			return false;
		}
        LOGGER.trace("isAuthorizedToSubmit: principal = {}, assignee = {}", principal, assigneeOid);
        if (principal.getOid() != null && principal.getOid().equals(assigneeOid)) {
            return true;
        }
        try {
			return wfConfiguration.isAllowApproveOthersItems() 
					&& securityEnforcer.isAuthorized(AuthorizationConstants.AUTZ_UI_WORK_ITEMS_APPROVE_OTHERS_ITEMS_URL, null, null, null);
		} catch (SchemaException e) {
			throw new SystemException(e.getMessage(), e);
		}
    }

    // todo move to something activiti-related

    public static Map<String,FormProperty> formPropertiesAsMap(List<FormProperty> properties) {
        Map<String,FormProperty> retval = new HashMap<String,FormProperty>();
        for (FormProperty property : properties) {
            retval.put(property.getId(), property);
        }
        return retval;
    }

}
