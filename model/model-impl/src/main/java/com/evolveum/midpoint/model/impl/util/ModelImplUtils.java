/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.util;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment.ExpressionEnvironmentBuilder;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.model.impl.importer.ObjectImporter;
import com.evolveum.midpoint.model.impl.lens.AssignmentPathVariables;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.FullTextFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.error.ErrorStackDumper;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.MaintenanceException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

public class ModelImplUtils {

    private static final String OPERATION_RESOLVE_REFERENCE = ObjectImporter.class.getName() + ".resolveReference";

    private static final Trace LOGGER = TraceManager.getTrace(ModelImplUtils.class);

    public static void validatePaging(ObjectPaging paging) {
        if (paging == null) {
            return;
        }

        if (paging.getMaxSize() != null && paging.getMaxSize().longValue() < 0) {
            throw new IllegalArgumentException("Paging max size must be more than 0.");
        }
        if (paging.getOffset() != null && paging.getOffset().longValue() < 0) {
            throw new IllegalArgumentException("Paging offset index must be more than 0.");
        }
    }

    public static void recordFatalError(OperationResult result, Throwable e) {
        recordFatalError(result, e.getMessage(), e);
    }

    public static void recordFatalError(OperationResult result, String message, Throwable e) {
        // Do not log at ERROR level. This is too harsh. Especially in object not found case.
        // What model considers an error may be just a normal situation for the code is using model API.
        // If this is really an error then it should be logged by the invoking code.
        LoggingUtils.logExceptionOnDebugLevel(LOGGER, message, e);
        result.recordFatalError(message, e);
        result.cleanupResult(e);
    }

    public static void recordException(OperationResult result, Throwable e) {
        recordException(result, e.getMessage(), e);
    }

    private static void recordException(OperationResult result, String message, Throwable e) {
        // Do not log at ERROR level. This is too harsh. Especially in object not found case.
        // What model considers an error may be just a normal situation for the code is using model API.
        // If this is really an error then it should be logged by the invoking code.
        LoggingUtils.logExceptionOnDebugLevel(LOGGER, message, e);
        if (InternalsConfig.consistencyChecks) { // TODO implement correctly
            // The idea is to do this only when explicitly enabled, at least for the PoC, so let's attach it to consistency
            // checks that are set e.g. in tests
            String stack = ErrorStackDumper.dump(e, result);
            LOGGER.debug("Error stack:\n{}", stack);
        }
        result.recordException(message, e);
        result.cleanup();
    }

    public static void recordPartialError(OperationResult result, Throwable e) {
        recordPartialError(result, e.getMessage(), e);
    }

    public static void recordPartialError(OperationResult result, String message, Throwable e) {
        // Do not log at ERROR level. This is too harsh. Especially in object not found case.
        // What model considers an error may be just a normal situation for the code is using model API.
        // If this is really an error then it should be logged by the invoking code.
        LoggingUtils.logExceptionOnDebugLevel(LOGGER, message, e);
        result.recordPartialError(message, e);
        result.cleanupResult(e);
    }

    public static <O extends ObjectType> String getOperationUrlFromDelta(ObjectDelta<O> delta) {
        if (delta == null) {
            return null;
        }
        if (delta.isAdd()) {
            return ModelAuthorizationAction.ADD.getUrl();
        }
        if (delta.isModify()) {
            return ModelAuthorizationAction.MODIFY.getUrl();
        }
        if (delta.isDelete()) {
            return ModelAuthorizationAction.DELETE.getUrl();
        }
        throw new IllegalArgumentException("Unknown delta type "+delta);
    }


