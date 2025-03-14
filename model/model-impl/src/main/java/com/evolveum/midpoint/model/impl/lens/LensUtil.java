/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

import static java.util.Collections.emptySet;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.config.AssignmentConfigItem;

import com.evolveum.midpoint.util.SingleLocalizableMessage;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathImpl;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathSegmentImpl;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.VirtualAssignmentSpecification;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.OriginProvider;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
public class LensUtil {

    private static final Trace LOGGER = TraceManager.getTrace(LensUtil.class);
    private static final QName CONDITION_OUTPUT_NAME = new QName(SchemaConstants.NS_C, "condition");

    public static <F extends ObjectType> ResourceType getResourceReadOnly(
            LensContext<F> context,
            String resourceOid,
            ProvisioningService provisioningService,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        ResourceType cached = context.getResource(resourceOid);
        if (cached != null) {
            return cached;
        }
        // Fetching from provisioning to take advantage of caching and pre-parsed schema
        ResourceType retrieved = provisioningService
                .getObject(ResourceType.class, resourceOid, readOnly(), task, result)
                .asObjectable();
        context.rememberResource(retrieved);
        return retrieved;
    }

    public static PropertyDelta<XMLGregorianCalendar> createActivationTimestampDelta(
            ActivationStatusType status, XMLGregorianCalendar now,
            PrismContainerDefinition<ActivationType> activationDefinition, OriginType origin) {
        ItemName timestampPropertyName;
        if (status == null || status == ActivationStatusType.ENABLED) {
            timestampPropertyName = ActivationType.F_ENABLE_TIMESTAMP;
        } else if (status == ActivationStatusType.DISABLED) {
            timestampPropertyName = ActivationType.F_DISABLE_TIMESTAMP;
        } else if (status == ActivationStatusType.ARCHIVED) {
            timestampPropertyName = ActivationType.F_ARCHIVE_TIMESTAMP;
        } else {
            throw new IllegalArgumentException("Unknown activation status "+status);
        }

        PropertyDelta<XMLGregorianCalendar> timestampDelta = activationDefinition
                .<XMLGregorianCalendar>findPropertyDefinition(timestampPropertyName)
                .createEmptyDelta(FocusType.F_ACTIVATION.append(timestampPropertyName));
        timestampDelta.setValueToReplace(
                PrismContext.get().itemFactory().createPropertyValue(now, origin, null));
        return timestampDelta;
    }

    public static <F extends ObjectType> void moveTriggers(LensProjectionContext projCtx, LensFocusContext<F> focusCtx) throws SchemaException {
        ObjectDelta<ShadowType> projSecondaryDelta = projCtx.getCurrentDelta();
        if (projSecondaryDelta == null) {
            return;
        }
        var modifications = projSecondaryDelta.getModifications();
        var iterator = modifications.iterator();
        while (iterator.hasNext()) {
            var projModification = iterator.next();
            LOGGER.trace("MOD: {}\n{}", projModification.getPath(), projModification.debugDumpLazily());
            if (projModification.getPath().equivalent(SchemaConstants.PATH_TRIGGER)) {
                focusCtx.swallowToSecondaryDelta(projModification);
                iterator.remove();
            }
        }
    }

    public static Object getIterationVariableValue(LensProjectionContext accCtx) {
        Integer iterationOld = null;
        PrismObject<ShadowType> shadowCurrent = accCtx.getObjectCurrent();
        if (shadowCurrent != null) {
            iterationOld = shadowCurrent.asObjectable().getIteration();
        }
        if (iterationOld == null) {
            return accCtx.getIteration();
        }
        PrismPropertyDefinition<Integer> propDef = PrismContext.get().definitionFactory()
                .newPropertyDefinition(ExpressionConstants.VAR_ITERATION_QNAME, DOMUtil.XSD_INT);
        PrismProperty<Integer> propOld = propDef.instantiate();
        propOld.setRealValue(iterationOld);
        PropertyDelta<Integer> propDelta = propDef.createEmptyDelta(ExpressionConstants.VAR_ITERATION_QNAME);
        propDelta.setRealValuesToReplace(accCtx.getIteration());
        PrismProperty<Integer> propNew = propDef.instantiate();
        propNew.setRealValue(accCtx.getIteration());
        return new ItemDeltaItem<>(propOld, propDelta, propNew, propDef);
    }

