/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.messages.ProcessEvent;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpWfTask;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.velocity.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

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

    @Autowired
    private ExpressionFactory expressionFactory;

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
    public ObjectTreeDeltas prepareDeltaOut(ProcessEvent event, PcpWfTask pcpJob, OperationResult result) throws SchemaException {
        ObjectTreeDeltas deltaIn = pcpJob.retrieveDeltasToProcess();
        if (ApprovalUtils.isApprovedFromUri(event.getOutcome())) {
            return deltaIn;
        } else {
            return null;
        }
    }

    //endregion

    public ShadowType resolveTargetUnchecked(ShadowAssociationType association, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (association == null) {
            return null;
        }

        ObjectReferenceType shadowRef = association.getShadowRef();
        if (shadowRef == null || shadowRef.getOid() == null) {
            throw new IllegalStateException("None or null-OID shadowRef in " + association);
        }
        PrismObject<ShadowType> shadow = shadowRef.asReferenceValue().getObject();
        if (shadow == null) {
            Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
                    GetOperationOptions.createNoFetch());
            shadow = repositoryService.getObject(ShadowType.class, shadowRef.getOid(), options, result);
            shadowRef.asReferenceValue().setObject(shadow);
        }
        return shadow.asObjectable();
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
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve reference to {} in {}", e, oid, user);
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

    public boolean hasApproverInformation(PcpAspectConfigurationType config) {
        return config != null && (!config.getApproverRef().isEmpty() || !config.getApproverExpression().isEmpty() || config.getApprovalSchema() != null);
    }

    //endregion

    //region ========================================================================== Expression evaluation

    public boolean evaluateApplicabilityCondition(PcpAspectConfigurationType config, ModelContext modelContext, Serializable itemToApprove,
                                                  ExpressionVariables additionalVariables, PrimaryChangeAspect aspect, Task task, OperationResult result) {

        if (config == null || config.getApplicabilityCondition() == null) {
            return true;
        }

        ExpressionType expressionType = config.getApplicabilityCondition();

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<Boolean> resultDef = new PrismPropertyDefinitionImpl(resultName, DOMUtil.XSD_BOOLEAN, prismContext);

        ExpressionVariables expressionVariables = new ExpressionVariables();
        expressionVariables.addVariableDefinition(SchemaConstants.C_MODEL_CONTEXT, modelContext);
        expressionVariables.addVariableDefinition(SchemaConstants.C_ITEM_TO_APPROVE, itemToApprove);
        if (additionalVariables != null) {
            expressionVariables.addVariableDefinitions(additionalVariables);
        }

        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> exprResultTriple;
        try {
            Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression =
                    expressionFactory.makeExpression(expressionType, resultDef,
                            "applicability condition expression", task, result);
            ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables,
                    "applicability condition expression", task, result);

			exprResultTriple = ModelExpressionThreadLocalHolder.evaluateExpressionInContext(expression, params, task, result);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | RuntimeException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            // TODO report as a specific exception?
            throw new SystemException("Couldn't evaluate applicability condition in aspect "
                    + aspect.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }

        Collection<PrismPropertyValue<Boolean>> exprResult = exprResultTriple.getZeroSet();
        if (exprResult.size() == 0) {
            return false;
        } else if (exprResult.size() > 1) {
            throw new IllegalStateException("Applicability condition expression should return exactly one boolean value; it returned " + exprResult.size() + " ones");
        }
        Boolean boolResult = exprResult.iterator().next().getValue();
        return boolResult != null ? boolResult : false;
    }

    //endregion

}