    // from the most to least appropriate
    @NotNull
    public static <O extends ObjectType> List<ObjectPolicyConfigurationType> getApplicablePolicies(
            @Nullable Class<O> objectClass, List<String> objectSubtypes, SystemConfigurationType systemConfigurationType)
            throws ConfigurationException {
        List<ObjectPolicyConfigurationType> rv = new ArrayList<>();
        List<ObjectPolicyConfigurationType> typeNoSubtype = new ArrayList<>();
        List<ObjectPolicyConfigurationType> typeWithSubtype = new ArrayList<>();
        List<ObjectPolicyConfigurationType> noTypeNoSubtype = new ArrayList<>();
        List<ObjectPolicyConfigurationType> noTypeWithSubtype = new ArrayList<>();
        List<ObjectPolicyConfigurationType> all = new ArrayList<>(
                systemConfigurationType.getDefaultObjectPolicyConfiguration());

        for (ObjectPolicyConfigurationType aPolicyConfigurationType: all) {
            QName typeQName = aPolicyConfigurationType.getType();
            if (typeQName != null) {
                ObjectTypes objectType = ObjectTypes.getObjectTypeFromTypeQName(typeQName);
                if (objectType == null) {
                    throw new ConfigurationException(
                            "Unknown type " + typeQName + " in default object policy definition "
                                    + "or object template definition in system configuration");
                }
                if (objectType.getClassDefinition() == objectClass) {
                    String aSubType = aPolicyConfigurationType.getSubtype();
                    if (aSubType == null) {
                        typeNoSubtype.add(aPolicyConfigurationType);
                    } else if (objectSubtypes != null && objectSubtypes.contains(aSubType)) {
                        typeWithSubtype.add(aPolicyConfigurationType);
                    }
                }
            } else {
                String aSubType = aPolicyConfigurationType.getSubtype();
                if (aSubType == null) {
                    noTypeNoSubtype.add(aPolicyConfigurationType);
                } else if (objectSubtypes != null && objectSubtypes.contains(aSubType)) {
                    noTypeWithSubtype.add(aPolicyConfigurationType);
                }
            }
        }
        rv.addAll(typeWithSubtype);
        rv.addAll(typeNoSubtype);
        rv.addAll(noTypeWithSubtype);
        rv.addAll(noTypeNoSubtype);
        return rv;
    }

    @NotNull
    public static <F extends ObjectType> List<ObjectPolicyConfigurationType> getApplicablePolicies(LensContext<F> context) {
        PrismObject<SystemConfigurationType> config = context.getSystemConfiguration();
        if (config == null) {
            return Collections.emptyList();
        }
        LensFocusContext<F> focusContext = context.getFocusContext();
        PrismObject<F> focusObject = focusContext != null ? focusContext.getObjectAny() : null;
        Class<F> focusClass = focusContext != null ? focusContext.getObjectTypeClass() : null;
        List<String> subTypes = FocusTypeUtil.determineSubTypes(focusObject);
        List<ObjectPolicyConfigurationType> relevantPolicies;
        try {
            relevantPolicies = ModelImplUtils.getApplicablePolicies(focusClass, subTypes, config.asObjectable());
        } catch (ConfigurationException e) {
            throw new SystemException("Couldn't get relevant object policies", e);
        }
        LOGGER.trace("Relevant policies: {}", relevantPolicies);
        return relevantPolicies;
    }

    public static <F extends ObjectType> ConflictResolutionType getConflictResolution(LensContext<F> context) {
        for (ObjectPolicyConfigurationType p : ModelImplUtils.getApplicablePolicies(context)) {
            if (p.getConflictResolution() != null) {
                return p.getConflictResolution();
            }
        }
        return null;
    }

    /**
     * Resolves references contained in given PrismObject.
     *
     * @param enforceReferentialIntegrity If true, missing reference causes fatal error when processing (if false, only warning is issued).
     * @param forceFilterReevaluation If true, references are reevaluated even if OID is present. (Given that filter is present as well, of course.)
     */
    public static <T extends ObjectType> void resolveReferences(
            PrismObject<T> object, RepositoryService repository,
                boolean enforceReferentialIntegrity, boolean forceFilterReevaluation, EvaluationTimeType resolutionTime,
                boolean throwExceptionOnFailure,
                OperationResult result) {

        Visitor visitor = visitable -> {
            if (!(visitable instanceof PrismReferenceValue)) {
                return;
            }
            resolveRef(
                    (PrismReferenceValue)visitable, repository, enforceReferentialIntegrity, forceFilterReevaluation,
                    resolutionTime, object.toString(), throwExceptionOnFailure, result);
        };
        object.accept(visitor);
    }