    public static Object getIterationTokenVariableValue(LensProjectionContext accCtx) {
        String iterationTokenOld = null;
        PrismObject<ShadowType> shadowCurrent = accCtx.getObjectCurrent();
        if (shadowCurrent != null) {
            iterationTokenOld = shadowCurrent.asObjectable().getIterationToken();
        }
        if (iterationTokenOld == null) {
            return accCtx.getIterationToken();
        }
        PrismPropertyDefinition<String> propDef = PrismContext.get().definitionFactory()
                .newPropertyDefinition(ExpressionConstants.VAR_ITERATION_TOKEN_QNAME, DOMUtil.XSD_STRING);
        PrismProperty<String> propOld = propDef.instantiate();
        propOld.setRealValue(iterationTokenOld);
        PropertyDelta<String> propDelta = propDef.createEmptyDelta(ExpressionConstants.VAR_ITERATION_TOKEN_QNAME);
        propDelta.setRealValuesToReplace(accCtx.getIterationToken());
        PrismProperty<String> propNew = propDef.instantiate();
        propNew.setRealValue(accCtx.getIterationToken());
        return new ItemDeltaItem<>(propOld, propDelta, propNew, propDef);
    }

    /**
     * Extracts the delta from this projection context and also from all other projection contexts that have
     * equivalent discriminator.
     */
    public static <F extends ObjectType> ObjectDelta<ShadowType> findAPrioriDelta(LensContext<F> context,
            LensProjectionContext projCtx) throws SchemaException {
        ObjectDelta<ShadowType> aPrioriDelta = null;
        for (LensProjectionContext aProjCtx: context.findRelatedContexts(projCtx)) {
            ObjectDelta<ShadowType> aProjDelta = aProjCtx.getSummaryDelta(); // todo check this
            if (aProjDelta != null) {
                if (aPrioriDelta == null) {
                    aPrioriDelta = aProjDelta.clone();
                } else {
                    aPrioriDelta.merge(aProjDelta);
                }
            }
        }
        return aPrioriDelta;
    }

    public static <T extends ObjectType, F extends ObjectType> void setContextOid(LensContext<F> context,
            LensElementContext<T> objectContext, String oid) {
        objectContext.setOid(oid);
        // Check if we need to propagate this oid also to higher-order contexts
        if (!(objectContext instanceof LensProjectionContext refProjCtx)) {
            return;
        }
        ProjectionContextKey refKey = refProjCtx.getKey();
        for (LensProjectionContext aProjCtx: context.getProjectionContexts()) {
            ProjectionContextKey aKey = aProjCtx.getKey();
            if (refKey.equivalent(aKey) && refKey.getOrder() < aKey.getOrder()) {
                aProjCtx.setOid(oid);
            }
        }
    }

    public static IterationSpecificationType getIterationSpecification(ObjectTemplateType objectTemplate) {
        return objectTemplate != null ? objectTemplate.getIterationSpecification() : null;
    }

    public static int determineMaxIterations(IterationSpecificationType iterationSpecType) {
        return iterationSpecType != null ? or0(iterationSpecType.getMaxIterations()) : 0;
    }

    public static String formatIterationToken(
            LensElementContext<?> accountContext,
            IterationSpecificationType iterationSpec,
            int iteration,
            ExpressionFactory expressionFactory,
            VariablesMap variables,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (iterationSpec == null) {
            return formatIterationTokenDefault(iteration);
        }
        ExpressionType tokenExpressionType = iterationSpec.getTokenExpression();
        if (tokenExpressionType == null) {
            return formatIterationTokenDefault(iteration);
        }
        PrismContext prismContext = PrismContext.get();
        PrismPropertyDefinition<String> outputDefinition = prismContext.definitionFactory().newPropertyDefinition(ExpressionConstants.VAR_ITERATION_TOKEN_QNAME,
                DOMUtil.XSD_STRING);
        Expression<PrismPropertyValue<String>,PrismPropertyDefinition<String>> expression =
                expressionFactory.makeExpression(
                        tokenExpressionType,
                        outputDefinition,
                        MiscSchemaUtil.getExpressionProfile(),
                        "iteration token expression in "+accountContext.getHumanReadableName(),
                        task,
                        result);

        Collection<Source<?,?>> sources = new ArrayList<>();
        PrismPropertyDefinition<Integer> inputDefinition =
                prismContext.definitionFactory().newPropertyDefinition(
                        ExpressionConstants.VAR_ITERATION_QNAME, DOMUtil.XSD_INT, 0, 1);
        PrismProperty<Integer> input = inputDefinition.instantiate();
        input.addRealValue(iteration);
        ItemDeltaItem<PrismPropertyValue<Integer>,PrismPropertyDefinition<Integer>> idi = new ItemDeltaItem<>(input);
        Source<PrismPropertyValue<Integer>,PrismPropertyDefinition<Integer>> iterationSource =
                new Source<>(idi, ExpressionConstants.VAR_ITERATION_QNAME);
        sources.add(iterationSource);

        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(
                sources , variables, "iteration token expression in "+accountContext.getHumanReadableName(), task);
        eeContext.setExpressionFactory(expressionFactory);

        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                ExpressionUtil.evaluateExpressionInContext(expression, eeContext, task, result);
        Collection<PrismPropertyValue<String>> outputValues = outputTriple.getNonNegativeValues();
        if (outputValues.isEmpty()) {
            return "";
        }
        if (outputValues.size() > 1) {
            throw new ExpressionEvaluationException("Iteration token expression in "+accountContext.getHumanReadableName()+" returned more than one value ("+outputValues.size()+" values)");
        }
        String realValue = outputValues.iterator().next().getValue();
        if (realValue == null) {
            return "";
        }
        return realValue;
    }

