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

package com.evolveum.midpoint.wf.impl.processors.primary.aspect;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.messages.ProcessEvent;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpJob;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericPcpAspectConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PcpAspectConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PrimaryChangeProcessorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;
import org.apache.velocity.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class PrimaryChangeAspectHelper {

    private static final Trace LOGGER = TraceManager.getTrace(PrimaryChangeAspectHelper.class);

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Autowired
    private PrismContext prismContext;

    //region ========================================================================== Jobs-related methods
    //endregion

    //region ========================================================================== Default implementation of aspect methods
    /**
     * Prepares deltaOut from deltaIn, based on process instance variables.
     * (Default implementation of the method from PrimaryChangeAspect.)
     *
     * In the default case, mapping deltaIn -> deltaOut is extremely simple.
     * DeltaIn contains a delta that has to be approved. Workflow answers simply yes/no.
     * Therefore, we either copy DeltaIn to DeltaOut, or generate an empty list of modifications.
     */
    public List<ObjectDelta<Objectable>> prepareDeltaOut(ProcessEvent event, PcpJob pcpJob, OperationResult result) throws SchemaException {
        List<ObjectDelta<Objectable>> deltaIn = pcpJob.retrieveDeltasToProcess();
        if (ApprovalUtils.isApproved(event.getAnswer())) {
            return new ArrayList<>(deltaIn);
        } else {
            return new ArrayList<>();
        }
    }

    //endregion

    //region ========================================================================== Miscellaneous
    public String getObjectOid(ModelContext<?> modelContext) {
        ModelElementContext<UserType> fc = (ModelElementContext<UserType>) modelContext.getFocusContext();
        String objectOid = null;
        if (fc.getObjectNew() != null && fc.getObjectNew().getOid() != null) {
            return fc.getObjectNew().getOid();
        } else if (fc.getObjectOld() != null && fc.getObjectOld().getOid() != null) {
            return fc.getObjectOld().getOid();
        } else {
            return null;
        }
    }

    public PrismObject<UserType> getRequester(Task task, OperationResult result) {
        // let's get fresh data (not the ones read on user login)
        PrismObject<UserType> requester = null;
        try {
            requester = ((PrismObject<UserType>) repositoryService.getObject(UserType.class, task.getOwner().getOid(), null, result));
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get data about task requester (" + task.getOwner() + "), because it does not exist in repository anymore. Using cached data.", e);
            requester = task.getOwner().clone();
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get data about task requester (" + task.getOwner() + "), due to schema exception. Using cached data.", e);
            requester = task.getOwner().clone();
        }

        if (requester != null) {
            resolveRolesAndOrgUnits(requester, result);
        }

        return requester;
    }

    public <T extends ObjectType> T resolveTargetRefUnchecked(AssignmentType a, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (a == null) {
            return null;
        }

        ObjectType object = a.getTarget();
        if (object == null) {
            if (a.getTargetRef() == null || a.getTargetRef().getOid() == null) {
                return null;
            }
            Class<? extends ObjectType> clazz = null;
            if (a.getTargetRef().getType() != null) {
                clazz = prismContext.getSchemaRegistry().determineCompileTimeClass(a.getTargetRef().getType());
            }
            if (clazz == null) {
                clazz = ObjectType.class;
            }
            object = repositoryService.getObject(clazz, a.getTargetRef().getOid(), null, result).asObjectable();
            a.setTarget(object);
        }
        return (T) object;
    }

    public <T extends ObjectType> T resolveTargetRef(AssignmentType a, OperationResult result) {
        try {
            return resolveTargetRefUnchecked(a, result);
        } catch (SchemaException|ObjectNotFoundException e) {
            throw new SystemException(e);
        }
    }

    public ResourceType resolveResourceRef(AssignmentType a, OperationResult result) {

        if (a == null) {
            return null;
        }

        if (a.getConstruction() == null) {
            return null;
        }

        if (a.getConstruction().getResourceRef() == null) {
            return null;
        }

        try {
            return repositoryService.getObject(ResourceType.class, a.getConstruction().getResourceRef().getOid(), null, result).asObjectable();
        } catch (ObjectNotFoundException|SchemaException e) {
            throw new SystemException(e);
        }
    }

    public <T extends ObjectType> T resolveTargetRef(ObjectReferenceType referenceType, Class<T> defaultObjectType, OperationResult result) {
        if (referenceType == null) {
            return null;
        }
        try {
            Class<? extends ObjectType> clazz = null;
            if (referenceType.getType() != null) {
                clazz = prismContext.getSchemaRegistry().determineCompileTimeClass(referenceType.getType());
            }
            if (clazz == null) {
                clazz = defaultObjectType;
            }
            return (T) repositoryService.getObject(clazz, referenceType.getOid(), null, result).asObjectable();
        } catch (ObjectNotFoundException|SchemaException e) {
            throw new SystemException(e);
        }
    }

    public void resolveRolesAndOrgUnits(PrismObject<UserType> user, OperationResult result) {
        for (AssignmentType assignmentType : user.asObjectable().getAssignment()) {
            if (assignmentType.getTargetRef() != null && assignmentType.getTarget() == null) {
                QName type = assignmentType.getTargetRef().getType();
                if (RoleType.COMPLEX_TYPE.equals(type) || OrgType.COMPLEX_TYPE.equals(type)) {
                    String oid = assignmentType.getTargetRef().getOid();
                    try {
                        PrismObject<ObjectType> o = repositoryService.getObject(ObjectType.class, oid, null, result);
                        assignmentType.setTarget(o.asObjectable());
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Resolved {} to {} in {}", new Object[]{oid, o, user});
                        }
                    } catch (ObjectNotFoundException e) {
                        LoggingUtils.logException(LOGGER, "Couldn't resolve reference to {} in {}", e, oid, user);
                    } catch (SchemaException e) {
                        LoggingUtils.logException(LOGGER, "Couldn't resolve reference to {} in {}", e, oid, user);
                    }
                }
            }
        }
    }

    public boolean isEnabled(PrimaryChangeProcessorConfigurationType processorConfigurationType, PrimaryChangeAspect aspect) {
        if (processorConfigurationType == null) {
            return aspect.isEnabledByDefault();
        }
        PcpAspectConfigurationType aspectConfigurationType = getPcpAspectConfigurationType(processorConfigurationType, aspect);     // result may be null
        return isEnabled(aspectConfigurationType, aspect.isEnabledByDefault());
    }

    public PcpAspectConfigurationType getPcpAspectConfigurationType(WfConfigurationType wfConfigurationType, PrimaryChangeAspect aspect) {
        if (wfConfigurationType == null) {
            return null;
        }
        return getPcpAspectConfigurationType(wfConfigurationType.getPrimaryChangeProcessor(), aspect);
    }

    public PcpAspectConfigurationType getPcpAspectConfigurationType(PrimaryChangeProcessorConfigurationType processorConfigurationType, PrimaryChangeAspect aspect) {
        if (processorConfigurationType == null) {
            return null;
        }
        String aspectName = aspect.getBeanName();
        String getterName = "get" + StringUtils.capitalizeFirstLetter(aspectName);
        Object aspectConfigurationObject;
        try {
            Method getter = processorConfigurationType.getClass().getDeclaredMethod(getterName);
            try {
                aspectConfigurationObject = getter.invoke(processorConfigurationType);
            } catch (IllegalAccessException|InvocationTargetException e) {
                throw new SystemException("Couldn't obtain configuration for aspect " + aspectName + " from the workflow configuration.", e);
            }
            if (aspectConfigurationObject != null) {
                return (PcpAspectConfigurationType) aspectConfigurationObject;
            }
            LOGGER.trace("Specific configuration for {} not found, trying generic configuration", aspectName);
        } catch (NoSuchMethodException e) {
            // nothing wrong with this, let's try generic configuration
            LOGGER.trace("Configuration getter method for {} not found, trying generic configuration", aspectName);
        }

        for (GenericPcpAspectConfigurationType genericConfig : processorConfigurationType.getOtherAspect()) {
            if (aspectName.equals(genericConfig.getName())) {
                return genericConfig;
            }
        }
        return null;
    }

    private boolean isEnabled(PcpAspectConfigurationType configurationType, boolean enabledByDefault) {
        if (configurationType == null) {
            return enabledByDefault;
        } else if (Boolean.FALSE.equals(configurationType.isEnabled())) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isUserRelated(ModelContext<? extends ObjectType> context) {
        return isRelatedToType(context, UserType.class);
    }

    public boolean isRelatedToType(ModelContext<? extends ObjectType> context, Class<?> type) {
        Class<? extends ObjectType> focusClass = getFocusClass(context);
        if (focusClass == null) {
            return false;
        }
        return type.isAssignableFrom(focusClass);
    }

    public Class<? extends ObjectType> getFocusClass(ModelContext<? extends ObjectType> context) {
        if (context.getFocusClass() != null) {
            return context.getFocusClass();
        }

        // if for some reason context.focusClass is not set... here is a fallback
        if (context.getFocusContext() == null) {
            return null;
        }

        ObjectDelta<? extends ObjectType> change = context.getFocusContext().getPrimaryDelta();
        if (change == null) {
            return null;
        }
        return change.getObjectTypeClass();
    }

    //endregion

    public boolean hasApproverInformation(PcpAspectConfigurationType config) {
        return config != null && (!config.getApproverRef().isEmpty() || !config.getApproverExpression().isEmpty() || config.getApprovalSchema() != null);
    }

}