    /**
     * Resolves references contained in ADD and REPLACE value sets for item modifications in a given ObjectDelta.
     * (specially treats collisions with values to be deleted)
     */
    public static <T extends ObjectType> void resolveReferences(
            ObjectDelta<T> objectDelta, RepositoryService repository,
            boolean enforceReferentialIntegrity, boolean forceFilterReevaluation,
            EvaluationTimeType resolutionTime, boolean throwExceptionOnFailure,
            OperationResult result) {

        Visitor visitor = visitable -> {
            if (!(visitable instanceof PrismReferenceValue)) {
                return;
            }
            resolveRef(
                    (PrismReferenceValue) visitable, repository, enforceReferentialIntegrity, forceFilterReevaluation,
                    resolutionTime, objectDelta.toString(), throwExceptionOnFailure, result);
        };
        // We could use objectDelta.accept(visitor), but we want to visit only values to add and replace
        // (NOT values to delete! - otherwise very strange effects could result)

        // Another problem is that it is possible that one of valuesToAdd became (after resolving)
        // a value that is meant do be deleted. The result would be deletion of that value; definitely
        // not what we would want or expect. So we have to check whether a value that was not among
        // values to be deleted accidentally becomes one of values to be deleted.
        if (objectDelta.isAdd()) {
            objectDelta.getObjectToAdd().accept(visitor);
        } else if (objectDelta.isModify()) {
            for (ItemDelta<?,?> delta : objectDelta.getModifications()) {
                applyVisitorToValues(delta.getValuesToAdd(), delta, visitor);
                applyVisitorToValues(delta.getValuesToReplace(), delta, visitor);
            }
        }
    }

    // see description in caller
    static void applyVisitorToValues(Collection<? extends PrismValue> values, ItemDelta<?,?> delta, Visitor visitor) {
        Collection<? extends PrismValue> valuesToDelete = delta.getValuesToDelete();
        if (valuesToDelete == null) {
            valuesToDelete = new ArrayList<>(0);        // just to simplify the code below
        }
        if (values != null) {
            for (PrismValue pval : values) {
                boolean isToBeDeleted = valuesToDelete.contains(pval);
                pval.accept(visitor);
                if (!isToBeDeleted && valuesToDelete.contains(pval)) {
                    // value becomes 'to be deleted' -> we remove it from toBeDeleted list
                    delta.removeValueToDelete(pval);
                }
            }
        }
    }

    /**
     * Resolves a filter in a reference and return objects. Skips the resolution if there's an expression in a filter.
     * (Currently checks only the top level!)
     */
    public static @NotNull List<String> resolveObjectsFromRef(
            PrismReferenceValue refVal, RepositoryService repository, EvaluationTimeType evaluationTime,
            String contextDesc, boolean throwExceptionOnFailure, OperationResult parentResult) {
        PrismContext prismContext = PrismContext.get();
        String refName = refVal.getParent() != null ?
                refVal.getParent().getElementName().toString() : "(unnamed)";

        var effectiveResolutionTime = refVal.getEffectiveResolutionTime();
        if (effectiveResolutionTime != evaluationTime) {
            LOGGER.trace("Skipping resolution of reference {} in {} because the resolutionTime is set to {}",
                    refName, contextDesc, effectiveResolutionTime);
            return List.of();
        }

        OperationResult result = parentResult.createMinorSubresult(OPERATION_RESOLVE_REFERENCE);
        result.addContext(OperationResult.CONTEXT_ITEM, refName);

        QName typeQName = refVal.determineTargetTypeName();
        Class<? extends ObjectType> type = ObjectType.class;
        if (typeQName != null) {
            type = prismContext.getSchemaRegistry().determineCompileTimeClass(typeQName);
            if (type == null) {
                result.recordWarning(
                        "Unknown type specified in reference or definition of reference " + refName + ": " + typeQName);
                type = ObjectType.class;
            }
        }

        return resolveObjectsFromRef(
                refVal,
                repository,
                contextDesc,
                throwExceptionOnFailure,
                type,
                result);
    }

