/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.focus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.util.ObjectTemplateIncludeProcessor;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.*;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.IvwoConsolidator;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.StrengthSelector;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import static com.evolveum.midpoint.model.impl.lens.projector.util.SkipWhenFocusDeleted.PRIMARY;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType.AFTER_ASSIGNMENTS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType.BEFORE_ASSIGNMENTS;

/**
 * Processor to handle object template.
 *
 * @author Radovan Semancik
 *
 */
@Component
@ProcessorExecution(focusRequired = true, focusType = FocusType.class, skipWhenFocusDeleted = PRIMARY)
public class ObjectTemplateProcessor implements ProjectorProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTemplateProcessor.class);

    @Autowired private PrismContext prismContext;
    @Autowired private ModelObjectResolver modelObjectResolver;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    @Autowired private MappingSetEvaluator mappingSetEvaluator;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;

    @ProcessorMethod
    <AH extends AssignmentHolderType> void processTemplateBeforeAssignments(LensContext<AH> context,
            XMLGregorianCalendar now, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException,
            SecurityViolationException, ConfigurationException, CommunicationException {
        processTemplate(context, BEFORE_ASSIGNMENTS, now, task, result);
    }

    @ProcessorMethod
    <AH extends AssignmentHolderType> void processTemplateAfterAssignments(LensContext<AH> context,
            XMLGregorianCalendar now, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException,
            SecurityViolationException, ConfigurationException, CommunicationException {
        processTemplate(context, AFTER_ASSIGNMENTS, now, task, result);
    }

    private <AH extends AssignmentHolderType> void processTemplate(LensContext<AH> context,
            ObjectTemplateMappingEvaluationPhaseType phase, XMLGregorianCalendar now, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException,
            SecurityViolationException, ConfigurationException, CommunicationException {
        LensFocusContext<AH> focusContext = context.getFocusContext();

        ObjectTemplateType objectTemplate = context.getFocusTemplate();
        String objectTemplateDesc;
        if (objectTemplate != null) {
            objectTemplateDesc = objectTemplate.toString();
        } else {
            objectTemplateDesc = "(no template)";
        }

        int iteration = focusContext.getIteration();
        String iterationToken = focusContext.getIterationToken();
        ObjectDeltaObject<AH> focusOdo = focusContext.getObjectDeltaObject();
        PrismObjectDefinition<AH> focusDefinition = getObjectDefinition(focusContext.getObjectTypeClass());

        LOGGER.trace("Applying object template {} to {}, iteration {} ({}), phase {}",
                objectTemplate, focusContext.getObjectNew(), iteration, iterationToken, phase);

        Map<UniformItemPath,ObjectTemplateItemDefinitionType> itemDefinitionsMap = collectItemDefinitionsFromTemplate(objectTemplate, objectTemplateDesc, task, result);
        focusContext.setItemDefinitionsMap(itemDefinitionsMap);

        List<FocalMappingEvaluationRequest<?, ?>> mappings = new ArrayList<>();
        collectMappingsFromTemplate(mappings, objectTemplate, objectTemplateDesc, task, result);
        collectAutoassignMappings(context, mappings, result);

        Map<UniformItemPath,DeltaSetTriple<? extends ItemValueWithOrigin<?,?>>> outputTripleMap = new HashMap<>();

        // TODO choose between these two
        //TargetObjectSpecification<AH> targetSpecification = new SelfTargetSpecification<>();
        TargetObjectSpecification<AH> targetSpecification = new FixedTargetSpecification<>(focusOdo.getNewObject());

        NextRecompute nextRecompute = mappingSetEvaluator.evaluateMappingsToTriples(context, mappings, phase, focusOdo,
                targetSpecification, outputTripleMap, null, null, iteration, iterationToken, now, task, result);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("outputTripleMap before item delta computation:\n{}", DebugUtil.debugDumpMapMultiLine(outputTripleMap));
        }

        String contextDesc = "object template "+objectTemplateDesc+ " for focus "+focusOdo.getAnyObject();
        Collection<ItemDelta<?,?>> itemDeltas = computeItemDeltas(outputTripleMap, itemDefinitionsMap, focusOdo.getObjectDelta(),
                focusOdo.getNewObject(), focusDefinition, contextDesc);

        focusContext.applyProjectionWaveSecondaryDeltas(itemDeltas);

        if (nextRecompute != null) {
            nextRecompute.createTrigger(focusContext);
        }

        focusContext.recompute();
    }

    /**
     * Processing object mapping: application of object template where focus is the source and another object is the target.
     * Used to map focus to personas.
     */
    public <F extends FocusType, T extends FocusType> Collection<ItemDelta<?,?>> processObjectMapping(LensContext<F> context,
            ObjectTemplateType objectMappingType, ObjectDeltaObject<F> focusOdo, PrismObject<T> target, ObjectDelta<T> targetAPrioriDelta,
            String contextDesc, XMLGregorianCalendar now, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException,
            SecurityViolationException, ConfigurationException, CommunicationException {
        LensFocusContext<F> focusContext = context.getFocusContext();

        int iteration = 0;
        String iterationToken = null;
        PrismObjectDefinition<F> focusDefinition = getObjectDefinition(focusContext.getObjectTypeClass());

        LOGGER.trace("Applying object mapping {} from {} to {}, iteration {} ({})",
                objectMappingType, focusContext.getObjectNew(), target, iteration, iterationToken);

        Map<UniformItemPath,ObjectTemplateItemDefinitionType> itemDefinitionsMap = collectItemDefinitionsFromTemplate(objectMappingType,
                objectMappingType.toString(), task, result);

        List<FocalMappingEvaluationRequest<?, ?>> mappings = new ArrayList<>();
        collectMappingsFromTemplate(mappings, objectMappingType, objectMappingType.toString(), task, result);
        collectAutoassignMappings(context, mappings, result);

        Map<UniformItemPath,DeltaSetTriple<? extends ItemValueWithOrigin<?,?>>> outputTripleMap = new HashMap<>();
        mappingSetEvaluator.evaluateMappingsToTriples(context, mappings, BEFORE_ASSIGNMENTS,
                focusOdo, new FixedTargetSpecification<>(target), outputTripleMap, null, null, iteration,
                iterationToken, now, task, result);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("outputTripleMap before item delta computation:\n{}", DebugUtil.debugDumpMapMultiLine(outputTripleMap));
        }

        return computeItemDeltas(outputTripleMap, itemDefinitionsMap, targetAPrioriDelta, target, focusDefinition, contextDesc);
    }

    @NotNull
    private Map<UniformItemPath, ObjectTemplateItemDefinitionType> collectItemDefinitionsFromTemplate(ObjectTemplateType objectTemplate, String contextDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Map<UniformItemPath, ObjectTemplateItemDefinitionType> definitions = new HashMap<>();
        if (objectTemplate != null) {
            new ObjectTemplateIncludeProcessor(modelObjectResolver)
                    .processThisAndIncludedTemplates(objectTemplate, contextDesc, task, result,
                            (includedTemplate) -> collectLocalItemDefinitions(includedTemplate, contextDesc, definitions));
        }
        return definitions;
    }

    private void collectLocalItemDefinitions(ObjectTemplateType objectTemplate, String contextDesc,
            Map<UniformItemPath, ObjectTemplateItemDefinitionType> definitions) {
        for (ObjectTemplateItemDefinitionType def : objectTemplate.getItem()) {
            if (def.getRef() == null) {
                throw new IllegalStateException("Item definition with null ref in " + contextDesc);
            }
            ItemPathCollectionsUtil.putToMap(definitions, prismContext.toUniformPath(def.getRef()), def);    // TODO check for incompatible overrides
        }
    }

    @SuppressWarnings("unchecked")
    <AH extends AssignmentHolderType, T extends AssignmentHolderType> Collection<ItemDelta<?,?>> computeItemDeltas(Map<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?,?>>> outputTripleMap,
            @Nullable Map<UniformItemPath,ObjectTemplateItemDefinitionType> itemDefinitionsMap,
            ObjectDelta<T> targetObjectAPrioriDelta, PrismObject<T> targetObject, PrismObjectDefinition<AH> focusDefinition, String contextDesc) throws ExpressionEvaluationException, PolicyViolationException, SchemaException {

        Collection<ItemDelta<?,?>> itemDeltas = new ArrayList<>();

        LOGGER.trace("Computing deltas in {}, focusDelta:\n{}", contextDesc, targetObjectAPrioriDelta);

        boolean addUnchangedValues = false;
        if (targetObjectAPrioriDelta != null && targetObjectAPrioriDelta.isAdd()) {
            addUnchangedValues = true;
        }

        for (Entry<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?,?>>> entry: outputTripleMap.entrySet()) {
            UniformItemPath itemPath = entry.getKey();
            boolean isAssignment = SchemaConstants.PATH_ASSIGNMENT.equivalent(itemPath);
            DeltaSetTriple<? extends ItemValueWithOrigin<?,?>> outputTriple = entry.getValue();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Computed triple for {}:\n{}", itemPath, outputTriple.debugDump());
            }
            final ObjectTemplateItemDefinitionType templateItemDefinition;
            if (itemDefinitionsMap != null) {
                templateItemDefinition = ItemPathCollectionsUtil.getFromMap(itemDefinitionsMap, itemPath);
            } else {
                templateItemDefinition = null;
            }
            boolean isNonTolerant = templateItemDefinition != null && Boolean.FALSE.equals(templateItemDefinition.isTolerant());

            ItemDelta aprioriItemDelta = LensUtil.getAprioriItemDelta(targetObjectAPrioriDelta, itemPath);

            IvwoConsolidator consolidator = new IvwoConsolidator<>();
            consolidator.setItemPath(itemPath);
            consolidator.setIvwoTriple(outputTriple);
            consolidator.setItemDefinition(focusDefinition.findItemDefinition(itemPath));
            consolidator.setAprioriItemDelta(aprioriItemDelta);
            consolidator.setItemContainer(targetObject);
            consolidator.setValueMatcher(null);
            consolidator.setComparator(null);
            consolidator.setAddUnchangedValues(addUnchangedValues);
            consolidator.setFilterExistingValues(!isNonTolerant); // if non-tolerant, we want to gather ZERO & PLUS sets
            consolidator.setExclusiveStrong(false);
            consolidator.setContextDescription(contextDesc);
            consolidator.setStrengthSelector(StrengthSelector.ALL);

            @NotNull ItemDelta itemDelta = consolidator.consolidateToDelta();

            // Do a quick version of reconciliation. There is not much to reconcile as both the source and the target
            // is focus. But there are few cases to handle, such as strong mappings, and sourceless normal mappings.
            Collection<? extends ItemValueWithOrigin<?,?>> zeroSet = outputTriple.getZeroSet();
            Item<PrismValue, ItemDefinition> itemNew = null;
            if (targetObject != null) {
                itemNew = targetObject.findItem(itemPath);
            }
            for (ItemValueWithOrigin<?,?> zeroSetIvwo: zeroSet) {

                PrismValueDeltaSetTripleProducer<?, ?> mapping = zeroSetIvwo.getMapping();
                if (mapping.getStrength() == null || mapping.getStrength() == MappingStrengthType.NORMAL) {
                    if (aprioriItemDelta != null && !aprioriItemDelta.isEmpty()) {
                        continue;
                    }
                    if (!mapping.isSourceless()) {
                        continue;
                    }
                    LOGGER.trace("Adding zero values from normal mapping {}, a-priori delta: {}, isSourceless: {}",
                            mapping, aprioriItemDelta, mapping.isSourceless());
                } else if (mapping.getStrength() == MappingStrengthType.WEAK) {
                    if (itemNew != null && !itemNew.isEmpty() || itemDelta.addsAnyValue()) {
                        continue;
                    }
                    LOGGER.trace("Adding zero values from weak mapping {}, itemNew: {}, itemDelta: {}",
                            mapping, itemNew, itemDelta);
                } else {
                    LOGGER.trace("Adding zero values from strong mapping {}", mapping);
                }

                PrismValue valueFromZeroSet = zeroSetIvwo.getItemValue();
                if (itemNew == null || !itemNew.contains(valueFromZeroSet, EquivalenceStrategy.REAL_VALUE)) {
                    LOGGER.trace("Reconciliation will add value {} for item {}. Existing item: {}", valueFromZeroSet, itemPath, itemNew);
                    itemDelta.addValuesToAdd(LensUtil.cloneAndApplyMetadata(valueFromZeroSet, isAssignment, mapping));
                }
            }

            if (isNonTolerant) {
                if (itemDelta.isDelete()) {
                    LOGGER.trace("Non-tolerant item with values to DELETE => removing them");
                    itemDelta.resetValuesToDelete();
                }
                if (itemDelta.isReplace()) {
                    LOGGER.trace("Non-tolerant item with resulting REPLACE delta => doing nothing");
                } else {
                    for (ItemValueWithOrigin<?,?> zeroSetIvwo: zeroSet) {
                        // TODO aren't values added twice (regarding addValuesToAdd called ~10 lines above)?
                        itemDelta.addValuesToAdd(LensUtil.cloneAndApplyMetadata(zeroSetIvwo.getItemValue(), isAssignment, zeroSetIvwo.getMapping()));
                    }
                    itemDelta.addToReplaceDelta();
                    LOGGER.trace("Non-tolerant item with resulting ADD delta => converted ADD to REPLACE values: {}", itemDelta.getValuesToReplace());
                }
                // To avoid phantom changes, compare with existing values (MID-2499).
                // TODO why we do this check only for non-tolerant items?
                if (isDeltaRedundant(targetObject, templateItemDefinition, itemDelta)) {
                    LOGGER.trace("Computed item delta is redundant => skipping it. Delta = \n{}", itemDelta.debugDumpLazily());
                    continue;
                }
                PrismUtil.setDeltaOldValue(targetObject, itemDelta);
            }

            itemDelta.simplify();
            itemDelta.validate(contextDesc);
            itemDeltas.add(itemDelta);
            LOGGER.trace("Computed delta:\n{}", itemDelta.debugDumpLazily());
        }
        return itemDeltas;
    }

    // TODO this should be maybe moved into LensUtil.consolidateTripleToDelta e.g.
    //  under a special option "createReplaceDelta", but for the time being, let's keep it here
    private <T extends AssignmentHolderType> boolean isDeltaRedundant(PrismObject<T> targetObject,
            ObjectTemplateItemDefinitionType templateItemDefinition, @NotNull ItemDelta<?, ?> itemDelta)
            throws SchemaException {
        if (itemDelta instanceof PropertyDelta) {
            QName matchingRuleName = templateItemDefinition != null ? templateItemDefinition.getMatchingRule() : null;
            return isPropertyDeltaRedundant(targetObject, matchingRuleName, (PropertyDelta<?>) itemDelta);
        } else {
            return itemDelta.isRedundant(targetObject, false);
        }
    }

    private <AH extends AssignmentHolderType, T> boolean isPropertyDeltaRedundant(PrismObject<AH> targetObject,
            QName matchingRuleName, @NotNull PropertyDelta<T> propertyDelta)
            throws SchemaException {
        MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleName, null);
        return propertyDelta.isRedundant(targetObject, EquivalenceStrategy.IGNORE_METADATA, matchingRule, false);
    }

    private void collectMappingsFromTemplate(List<FocalMappingEvaluationRequest<?, ?>> mappings,
            ObjectTemplateType objectTemplate, String contextDesc, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException {
        if (objectTemplate == null) {
            return;
        }
        LOGGER.trace("Collecting mappings from {}", objectTemplate);

        new ObjectTemplateIncludeProcessor(modelObjectResolver)
                .processThisAndIncludedTemplates(objectTemplate, contextDesc, task, result,
                        (includedTemplate -> collectMappings(mappings, includedTemplate)));
    }

    private void collectMappings(List<FocalMappingEvaluationRequest<?, ?>> mappings, ObjectTemplateType objectTemplateType) {
        for (ObjectTemplateMappingType mapping: objectTemplateType.getMapping()) {
            mappings.add(new TemplateMappingEvaluationRequest(mapping, objectTemplateType));
        }
        for (ObjectTemplateItemDefinitionType templateItemDefType: objectTemplateType.getItem()) {
            for (ObjectTemplateMappingType mapping: templateItemDefType.getMapping()) {
                setMappingTarget(mapping, templateItemDefType.getRef());
                mappings.add(new TemplateMappingEvaluationRequest(mapping, objectTemplateType));
            }
        }
    }

    private <AH extends AssignmentHolderType> void collectAutoassignMappings(LensContext<AH> context,
            List<FocalMappingEvaluationRequest<?, ?>> mappings, OperationResult result) throws SchemaException {

        if (!autoassignEnabled(context.getSystemConfiguration())) {
            return;
        }

        ObjectQuery query = prismContext
                .queryFor(AbstractRoleType.class)
                .item(SchemaConstants.PATH_AUTOASSIGN_ENABLED)
                .eq(true)
                .build();

        ResultHandler<AbstractRoleType> handler = (role, objectResult) -> {
            AutoassignSpecificationType autoassign = role.asObjectable().getAutoassign();
            if (autoassign == null) {
                return true;
            }
            if (!BooleanUtils.isTrue(autoassign.isEnabled())) {
                return true;
            }
            FocalAutoassignSpecificationType focalAutoassignSpec = autoassign.getFocus();
            if (focalAutoassignSpec == null) {
                return true;
            }

            if (!isApplicableFor(focalAutoassignSpec.getSelector(), context.getFocusContext(), objectResult)) {
                return true;
            }

            for (AutoassignMappingType autoMapping: focalAutoassignSpec.getMapping()) {
                AutoassignMappingType mapping = autoMapping.clone();
                setMappingTarget(mapping, new ItemPathType(SchemaConstants.PATH_ASSIGNMENT));
                mappings.add(new AutoassignRoleMappingEvaluationRequest(mapping, role.asObjectable()));
                LOGGER.trace("Collected autoassign mapping {} from {}", mapping.getName(), role);
            }
            return true;
        };
        cacheRepositoryService.searchObjectsIterative(AbstractRoleType.class, query, handler, GetOperationOptions.createReadOnlyCollection(), true, result);
    }

    private <AH extends AssignmentHolderType> boolean isApplicableFor(ObjectSelectorType selector, LensFocusContext<AH> focusContext, OperationResult result) {
        if (selector == null) {
            return true;
        }
        try {
            return cacheRepositoryService.selectorMatches(selector, focusContext.getObjectAny(), null, LOGGER, "");
        } catch (SchemaException | SecurityViolationException | ExpressionEvaluationException | CommunicationException | ObjectNotFoundException | ConfigurationException e) {
            LOGGER.error("Failed to evaluate selector constraints, selector {}, focusContext {}\nReason: {}", selector, focusContext, e.getMessage(), e);
            result.recordFatalError("Failed to evaluate selector constrains, selector: " + selector + ", focusContext: " + focusContext + "\nReason: " + e.getMessage(), e);
            throw new SystemException(e);
        }
    }

    private void setMappingTarget(MappingType mapping, ItemPathType path) {
        VariableBindingDefinitionType target = mapping.getTarget();
        if (target == null) {
            target = new VariableBindingDefinitionType();
            target.setPath(path);
            mapping.setTarget(target);
        } else if (target.getPath() == null) {
            target = target.clone();
            target.setPath(path);
            mapping.setTarget(target);
        }
    }

    private boolean autoassignEnabled(PrismObject<SystemConfigurationType> systemConfiguration) {
        if (systemConfiguration == null) {
            return false;
        }
        RoleManagementConfigurationType roleManagement = systemConfiguration.asObjectable().getRoleManagement();
        if (roleManagement == null) {
            return false;
        }
        return BooleanUtils.isTrue(roleManagement.isAutoassignEnabled());
    }

    private <F extends ObjectType> PrismObjectDefinition<F> getObjectDefinition(Class<F> focusClass) {
        return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusClass);
    }
}
