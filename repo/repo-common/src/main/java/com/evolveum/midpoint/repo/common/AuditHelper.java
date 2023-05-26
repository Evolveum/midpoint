/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common;

import static java.util.Collections.emptyList;

import static com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil.resolveNames;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil;
import com.evolveum.midpoint.task.api.ExpressionEnvironment;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Uses cache repository service to resolve object names.
 */
@Component
public class AuditHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AuditHelper.class);

    @Autowired private AuditService auditService;
    @Autowired private PrismContext prismContext;
    @Autowired private SchemaService schemaService;
    @Autowired private ExpressionFactory expressionFactory;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    private static final String DOT_CLASS = AuditHelper.class.getName() + ".";
    private static final String OP_AUDIT = DOT_CLASS + "audit";
    private static final String OP_RESOLVE_NAME = DOT_CLASS + "resolveName";
    private static final String OP_EVALUATE_AUDIT_RECORD_PROPERTY = DOT_CLASS + "evaluateAuditRecordProperty";
    private static final String OP_EVALUATE_RECORDING_SCRIPT = DOT_CLASS + "evaluateRecordingScript";

    /**
     * @param externalNameResolver Name resolver that should be tried first. It should be fast.
     * If it returns null it means "I don't know".
     */
    public void audit(AuditEventRecord record, ObjectDeltaSchemaLevelUtil.NameResolver externalNameResolver, Task task,
            OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_AUDIT)
                .operationKind(OperationKindType.MODEL_AUDIT)
                .setMinor()
                .addArbitraryObjectAsParam("stage", record.getEventStage())
                .addArbitraryObjectAsParam("eventType", record.getEventType())
                .build();

        try {
            LOGGER.trace("Auditing the record:\n{}", record.debugDumpLazily());
            resolveNamesInDeltas(record, externalNameResolver, result);
            auditService.audit(record, task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            if (record.getTargetRef() != null) {
                result.addParam("targetOid", record.getTargetRef().getOid());
                result.addParam("targetName", record.getTargetRef().getTargetName());
            }
            result.computeStatusIfUnknown();
        }
    }

    private void resolveNamesInDeltas(
            AuditEventRecord record,
            ObjectDeltaSchemaLevelUtil.NameResolver externalNameResolver,
            OperationResult parentResult) {
        for (ObjectDeltaOperation<? extends ObjectType> objectDeltaOperation : emptyIfNull(record.getDeltas())) {
            ObjectDelta<? extends ObjectType> delta = objectDeltaOperation.getObjectDelta();
            ObjectDeltaSchemaLevelUtil.NameResolver nameResolver = (objectClass, oid) -> {
                OperationResult result = parentResult.subresult(OP_RESOLVE_NAME)
                        .setMinor()
                        .build();
                try {
                    if (record.getNonExistingReferencedObjects().contains(oid)) {
                        // This information could come from upper layers (not now, but maybe in the future).
                        return null;
                    }
                    if (externalNameResolver != null) {
                        PolyString externallyResolvedName = externalNameResolver.getName(objectClass, oid);
                        if (externallyResolvedName != null) {
                            return externallyResolvedName;
                        }
                    }
                    // we use only cache-compatible options here, in order to utilize the local or global repository cache
                    Collection<SelectorOptions<GetOperationOptions>> options =
                            schemaService.getOperationOptionsBuilder()
                                    .readOnly()
                                    .allowNotFound()
                                    .build();
                    PrismObject<? extends ObjectType> object = repositoryService.getObject(objectClass, oid, options, result);
                    return object.getName();
                } catch (ObjectNotFoundException e) {
                    record.addNonExistingReferencedObject(oid);
                    return null;        // we will NOT record an error here
                } catch (Throwable t) {
                    result.recordFatalError(t);
                    throw t;
                } finally {
                    result.computeStatusIfUnknown();
                }
            };
            resolveNames(delta, nameResolver, prismContext);
        }
    }

    public AuditEventRecord evaluateRecordingExpression(ExpressionType expression, AuditEventRecord auditRecord,
            PrismObject<? extends ObjectType> primaryObject, ExpressionProfile expressionProfile,
            Supplier<ExpressionEnvironment> expressionEnvironmentSupplier, Task task, OperationResult parentResult) {

        OperationResult result = parentResult.createMinorSubresult(OP_EVALUATE_RECORDING_SCRIPT);

        try {
            VariablesMap variables = new VariablesMap();
            variables.put(ExpressionConstants.VAR_TARGET, primaryObject, PrismObject.class);
            variables.put(ExpressionConstants.VAR_AUDIT_RECORD, auditRecord, AuditEventRecord.class);

            if (expressionEnvironmentSupplier != null) {
                ExpressionEnvironment env = expressionEnvironmentSupplier.get();
                ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(env);
            }

            try {
                PrismValue returnValue = ExpressionUtil.evaluateExpression(
                        variables,
                        null,
                        expression, expressionProfile,
                        expressionFactory,
                        OP_EVALUATE_RECORDING_SCRIPT,
                        task,
                        result
                );

                return returnValue != null
                        ? (AuditEventRecord) returnValue.getRealValue()
                        : null;
            } finally {
                ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't evaluate audit recording expression", t);
            // Copied from evaluateAuditRecordProperty: Intentionally not throwing the exception. The error is marked as partial.
            // (It would be better to mark it as fatal and to derive overall result as partial, but we aren't that far yet.)
            result.recordPartialError(t);
        } finally {
            result.recordSuccessIfUnknown();
        }

        // In case of failure we want to return original auditRecord, although it might be
        // modified by some part of the script too - this we have to suffer.
        return auditRecord;
    }

    public void evaluateAuditRecordProperty(SystemConfigurationAuditEventRecordingPropertyType propertyDef,
            AuditEventRecord auditRecord, PrismObject<? extends ObjectType> primaryObject, ExpressionProfile expressionProfile, Task task,
            OperationResult parentResult) {

        String name = propertyDef.getName();
        OperationResult result = parentResult.subresult(OP_EVALUATE_AUDIT_RECORD_PROPERTY)
                .addParam("name", name)
                .setMinor()
                .build();
        try {
            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("Name of SystemConfigurationAuditEventRecordingPropertyType is empty or null in " + propertyDef);
            }
            if (!targetSelectorMatches(propertyDef.getTargetSelector(), primaryObject)) {
                result.recordNotApplicable();
                return;
            }
            ExpressionType expression = propertyDef.getExpression();
            if (expression != null) {
                VariablesMap variables = new VariablesMap();
                variables.put(ExpressionConstants.VAR_TARGET, primaryObject, PrismObject.class);
                variables.put(ExpressionConstants.VAR_AUDIT_RECORD, auditRecord, AuditEventRecord.class);
                String shortDesc = "value for custom column of audit table";
                Collection<String> values = ExpressionUtil.evaluateStringExpression(variables, prismContext, expression,
                        expressionProfile, expressionFactory, shortDesc, task, result);
                if (values == null || values.isEmpty()) {
                    // nothing to do
                } else if (values.size() == 1) {
                    auditRecord.getCustomColumnProperty().put(name, values.iterator().next());
                } else {
                    throw new IllegalArgumentException("Collection of expression result contains more than one value");
                }
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't evaluate audit record property expression {}", t, name);
            // Intentionally not throwing the exception. The error is marked as partial.
            // (It would be better to mark it as fatal and to derive overall result as partial, but we aren't that far yet.)
            result.recordPartialError(t);
        } finally {
            result.recordSuccessIfUnknown();
        }
    }

    private boolean targetSelectorMatches(List<ObjectSelectorType> targetSelectors,
            PrismObject<? extends ObjectType> primaryObject) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        if (targetSelectors.isEmpty()) {
            return true;
        }
        for (ObjectSelectorType targetSelector : targetSelectors) {
            if (repositoryService.selectorMatches(targetSelector, primaryObject, null, LOGGER, "target selector")) {
                return true;
            }
        }
        LOGGER.debug("No selector matches for {}", primaryObject);
        return false;
    }

    public AuditConfiguration getAuditConfiguration(SystemConfigurationType config) {
        boolean recordResourceOids = false;
        List<SystemConfigurationAuditEventRecordingPropertyType> propertiesToRecord = emptyList();
        ExpressionType eventRecordingExpression = null;

        if (config != null && config.getAudit() != null && config.getAudit().getEventRecording() != null) {
            SystemConfigurationAuditEventRecordingType eventRecording = config.getAudit().getEventRecording();
            recordResourceOids = Boolean.TRUE.equals(eventRecording.isRecordResourceOids());
            propertiesToRecord = eventRecording.getProperty();
            eventRecordingExpression = eventRecording.getExpression();
        }

        return new AuditConfiguration(recordResourceOids, propertiesToRecord, eventRecordingExpression);
    }

    public OperationResult cloneResultForAuditEventRecord(OperationResult result) {
        // This is a brutal hack -- FIXME: create some "compute in-depth preview" method on operation result
        OperationResult clone = result.clone(2, false);
        for (OperationResult subresult : clone.getSubresults()) {
            subresult.computeStatusIfUnknown();
        }
        clone.computeStatus();

        return clone;
    }

    /**
     * Adds a message to the record by pulling the messages from individual delta results.
     */
    public void addRecordMessage(AuditEventRecord auditRecord, String message) {
        if (auditRecord.getMessage() != null) {
            return;
        }
        if (!StringUtils.isEmpty(message)) {
            auditRecord.setMessage(message);
            return;
        }
        Collection<ObjectDeltaOperation<? extends ObjectType>> deltas = auditRecord.getDeltas();
        if (deltas.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (ObjectDeltaOperation<? extends ObjectType> delta : deltas) {
            OperationResult executionResult = delta.getExecutionResult();
            if (executionResult != null) {
                String deltaMessage = executionResult.getMessage();
                if (!StringUtils.isEmpty(deltaMessage)) {
                    if (sb.length() != 0) {
                        sb.append("; ");
                    }
                    sb.append(deltaMessage);
                }
            }
        }
        auditRecord.setMessage(sb.toString());
    }
}