    private static List<String> resolveObjectsFromRef(
            PrismReferenceValue refVal, RepositoryService repository,
            String contextDesc, boolean throwExceptionOnFailure, Class<? extends ObjectType> type, OperationResult result) {
        PrismContext prismContext = PrismContext.get();
        String refName = refVal.getParent() != null ?
                refVal.getParent().getElementName().toString() : "(unnamed)";

        if (type == null) {
            type = ObjectType.class;
        }
        SearchFilterType filter = refVal.getFilter();

        if (filter == null) {
            // No filter. We are lost.
            String message = "Nor filter for property " + refName + ": cannot resolve objects";
            result.recordFatalError(message);
            if (throwExceptionOnFailure) {
                throw new SystemException(message);
            }
            return List.of();
        }
        // No OID and we have filter. Let's check the filter a bit
        ObjectFilter objFilter;
        try{
            PrismObjectDefinition<?> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
            objFilter = prismContext.getQueryConverter().parseFilter(filter, objDef);
        } catch (SchemaException ex){
            LOGGER.error("Failed to convert object filter from filter because of: "+ ex.getMessage() + "; filter: " + filter.debugDump(), ex);
            throw new SystemException("Failed to convert object filter from filter. Reason: " + ex.getMessage(), ex);
        }

        LOGGER.trace("Resolving using filter {}", objFilter.debugDumpLazily());
        List<PrismObject<? extends ObjectType>> objects;
        QName objectType = refVal.getTargetType();
        if (objectType == null) {
            String message = "Missing definition of type of reference " + refName;
            result.recordFatalError(message);
            if (throwExceptionOnFailure) {
                throw new SystemException(message);
            }
            return List.of();
        }

        if (containExpression(objFilter)) {
            result.recordSuccessIfUnknown();
            return List.of();
        }

        try {
            ObjectQuery query = prismContext.queryFactory().createQuery(objFilter);
            objects = (List)repository.searchObjects(type, query, readOnly(), result);

        } catch (SchemaException e) {
            // This is unexpected, but may happen. Record fatal error
            String message = "Repository schema error during resolution of reference " + refName;
            result.recordFatalError(message, e);
            if (throwExceptionOnFailure) {
                throw new SystemException(message, e);
            }
            return List.of();
        } catch (SystemException e) {
            // We don't want this to tear down entire import.
            String message = "Repository system error during resolution of reference " + refName;
            result.recordFatalError(message, e);
            if (throwExceptionOnFailure) {
                throw new SystemException(message, e);
            }
            return List.of();
        }
        return objects.stream()
                .map(PrismObject::getOid)
                .toList();
    }