    public static String formatIterationTokenDefault(int iteration) {
        if (iteration == 0) {
            return "";
        }
        return Integer.toString(iteration);
    }

    public static <F extends ObjectType> boolean evaluateIterationCondition(
            LensContext<F> context,
            LensElementContext<?> elementContext,
            IterationSpecificationType iterationSpecification,
            int iteration,
            String iterationToken,
            boolean beforeIteration,
            ExpressionFactory expressionFactory,
            VariablesMap variables,
            Task task,
            OperationResult result)
            throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (iterationSpecification == null) {
            return true;
        }
        ExpressionType expressionType;
        String desc;
        if (beforeIteration) {
            expressionType = iterationSpecification.getPreIterationCondition();
            desc = "pre-iteration expression in "+elementContext.getHumanReadableName();
        } else {
            expressionType = iterationSpecification.getPostIterationCondition();
            desc = "post-iteration expression in "+elementContext.getHumanReadableName();
        }
        if (expressionType == null) {
            return true;
        }
        Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression = expressionFactory.makeExpression(
                expressionType, ExpressionUtil.createConditionOutputDefinition(), MiscSchemaUtil.getExpressionProfile(),
                desc, task, result);

        variables.put(ExpressionConstants.VAR_ITERATION, iteration, Integer.class);
        variables.put(ExpressionConstants.VAR_ITERATION_TOKEN, iterationToken, String.class);

        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null , variables, desc, task);
        eeContext.setExpressionFactory(expressionFactory);
        ModelExpressionEnvironment<?,?> env = new ModelExpressionEnvironment<>(context, null, task, result);
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple =
                ExpressionUtil.evaluateExpressionInContext(expression, eeContext, env, result);
        Collection<PrismPropertyValue<Boolean>> outputValues = outputTriple.getNonNegativeValues();
        if (outputValues.isEmpty()) {
            return false;
        }
        if (outputValues.size() > 1) {
            throw new ExpressionEvaluationException(desc+" returned more than one value ("+outputValues.size()+" values)");
        }
        Boolean realValue = outputValues.iterator().next().getValue();
        if (realValue == null) {
            return false;
        }
        return realValue;
    }

    /**
     * Used for assignments and similar objects that do not have separate lifecycle.
     */
    public static boolean isAssignmentValid(
            AssignmentHolderType focus, AssignmentType assignment,
            XMLGregorianCalendar now, ActivationComputer activationComputer, LifecycleStateModelType focusStateModel,
            @NotNull TaskExecutionMode taskExecutionMode) {
        String focusLifecycleState = focus.getLifecycleState();
        if (!activationComputer.lifecycleHasActiveAssignments(focusLifecycleState, focusStateModel, taskExecutionMode)) {
            return false;
        }
        return isValid(
                assignment.getLifecycleState(),
                assignment.getActivation(),
                now,
                activationComputer,
                focusStateModel,
                taskExecutionMode);
    }

    // [EP:APSO] DONE origins are correct here
    public static @NotNull <R extends AbstractRoleType> List<AssignmentConfigItem> getForcedAssignments(
            LifecycleStateModelType lifecycleModel, String stateName,
            ObjectResolver objectResolver, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        // We intentionally do not use DISTINCT option here, as it causes cache pass - at least in 4.8.
        // Instead, we do the deduplication by using a set, hoping it will work well enough
        // (assignments for the same object should be equal).
        Set<AssignmentType> forcedAssignments = new HashSet<>();

        VirtualAssignmentSpecification<R> forcedAssignmentSpec =
                LifecycleUtil.getForcedAssignmentSpecification(lifecycleModel, stateName);
        if (forcedAssignmentSpec != null) {
            objectResolver.searchIterative(
                    forcedAssignmentSpec.type(),
                    PrismContext.get().queryFactory().createQuery(forcedAssignmentSpec.filter()),
                    readOnly(),
                    (object, result1) -> {
                        forcedAssignments.add( // [EP:APSO] this results in pure generated assignment, no expressions
                                ObjectTypeUtil.createAssignmentTo(object));
                        return true;
                    },
                    task, result);
        }
        // Technically, the targetRef comes from forced assignment specification (potentially from various sources),
        // but for the purpose of expression profile determination, these assignments are considered to be generated
        // i.e. safe to evaluate under any profile. [Moreover, there are no expressions in these assignments anyway ;)]
        var originProvider = OriginProvider.generated();
        return ConfigurationItem.ofList(
                new ArrayList<>(forcedAssignments),
                originProvider,
                AssignmentConfigItem.class);
    }

    public static boolean isFocusValid(
            AssignmentHolderType focus,
            XMLGregorianCalendar now,
            ActivationComputer activationComputer,
            LifecycleStateModelType focusStateModel,
            @NotNull TaskExecutionMode taskExecutionMode) {
        ActivationType activation = focus instanceof FocusType ? ((FocusType) focus).getActivation() : null;
        return isValid(focus.getLifecycleState(), activation, now, activationComputer, focusStateModel, taskExecutionMode);
    }

    private static boolean isValid(
            String lifecycleState,
            ActivationType activationType,
            XMLGregorianCalendar now,
            ActivationComputer activationComputer,
            LifecycleStateModelType focusStateModel,
            @NotNull TaskExecutionMode taskExecutionMode) {
        String lifecycleStateHacked;
        if (!taskExecutionMode.isProductionConfiguration() && SchemaConstants.LIFECYCLE_PROPOSED.equals(lifecycleState)) {
            lifecycleStateHacked = SchemaConstants.LIFECYCLE_ACTIVE; // FIXME brutal hack
        } else {
            lifecycleStateHacked = lifecycleState;
        }
        TimeIntervalStatusType validityStatus = activationComputer.getValidityStatus(activationType, now);
        ActivationStatusType effectiveStatus =
                activationComputer.getEffectiveStatus(lifecycleStateHacked, activationType, validityStatus, focusStateModel);
        return effectiveStatus == ActivationStatusType.ENABLED;
    }

    /** Call this method through {@link AssignmentPathImpl} and eventually move there. */
    public static AssignmentPathVariables computeAssignmentPathVariables(AssignmentPathImpl assignmentPath) throws SchemaException {
        if (assignmentPath == null || assignmentPath.isEmpty()) {
            return null;
        }
        AssignmentPathVariables vars = new AssignmentPathVariables();
        vars.setAssignmentPath(assignmentPath.clone());

        Iterator<AssignmentPathSegmentImpl> iterator = assignmentPath.getSegments().iterator();
        while (iterator.hasNext()) {
            AssignmentPathSegmentImpl segment = iterator.next();
            ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> segmentAssignmentIdi =
                    segment.getAssignmentIdi();

            ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> magicAssignmentIdi;
            // Magic assignment
            if (vars.getMagicAssignment() == null) {
                magicAssignmentIdi = segmentAssignmentIdi.clone();
                vars.setMagicAssignment(magicAssignmentIdi);
            } else {
                // Collect extension values from the assignment extension
                magicAssignmentIdi = vars.getMagicAssignment();
                mergeExtension(magicAssignmentIdi, segmentAssignmentIdi);
            }

            // Collect extension values from the source object extension
            ObjectType segmentSource = segment.getSource();
            if (segmentSource != null) {
                mergeExtension(magicAssignmentIdi, segmentSource.asPrismObject());
            }

            // immediate assignment (use assignment from previous iteration)
            vars.setImmediateAssignment(vars.getThisAssignment());

            // this assignment
            vars.setThisAssignment(segmentAssignmentIdi.copy());

            if (iterator.hasNext() && segmentSource instanceof AbstractRoleType) {
                //noinspection unchecked
                vars.setImmediateRole((PrismObject<? extends AbstractRoleType>) segmentSource.asPrismObject());
            }
        }

        AssignmentPathSegmentImpl focusAssignmentSegment = assignmentPath.first();
        vars.setFocusAssignment(focusAssignmentSegment.getAssignmentIdi().copy());

        // a bit of hack -- if deleted, TestPreviewChanges and TestRbac start to fail -> TODO should be investigated
        // objects are already cloned
        vars.setMagicAssignment(convertToLegacy(vars.getMagicAssignment()));
        vars.setThisAssignment(convertToLegacy(vars.getThisAssignment()));
        vars.setFocusAssignment(convertToLegacy(vars.getFocusAssignment()));
        vars.setImmediateAssignment(convertToLegacy(vars.getImmediateAssignment()));

        return vars;
    }

    private static ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> convertToLegacy(
            ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> idi) {
        if (idi == null || idi.getDelta() == null || idi.getSubItemDeltas() != null) {
            return idi;
        }
        // Legacy approach (when adding/removing assignments) was: itemOld+itemNew = value, delta = null
        // This was recently changed, to provide precise information (add = null->itemNew, delete = itemOld->null).
        // However, to not break scripts before 3.6 release we provide imitation of old behavior here.
        // (Moreover, for magic assignment the delta is not correct anyway.)
        if (idi.getDelta().isAdd() || idi.getDelta().isReplace()) {
            var itemNew = idi.getItemNew();
            assert itemNew != null;
            return new ItemDeltaItem<>(itemNew.copy(), null, itemNew, idi.getDefinition());
        } else {
            var itemOld = idi.getItemOld();
            assert itemOld != null;
            return new ItemDeltaItem<>(itemOld, null, itemOld.copy(), idi.getDefinition());
        }
    }

    private static void mergeExtension(
            ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> destIdi,
            ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> srcIdi)
            throws SchemaException {
        mergeExtension(destIdi.getItemOld(), srcIdi.getItemOld());
        mergeExtension(destIdi.getItemNew(), srcIdi.getItemNew());
        if (srcIdi.getDelta() != null || srcIdi.getSubItemDeltas() != null) {
            throw new UnsupportedOperationException("Merge of IDI with deltas not supported");
        }
    }

    private static void mergeExtension(Item<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> dstItem,
            Item<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> srcItem) throws SchemaException {
        if (srcItem == null || dstItem == null) {
            return;
        }
        PrismContainer<Containerable> srcExtension = ((PrismContainer<AssignmentType>)srcItem).findContainer(AssignmentType.F_EXTENSION);
        mergeExtensionContainers(dstItem, srcExtension);
    }

    private static <O extends ObjectType> void mergeExtension(
            ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> destIdi,
            PrismObject<O> srcObject) throws SchemaException {
        if (srcObject == null) {
            return;
        }

        PrismContainer<Containerable> srcExtension = srcObject.findContainer(ObjectType.F_EXTENSION);

        mergeExtensionContainers(destIdi.getItemNew(), srcExtension);
        mergeExtensionContainers(destIdi.getItemOld(), srcExtension);
    }

    private static void mergeExtensionContainers(
            Item<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> dstItem,
            PrismContainer<Containerable> srcExtension) throws SchemaException {
        if (dstItem == null) {
            return;
        }
        PrismContainer<AssignmentType> dstContainer = (PrismContainer<AssignmentType>) dstItem;
        if (srcExtension != null && !srcExtension.isEmpty()) {
            PrismContainer<?> dstExtensionContainer = dstContainer.findOrCreateContainer(AssignmentType.F_EXTENSION);
            PrismContainerValue<?> dstExtensionContainerValue = dstExtensionContainer.getValues().isEmpty()
                    ? dstExtensionContainer.createNewValue() : dstExtensionContainer.getValue();
            ObjectTypeUtil.mergeExtension(dstExtensionContainerValue, srcExtension.getValue());
        }
    }

    public static <V extends PrismValue,D extends ItemDefinition<?>> MappingBuilder<V,D> addAssignmentPathVariables(
            MappingBuilder<V,D> builder, AssignmentPathVariables assignmentPathVariables) {
        VariablesMap variablesMap = new VariablesMap();
        ModelImplUtils.addAssignmentPathVariables(assignmentPathVariables, variablesMap);
        return builder.addVariableDefinitions(variablesMap);
    }

    public static VariablesMap getAssignmentPathVariablesMap(AssignmentPathVariables assignmentPathVariables) {
        VariablesMap variablesMap = new VariablesMap();
        ModelImplUtils.addAssignmentPathVariables(assignmentPathVariables, variablesMap);
        return variablesMap;
    }

    public static <F extends ObjectType> void checkContextSanity(LensContext<F> context, String activityDescription)
            throws SchemaException, PolicyViolationException {
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext != null) {
            PrismObject<F> focusObjectNew = focusContext.getObjectNew();
            if (focusObjectNew != null) {
                PolyStringType namePolyType = focusObjectNew.asObjectable().getName();
                if (namePolyType == null) {
                    throw new SchemaException("Focus "+focusObjectNew+" does not have a name after "+activityDescription);
                }
                ArchetypePolicyType archetypePolicy = focusContext.getArchetypePolicy();
                checkArchetypePolicy(focusContext, archetypePolicy);
            }
        }
    }

    private static <F extends ObjectType> void checkArchetypePolicy(LensFocusContext<F> focusContext, ArchetypePolicyType archetypePolicy) throws PolicyViolationException {
        if (archetypePolicy == null) {
            return;
        }
        PrismObject<F> focusObjectNew = focusContext.getObjectNew();
        ObjectDelta<F> focusDelta = focusContext.getSummaryDelta(); // todo check this

        for (ItemConstraintType itemConstraintType : archetypePolicy.getItemConstraint()) {
            processItemConstraint(focusContext, focusDelta, focusObjectNew, itemConstraintType);
        }
    }

    private static <F extends ObjectType> void processItemConstraint(LensFocusContext<F> focusContext, ObjectDelta<F> focusDelta, PrismObject<F> focusObjectNew, ItemConstraintType itemConstraintType) throws PolicyViolationException {
        ItemPath itemPath = itemConstraintType.getPath().getItemPath();
        if (BooleanUtils.isTrue(itemConstraintType.isOidBound())) {
            if (focusDelta != null) {
                if (focusDelta.isAdd()) {
                    PrismProperty<Object> propNew = focusObjectNew.findProperty(itemPath);
                    if (propNew != null) {
                        // prop delta is OK, but it has to match
                        if (focusObjectNew.getOid() != null) {
                            if (!focusObjectNew.getOid().equals(propNew.getRealValue().toString())) {
                                throw new PolicyViolationException("Cannot set "+itemPath+" to a value different than OID in oid bound mode");
                            }
                        }
                    }
                } else {
                    PropertyDelta<Object> nameDelta = focusDelta.findPropertyDelta(itemPath);
                    if (nameDelta != null) {
                        if (nameDelta.isReplace()) {
                            Collection<PrismPropertyValue<Object>> valuesToReplace = nameDelta.getValuesToReplace();
                            if (valuesToReplace.size() == 1) {
                                String stringValue = valuesToReplace.iterator().next().getValue().toString();
                                if (focusContext.getOid().equals(stringValue)) {
                                    // This is OK. It is most likely a correction made by a recompute.
                                    return;
                                }
                            }
                        }
                        throw new PolicyViolationException("Cannot change "+itemPath+" in oid bound mode");
                    }
                }
            }
        }

    }

    public static PrismContainer<AssignmentType> createAssignmentSingleValueContainer(@NotNull AssignmentType assignmentType) throws SchemaException {
        // Make it appear to be single-value. Therefore paths without segment IDs will work.
        return assignmentType.asPrismContainerValue().asSingleValuedContainer(SchemaConstantsGenerated.C_ASSIGNMENT);
    }

    public static AssignmentType getAssignmentType(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi, boolean old) {
        return PrismContainerValue.asContainerable(assignmentIdi.getSingleValue(old));
    }


    public static <F extends ObjectType> String getChannel(LensContext<F> context, Task task) {
        if (context != null && context.getChannel() != null){
            return context.getChannel();
        } else if (task.getChannel() != null){
            return task.getChannel();
        }
        return null;
    }

    public static <O extends ObjectType> void setDeltaOldValue(LensElementContext<O> ctx, ItemDelta<?,?> itemDelta) {
        if (itemDelta.getEstimatedOldValues() != null) {
            return;
        }
        if (ctx.getObjectOld() == null) {
            return;
        }
        Item<?, ?> itemOld = ctx.getObjectOld().findItem(itemDelta.getPath());
        if (itemOld != null) {
            //noinspection unchecked, rawtypes
            itemDelta.setEstimatedOldValuesWithCloning((Collection) itemOld.getValues());
            return;
        }
        // Here we need to distinguish whether the item is missing because it is not filled in (e.g. familyName in MID-4237)
        // or because it was not loaded (as for attributes or associations).
        if (!isItemLoadable(ctx.getObjectOld(), itemDelta.getPath())) {
            itemDelta.setEstimatedOldValues(emptySet());
            return;
        }
        // get the old data from current object. Still better estimate than nothing
        if (ctx.getObjectCurrent() != null) {
            itemOld = ctx.getObjectCurrent().findItem(itemDelta.getPath());
            if (itemOld != null) {
                //noinspection unchecked, rawtypes
                itemDelta.setEstimatedOldValuesWithCloning((Collection) itemOld.getValues());
            }
        }
    }

    // a heuristic by now
    private static <O extends ObjectType> boolean isItemLoadable(PrismObject<O> object, ItemPath path) {
        if (!(object.asObjectable() instanceof ShadowType)) {
            return false;
        }
        return path.startsWithName(ShadowType.F_ATTRIBUTES) || path.startsWithName(ShadowType.F_ASSOCIATIONS);
    }

    public static <O extends ObjectType> void setDeltaOldValue(LensElementContext<O> ctx, ObjectDelta<O> objectDelta) {
        if (objectDelta == null) {
            return;
        }
        if (!objectDelta.isModify()) {
            return;
        }
        for (ItemDelta<?, ?> modification: objectDelta.getModifications()) {
            setDeltaOldValue(ctx, modification);
        }
    }

    public static <T extends ObjectType> LensObjectDeltaOperation<T> createObjectDeltaOperation(
            ObjectDelta<T> objectDelta,
            OperationResult result,
            LensElementContext<T> objectContext,
            ResourceType resource) {
        LensObjectDeltaOperation<T> objectDeltaOp = new LensObjectDeltaOperation<>(objectDelta.clone());
        objectDeltaOp.setWave(objectContext.getLensContext().getExecutionWave());
        if (objectContext instanceof LensProjectionContext) {
            objectDeltaOp.setBaseObject(
                    CloneUtil.cloneCloneable(
                            asObjectable(objectContext.getObjectCurrent())));
        }
        objectDeltaOp.setExecutionResult(result);
        PrismObject<T> object = objectContext.getObjectAny();
        if (object != null) {
            PolyString name = object.getName();
            if (name == null && object.asObjectable() instanceof ShadowType) {
                try {
                    //noinspection unchecked
                    name = ShadowUtil.determineShadowName((PrismObject<ShadowType>) object);
                    if (name == null) {
                        LOGGER.debug("No name for shadow:\n{}", object.debugDump());
                    } else if (name.getNorm() == null) {
                        name.recompute(PrismContext.get().getDefaultPolyStringNormalizer());
                    }
                } catch (SchemaException e) {
                    LoggingUtils.logUnexpectedException(
                            LOGGER, "Couldn't determine name for shadow -- continuing with no name; shadow:\n{}",
                            e, object.debugDump());
                }
            }
            objectDeltaOp.setObjectName(name);
        }
        if (resource != null) {
            objectDeltaOp.setResourceOid(resource.getOid());
            objectDeltaOp.setResourceName(PolyString.toPolyString(resource.getName()));
            if (object != null && object.asObjectable() instanceof ShadowType shadow) {
                objectDeltaOp.setShadowKind(shadow.getKind());
                objectDeltaOp.setShadowIntent(shadow.getIntent());
            }
        } else if (objectContext instanceof LensProjectionContext ctx) {
            objectDeltaOp.setResourceOid(ctx.getResourceOid());
            objectDeltaOp.setShadowKind(ctx.getKind());
            objectDeltaOp.setShadowIntent(ctx.getKey().getIntent());
        }
        return objectDeltaOp;
    }

    public static void checkMaxIterations(
            int iteration, int maxIterations, String conflictMessage, SingleLocalizableMessage humanReadableReason)
            throws ObjectAlreadyExistsException {
        if (iteration > maxIterations) {
            throw new ObjectAlreadyExistsException(
                    new SingleLocalizableMessage(humanReadableReason.getKey(), humanReadableReason.getArgs(), conflictMessage));
        }
    }

    public static boolean needsFullShadowForCredentialProcessing(LensProjectionContext projCtx)
            throws SchemaException, ConfigurationException {
        ResourceObjectDefinition refinedProjDef = projCtx.getStructuralObjectDefinition();
        if (refinedProjDef == null) {
            return false;
        }

        for (MappingType mappingBean : refinedProjDef.getPasswordOutboundMappings()) {
            if (mappingBean.getStrength() == MappingStrengthType.STRONG || mappingBean.getStrength() == MappingStrengthType.WEAK) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPasswordReturnedByDefault(LensProjectionContext projCtx) {
        return CapabilityUtil.isPasswordReturnedByDefault(
                ResourceTypeUtil.getEnabledCapability(projCtx.getResource(), CredentialsCapabilityType.class));
    }

    public static <F extends ObjectType> void reclaimSequences(
            LensContext<F> context, RepositoryService repositoryService, OperationResult result)
            throws SchemaException {
        if (context == null) {
            return;
        }

        if (context.isSimulation()) {
            LOGGER.trace("We're in simulation mode, sequences don't have to be reclaimed");
            return;
        }

        Map<String, Long> sequenceMap = context.getSequences();
        LOGGER.trace("Context sequence map: {}", sequenceMap);
        for (Map.Entry<String, Long> sequenceMapEntry: sequenceMap.entrySet()) {
            Collection<Long> unusedValues = new ArrayList<>(1);
            unusedValues.add(sequenceMapEntry.getValue());
            try {
                LOGGER.trace("Returning value {} to sequence {}", sequenceMapEntry.getValue(), sequenceMapEntry.getKey());
                repositoryService.returnUnusedValuesToSequence(sequenceMapEntry.getKey(), unusedValues, result);
            } catch (ObjectNotFoundException e) {
                LOGGER.error("Cannot return unused value to sequence {}: it does not exist", sequenceMapEntry.getKey(), e);
                // ... but otherwise ignore it and go on
            }
        }
        context.getSequences().clear();
    }

    public static <AH extends AssignmentHolderType> void applyObjectPolicyConstraints(LensFocusContext<AH> focusContext, ArchetypePolicyType archetypePolicy) throws SchemaException {
        if (archetypePolicy == null) {
            return;
        }

        final PrismObject<AH> focusNew = focusContext.getObjectNew();
        if (focusNew == null) {
            // This is delete. Nothing to do.
            return;
        }

        for (ItemConstraintType itemConstraintType : archetypePolicy.getItemConstraint()) {
            applyObjectPolicyItemConstraint(focusContext, archetypePolicy, focusNew, itemConstraintType);
        }
    }

    private static <AH extends AssignmentHolderType> void applyObjectPolicyItemConstraint(LensFocusContext<AH> focusContext, ArchetypePolicyType archetypePolicy, PrismObject<AH> focusNew, ItemConstraintType itemConstraintType) throws SchemaException {
        if (itemConstraintType.getPath() == null) {
            LOGGER.error("Invalid configuration. Path is mandatory for property constraint definition in {} defined in system configuration", archetypePolicy);
            throw new SchemaException("Invalid configuration. Path is mandatory for property constraint definition in " + archetypePolicy + " defined in system configuration.");
        }
        ItemPath itemPath = itemConstraintType.getPath().getItemPath();
        if (BooleanUtils.isTrue(itemConstraintType.isOidBound())) {
            PrismProperty<Object> prop = focusNew.findProperty(itemPath);
            if (prop == null || prop.isEmpty()) {
                String newValue = focusNew.getOid();
                if (newValue == null) {
                    newValue = OidUtil.generateOid();
                }
                LOGGER.trace("Generating new OID-bound value for {}: {}", itemPath, newValue);
                PrismObjectDefinition<AH> focusDefinition = focusContext.getObjectDefinition();
                PrismPropertyDefinition<Object> propDef = focusDefinition.findPropertyDefinition(itemPath);
                if (propDef == null) {
                    throw new SchemaException("No definition for property "+itemPath+" in "+focusDefinition+" as specified in object policy");
                }
                PropertyDelta<Object> propDelta = propDef.createEmptyDelta(itemPath);
                if (String.class.isAssignableFrom(propDef.getTypeClass())) {
                    propDelta.setValueToReplace(PrismContext.get().itemFactory().createPropertyValue(newValue, OriginType.USER_POLICY, null));
                } else if (PolyString.class.isAssignableFrom(propDef.getTypeClass())) {
                    propDelta.setValueToReplace(PrismContext.get().itemFactory().createPropertyValue(new PolyString(newValue), OriginType.USER_POLICY, null));
                } else {
                    throw new SchemaException("Unsupported type "+propDef.getTypeName()+" for property "+itemPath+" in "+focusDefinition+" as specified in object policy, only string and polystring properties are supported for OID-bound mode");
                }
                focusContext.swallowToSecondaryDelta(propDelta);
            }
        }
    }

    public static LensContext.ExportType getExportType(TraceType trace, OperationResult result) {
        return result.isTracingNormal(trace.getClass()) ? LensContext.ExportType.TRACE : LensContext.ExportType.MINIMAL;
    }

    static LensContext.ExportType getExportTypeTraceOrReduced(TraceType trace, OperationResult result) {
        return result.isTracingNormal(trace.getClass()) ? LensContext.ExportType.TRACE : LensContext.ExportType.REDUCED;
    }

    public static <AH extends AssignmentHolderType> ItemDelta getAprioriItemDelta(ObjectDelta<AH> focusDelta, ItemPath itemPath) {
        return focusDelta != null ? focusDelta.findItemDelta(itemPath) : null;
    }

    @NotNull
    static Set<String> determineExplicitArchetypeOidsFromAssignments(AssignmentHolderType object) {
        return object.getAssignment().stream()
                .map(AssignmentType::getTargetRef)
                .filter(Objects::nonNull)
                .filter(ref -> QNameUtil.match(ArchetypeType.COMPLEX_TYPE, ref.getType()))
                .map(ObjectReferenceType::getOid)
                .collect(Collectors.toSet());
    }

    public static void rejectNonTolerantSettingIfPresent(ObjectTemplateItemDefinitionType templateItemDefinition,
            ItemPath itemPath, String contextDescription) {
        if (templateItemDefinition != null && Boolean.FALSE.equals(templateItemDefinition.isTolerant())) {
            throw new UnsupportedOperationException("The 'tolerant=false' setting on template items is no longer supported."
                    + " Please use mapping range instead. In '" + itemPath + "' consolidation in " + contextDescription);
        }
    }

    public static @NotNull PrismPropertyDefinition<Boolean> createConditionDefinition() {
        PrismPropertyDefinition<Boolean> booleanDefinition =
                PrismContext.get().definitionFactory()
                        .newPropertyDefinition(CONDITION_OUTPUT_NAME, DOMUtil.XSD_BOOLEAN);
        booleanDefinition.freeze();
        return booleanDefinition;
    }

    public static ShadowDiscriminatorType createDiscriminatorBean(ProjectionContextKey key, LensContext<?> lensContext) {
        if (key == null) {
            return null;
        }
        ShadowDiscriminatorType bean = key.toResourceShadowDiscriminatorType();
        provideResourceName(bean.getResourceRef(), lensContext);
        return bean;
    }

    private static void provideResourceName(ObjectReferenceType resourceRef, LensContext<?> lensContext) {
        if (resourceRef != null) {
            if (resourceRef.getTargetName() == null) {
                ResourceType resource = lensContext.getResource(resourceRef.getOid());
                if (resource != null) {
                    resourceRef.setTargetName(resource.getName());
                }
            }
        }
    }

    public static AssignmentType cloneResolveResource(AssignmentType assignmentBean, LensContext<?> lensContext) {
        if (assignmentBean == null) {
            return null;
        }
        AssignmentType clone = assignmentBean.clone();
        if (clone.getConstruction() != null) {
            provideResourceName(clone.getConstruction().getResourceRef(), lensContext);
        }
        return clone;
    }
}