    /**
     * Resolves a filter in a reference. Skips the resolution if there's an expression in a filter.
     * (Currently checks only the top level!)
     */
    public static void resolveRef(
            PrismReferenceValue refVal, RepositoryService repository,
            boolean enforceReferentialIntegrity, boolean forceFilterReevaluation, EvaluationTimeType evaluationTime,
            String contextDesc, boolean throwExceptionOnFailure, OperationResult parentResult) {

        PrismContext prismContext = PrismContext.get();
        String refName = refVal.getParent() != null ?
                refVal.getParent().getElementName().toString() : "(unnamed)";

        var effectiveResolutionTime = refVal.getEffectiveResolutionTime();
        if (effectiveResolutionTime != evaluationTime) {
            LOGGER.trace("Skipping resolution of reference {} in {} because the resolutionTime is set to {}",
                    refName, contextDesc, effectiveResolutionTime);
            return;
        }

        OperationResult result = parentResult.createMinorSubresult(OPERATION_RESOLVE_REFERENCE);
        result.addContext(OperationResult.CONTEXT_ITEM, refName);

        QName typeQName = refVal.determineTargetTypeName();
        Class<? extends ObjectType> type = ObjectType.class;
        if (typeQName != null) {
            type = prismContext.getSchemaRegistry().determineCompileTimeClass(typeQName);
            if (type == null) {
                result.recordWarning(
                        "Unknown type specified in reference or definition of reference " + refName + ": " + typeQName);
                type = ObjectType.class;
            }
        }

        SearchFilterType filter = refVal.getFilter();

        if (!StringUtils.isBlank(refVal.getOid()) && (!forceFilterReevaluation || filter == null)) {
            // We have OID (and "force filter reevaluation" is not requested or not possible)
            if (filter != null) {
                // We have both filter and OID. We will choose OID, but let's at least log a warning.
                LOGGER.debug("Both OID and filter for property {} in {}, OID takes precedence", refName, contextDesc);
            }
            // Nothing to resolve, but let's check if the OID exists
            PrismObject<? extends ObjectType> object = null;
            try {
                // Maybe too courageous (we are not sure what clients do with the resolved objects).
                object = repository.getObject(type, refVal.getOid(), readOnly(), result);
            } catch (ObjectNotFoundException e) {
                String message = "Reference " + refName + " refers to a non-existing object " + refVal.getOid();
                if (enforceReferentialIntegrity) {
                    LOGGER.error(message);
                    result.recordFatalError(message);
                    if (throwExceptionOnFailure) {
                        throw new SystemException(message, e);
                    }
                } else {
                    LOGGER.warn(message);
                    result.recordWarning(message);
                }
            } catch (SchemaException e) {
                String message = "Schema error while trying to retrieve object " + refVal.getOid() + " : " + e.getMessage();
                result.recordPartialError(message, e);
                LOGGER.error(message, e);
                // But continue otherwise
            }
            if (object != null && refVal.getOriginType() != null) {
                // Check if declared and actual type matches
                //noinspection EqualsBetweenInconvertibleTypes - both are Class, false alarm
                if (!object.getClass().equals(type)) {
                    result.recordWarning("Type mismatch on property " + refName + ": declared:"
                            + refVal.getOriginType() + ", actual: " + object.getClass());
                }
            }
            result.recordSuccessIfUnknown();
            parentResult.computeStatus();
            return;
        }

        if (filter == null) {
            if (refVal.getObject() != null) {
                LOGGER.trace("Skipping resolution of reference {} in {} because the object is present and the filter is not", refName, contextDesc);
                result.recordNotApplicableIfUnknown();
                return;
            }
            // No OID and no filter. We are lost.
            String message = "Neither OID nor filter for property " + refName + ": cannot resolve reference";
            result.recordFatalError(message);
            if (throwExceptionOnFailure) {
                throw new SystemException(message);
            }
            return;
        }

        List<String> objects = resolveObjectsFromRef(
                refVal,
                repository,
                contextDesc,
                throwExceptionOnFailure,
                type,
                result);

        if (objects.isEmpty()) {
            String message = "Repository reference " + refName + " cannot be resolved: filter matches no object";
            result.recordFatalError(message);
            if (throwExceptionOnFailure) {
                throw new SystemException(message);
            }
            return;
        }
        if (objects.size() > 1) {
            String message = "Repository reference " + refName
                    + " cannot be resolved: filter matches " + objects.size() + " objects";
            result.recordFatalError(message);
            if (throwExceptionOnFailure) {
                throw new SystemException(message);
            }
            return;
        }
        // Bingo. We have exactly one object.
        String oid = objects.get(0);
        refVal.setOid(oid);
        result.recordSuccessIfUnknown();
    }

    // TODO what about expressions in deeper filters (e.g. when using AND/OR/NOT?)
    private static boolean containExpression(ObjectFilter filter) {
        return filter instanceof InOidFilter inOidFilter && inOidFilter.getExpression() != null
                || filter instanceof FullTextFilter fullTextFilter && fullTextFilter.getExpression() != null
                || filter instanceof ValueFilter<?, ?> valueFilter && valueFilter.getExpression() != null;
    }

    public static void encrypt(Collection<ObjectDelta<? extends ObjectType>> deltas, Protector protector, ModelExecuteOptions options,
            OperationResult result) {
        // Encrypt values even before we log anything. We want to avoid showing unencrypted values in the logfiles
        if (!ModelExecuteOptions.isNoCrypt(options)) {
            for(ObjectDelta<? extends ObjectType> delta: deltas) {
                try {
                    CryptoUtil.encryptValues(protector, delta);
                } catch (EncryptionException e) {
                    result.recordFatalError(e);
                    throw new SystemException(e.getMessage(), e);
                }
            }
        }
    }

    public static void setRequestee(Task task, LensContext<?> context) {
        PrismObject<? extends ObjectType> object;
        if (context != null
                && context.getFocusContext() != null
                && UserType.class.isAssignableFrom(context.getFocusContext().getObjectTypeClass())) {
            object = context.getFocusContext().getObjectAny();
        } else {
            object = null;
        }
        setRequestee(task, object);
    }

    public static <F extends ObjectType> void setRequestee(Task task, LensFocusContext<F> context) {
        setRequestee(task, context.getLensContext());
    }

    public static void setRequestee(Task task, PrismObject object) {
        LOGGER.trace("setting requestee in {} to {}", task, object);
        if (task != null) {
            task.setRequesteeTransient(object);
        }
    }

    public static void clearRequestee(Task task) {
        setRequestee(task, (PrismObject) null);
    }

    private static ModelExecuteOptionsType getModelExecuteOptionsBean(PrismContainerValue<?> taskExtension) {
        if (taskExtension == null) {
            return null;
        }

        ModelExecuteOptionsType options1 =
                taskExtension.getItemRealValue(SchemaConstants.C_MODEL_EXECUTE_OPTIONS, ModelExecuteOptionsType.class);
        if (options1 != null) {
            return options1;
        }

        ModelExecuteOptionsType options2 =
                taskExtension.getItemRealValue(
                        SchemaConstants.MODEL_EXTENSION_MODEL_EXECUTE_OPTIONS, ModelExecuteOptionsType.class);
        if (options2 != null) {
            return options2;
        }

        return taskExtension.getItemRealValue(SchemaConstants.MODEL_EXTENSION_EXECUTE_OPTIONS, ModelExecuteOptionsType.class);
    }

    public static ModelExecuteOptions getModelExecuteOptions(PrismContainerValue<?> taskExtension) {
        return ModelExecuteOptions.fromModelExecutionOptionsType(
                getModelExecuteOptionsBean(taskExtension));
    }

    public static VariablesMap getDefaultVariablesMap(@NotNull LensContext<?> context,
            @Nullable LensProjectionContext projCtx, boolean focusOdoAbsolute) throws SchemaException, ConfigurationException {
        VariablesMap variables = new VariablesMap();
        if (context.getFocusContext() != null) {
            ObjectDeltaObject<?> focusOdo = focusOdoAbsolute
                    ? context.getFocusContext().getObjectDeltaObjectAbsolute()
                    : context.getFocusContext().getObjectDeltaObjectRelative();
            variables.put(ExpressionConstants.VAR_FOCUS, focusOdo, focusOdo.getDefinition());
            variables.put(ExpressionConstants.VAR_USER, focusOdo, focusOdo.getDefinition());
            variables.registerAlias(ExpressionConstants.VAR_USER, ExpressionConstants.VAR_FOCUS);
        }
        if (projCtx != null) {
            variables.put(ExpressionConstants.VAR_PROJECTION, projCtx.getObjectDeltaObject(), projCtx.getObjectDefinition());
            variables.put(ExpressionConstants.VAR_SHADOW, projCtx.getObjectDeltaObject(), projCtx.getObjectDefinition());
            variables.put(ExpressionConstants.VAR_ACCOUNT, projCtx.getObjectDeltaObject(), projCtx.getObjectDefinition());
            variables.registerAlias(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION);
            variables.registerAlias(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION);
            variables.put(ExpressionConstants.VAR_RESOURCE, projCtx.getResource(), projCtx.getResource().asPrismObject().getDefinition());
            variables.put(ExpressionConstants.VAR_OPERATION, projCtx.getOperation().getValue(), String.class);
            variables.put(ExpressionConstants.VAR_ITERATION, LensUtil.getIterationVariableValue(projCtx), Integer.class);
            variables.put(ExpressionConstants.VAR_ITERATION_TOKEN, LensUtil.getIterationTokenVariableValue(projCtx), String.class);
        }

        PrismObject<SystemConfigurationType> systemConfiguration = context.getSystemConfiguration();
        if (systemConfiguration != null) {
            variables.put(ExpressionConstants.VAR_CONFIGURATION, systemConfiguration, systemConfiguration.getDefinition());
        } else {
            variables.put(ExpressionConstants.VAR_CONFIGURATION, null, SystemConfigurationType.class);
        }
        return variables;
    }

    public static VariablesMap getDefaultVariablesMap(
            ObjectType focus, ShadowType shadow, ResourceType resource, SystemConfigurationType configuration) {
        return ExpressionUtil.getDefaultVariablesMap(focus, shadow, resource, configuration);
    }

    public static <O extends ObjectType> VariablesMap getDefaultVariablesMap(
            PrismObject<? extends ObjectType> focus,
            PrismObject<? extends ShadowType> shadow,
            PrismObject<ResourceType> resource,
            PrismObject<SystemConfigurationType> configuration,
            LensElementContext<O> affectedElementContext) {
        VariablesMap variables = new VariablesMap();
        ExpressionUtil.addDefaultVariablesMap(variables, focus, shadow, resource, configuration);
        addOperation(variables, affectedElementContext);
        return variables;
    }

    private static <O extends ObjectType> void addOperation(
            VariablesMap variables,
            LensElementContext<O> affectedElementContext) {
        if (affectedElementContext != null) {
            variables.put(ExpressionConstants.VAR_OPERATION, affectedElementContext.getOperation().getValue(), String.class);
            // We do not want to add delta to all expressions. The delta may be tricky. Is it focus delta? projection delta? Primary? Secondary?
            // It is better to leave delta to be accessed from the model context. And in cases when it is clear which delta is meant
            // (e.g. provisioning scripts) we can still add the delta explicitly.
        }
    }

    public static void addAssignmentPathVariables(AssignmentPathVariables assignmentPathVariables, VariablesMap VariablesMap) {
        if (assignmentPathVariables != null) {
            PrismContainerDefinition<AssignmentType> assignmentDef = assignmentPathVariables.getAssignmentDefinition();
            VariablesMap.put(ExpressionConstants.VAR_ASSIGNMENT, assignmentPathVariables.getMagicAssignment(), assignmentDef);
            VariablesMap.put(ExpressionConstants.VAR_ASSIGNMENT_PATH, assignmentPathVariables.getAssignmentPath(), AssignmentPath.class);
            VariablesMap.put(ExpressionConstants.VAR_IMMEDIATE_ASSIGNMENT, assignmentPathVariables.getImmediateAssignment(), assignmentDef);
            VariablesMap.put(ExpressionConstants.VAR_THIS_ASSIGNMENT, assignmentPathVariables.getThisAssignment(), assignmentDef);
            VariablesMap.put(ExpressionConstants.VAR_FOCUS_ASSIGNMENT, assignmentPathVariables.getFocusAssignment(), assignmentDef);
            PrismObjectDefinition<AbstractRoleType> abstractRoleDefinition =
                    PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AbstractRoleType.class);
            VariablesMap.put(ExpressionConstants.VAR_IMMEDIATE_ROLE, assignmentPathVariables.getImmediateRole(), abstractRoleDefinition);
        } else {
            // to avoid "no such variable" exceptions in boundary cases
            // for null/empty paths we might consider creating empty AssignmentPathVariables objects to keep null/empty path distinction
            VariablesMap.put(ExpressionConstants.VAR_ASSIGNMENT_PATH, null, AssignmentPath.class);
        }
    }

    public static PrismReferenceValue determineAuditTargetDeltaOps(
            Collection<ObjectDeltaOperation<? extends ObjectType>> deltaOps) {
        if (deltaOps == null || deltaOps.isEmpty()) {
            return null;
        }
        if (deltaOps.size() == 1) {
            ObjectDeltaOperation<? extends ObjectType> deltaOp = deltaOps.iterator().next();
            return getAuditTarget(deltaOp.getObjectDelta());
        }
        for (ObjectDeltaOperation<? extends ObjectType> deltaOp: deltaOps) {
            if (!ShadowType.class.isAssignableFrom(deltaOp.getObjectDelta().getObjectTypeClass())) {
                return getAuditTarget(deltaOp.getObjectDelta());
            }
        }
        // Several raw operations, all on shadows, no focus ... this should not happen
        // But if it does we rather do not specify any target. We should not like to choose
        // target randomly. That would be confusing.
        return null;
    }

    public static PrismReferenceValue determineAuditTarget(Collection<ObjectDelta<? extends ObjectType>> deltas) {
        if (deltas == null || deltas.isEmpty()) {
            return null;
        }
        if (deltas.size() == 1) {
            ObjectDelta<? extends ObjectType> delta = deltas.iterator().next();
            return getAuditTarget(delta);
        }
        for (ObjectDelta<? extends ObjectType> delta: deltas) {
            if (!ShadowType.class.isAssignableFrom(delta.getObjectTypeClass())) {
                return getAuditTarget(delta);
            }
        }
        // Several raw operations, all on shadows, no focus ... this should not happen
        // But if it does we rather do not specify any target. We should not like to choose
        // target randomly. That would be confusing.
        return null;
    }

    private static PrismReferenceValue getAuditTarget(ObjectDelta<? extends ObjectType> delta) {
        PrismReferenceValue targetRef = PrismContext.get().itemFactory().createReferenceValue(delta.getOid());
        targetRef.setTargetType(ObjectTypes.getObjectType(delta.getObjectTypeClass()).getTypeQName());
        if (delta.isAdd()) {
            targetRef.setObject(delta.getObjectToAdd());
        }
        return targetRef;
    }

    public static <V extends PrismValue, F extends ObjectType> @NotNull List<V> evaluateScript(
            ScriptExpression scriptExpression,
            LensContext<F> lensContext,
            VariablesMap variables,
            boolean useNew,
            String shortDesc,
            Task task,
            OperationResult parentResult)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                new ExpressionEnvironmentBuilder<PrismValue, ItemDefinition<?>>()
                        .lensContext(lensContext)
                        .currentResult(parentResult)
                        .currentTask(task)
                        .build());

        try {
            ScriptExpressionEvaluationContext context = new ScriptExpressionEvaluationContext();
            context.setVariables(variables);
            context.setSuggestedReturnType(ScriptExpressionReturnTypeType.SCALAR);
            context.setEvaluateNew(useNew);
            context.setScriptExpression(scriptExpression);
            context.setContextDescription(shortDesc);
            context.setTask(task);
            context.setResult(parentResult);
            return scriptExpression.evaluate(context);
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    public static CriticalityType handleConnectorErrorCriticality(ResourceType resourceType, Throwable e, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException {
        CriticalityType criticality;
        if (resourceType == null) {
            RepoCommonUtils.throwException(e, result);
            return CriticalityType.FATAL; // not reached
        } else {
            ErrorSelectorType errorSelector = ResourceTypeUtil.getConnectorErrorCriticality(resourceType);
            if (e instanceof MaintenanceException) {
                // The resource was put to maintenance mode administratively. Do not log the error.
                if (result != null) {
                    result.recordSuccess();
                }
                return CriticalityType.IGNORE;
            } else if (e instanceof CommunicationException) {
                // Network problem. Just continue evaluation. The error is recorded in the result.
                // The consistency mechanism has (most likely) already done the best.
                // We cannot do any better.
                criticality = ExceptionUtil.getCriticality(errorSelector, e, CriticalityType.PARTIAL);
            } else if (e instanceof SchemaException || e instanceof PolicyViolationException) {
                // This may be caused by a variety of causes. It may be multiple values in a single-valued attribute.
                // But it may also be duplicate value or a problem of resource-side password policy.
                // Treat this as partial error by default. This is partially motivated by compatibility with
                // midPoint 3.8 and earlier. This may be reviewed in the future.
                criticality = ExceptionUtil.getCriticality(errorSelector, e, CriticalityType.PARTIAL);
            } else {
                criticality = ExceptionUtil.getCriticality(errorSelector, e, CriticalityType.FATAL);
            }
        }
        RepoCommonUtils.processErrorCriticality(resourceType, criticality, e, result);
        return criticality;
    }

    public static String generateRequestIdentifier() {
        return UUID.randomUUID().toString();
    }
}
