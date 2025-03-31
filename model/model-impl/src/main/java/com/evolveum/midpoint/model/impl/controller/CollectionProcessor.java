/*
 * Copyright (C) 2019-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule.TargetType;
import com.evolveum.midpoint.schema.config.AbstractAssignmentConfigItem;
import com.evolveum.midpoint.schema.config.AssignmentConfigItem;
import com.evolveum.midpoint.schema.config.OriginProvider;
import com.evolveum.midpoint.schema.config.PolicyRuleConfigItem;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.CollectionStats;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.context.EvaluatedCollectionStatsTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathImpl;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathSegmentImpl;
import com.evolveum.midpoint.model.impl.lens.assignments.ConditionState;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluationOrderImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author semancik
 */
@Component
public class CollectionProcessor {

    private static final String CONSTRAINT_KEY = "collectionStatsConstraint";

    private static final Trace LOGGER = TraceManager.getTrace(CollectionProcessor.class);

    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private ModelService modelService;
    @Autowired @Qualifier("modelObjectResolver") private ObjectResolver objectResolver;
    @Autowired private ArchetypeManager archetypeManager;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private SchemaService schemaService;

    Collection<EvaluatedPolicyRule> evaluateCollectionPolicyRules(
            PrismObject<ObjectCollectionType> collection, // [EP:APSO] DONE 1/1
            CompiledObjectCollectionView preCompiledCollectionView,
            Class<? extends ObjectType> targetTypeClass,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        ObjectCollectionType collectionBean = collection.asObjectable();
        CompiledObjectCollectionView collectionView;
        if (preCompiledCollectionView != null) {
            collectionView = preCompiledCollectionView;
        } else {
            collectionView = new CompiledObjectCollectionView();
            compileObjectCollectionView(
                    collectionView, null, collectionBean, targetTypeClass, task, result);
        }

        Collection<EvaluatedPolicyRule> evaluatedPolicyRules = new ArrayList<>();
        for (AssignmentType assignmentBean : collectionBean.getAssignment()) {
            PolicyRuleType policyRuleBean = assignmentBean.getPolicyRule();
            if (policyRuleBean != null) {
                evaluatedPolicyRules.add(
                        evaluatePolicyRule(
                                collection,
                                collectionView,
                                // [EP:APSO] DONE, as the collection is taken from repository (see call chain upwards)^
                                AssignmentConfigItem.of(assignmentBean, OriginProvider.embedded()),
                                task, result));
            }
        }
        return evaluatedPolicyRules;
    }

    /**
     * Very simple implementation, needs to be extended later.
     * Assumes the assignment has a policy rule.
     */
    @NotNull
    private EvaluatedPolicyRule evaluatePolicyRule(
            @NotNull PrismObject<ObjectCollectionType> collection,
            @NotNull CompiledObjectCollectionView collectionView,
            @NotNull AbstractAssignmentConfigItem assignmentCI, // [EP:APSO] DONE 1/1
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            ConfigurationException, CommunicationException, ExpressionEvaluationException {
        AssignmentPathImpl assignmentPath = new AssignmentPathImpl();
        AssignmentPathSegmentImpl assignmentPathSegment = new AssignmentPathSegmentImpl.Builder()
                .source(collection.asObjectable())
                .sourceDescription("object collection " + collection)
                .assignment(assignmentCI.value())
                .assignmentOrigin(assignmentCI.origin()) // [EP:APSO] DONE
                .isAssignment()
                .evaluationOrder(EvaluationOrderImpl.zero(relationRegistry))
                .evaluationOrderForTarget(EvaluationOrderImpl.zero(relationRegistry))
                // direct=true is to be reconsidered - but assignment path is empty, so we consider this to be directly assigned
                .direct(true)
                .pathToSourceValid(true)
                .pathToSourceConditionState(ConditionState.allTrue())
                .build();
        assignmentPath.add(assignmentPathSegment);

        // Generated proforma - actually not much needed for now.
        String ruleId = PolicyRuleTypeUtil.createId(collection.getOid(), assignmentCI.value().getId());

        PolicyRuleConfigItem policyRule = Objects.requireNonNull(assignmentCI.getPolicyRule());
        EvaluatedPolicyRuleImpl evaluatedPolicyRule = // TODO why cloning here?
                new EvaluatedPolicyRuleImpl(policyRule.clone(), ruleId, assignmentPath, TargetType.OBJECT);

        PolicyConstraintsType policyConstraints = policyRule.value().getPolicyConstraints();
        if (policyConstraints == null) {
            return evaluatedPolicyRule;
        }

        PolicyThresholdType policyThreshold = policyRule.value().getPolicyThreshold();

        for (CollectionStatsPolicyConstraintType collectionStatsPolicy : policyConstraints.getCollectionStats()) {
            CollectionStats stats = determineCollectionStats(collectionView, task, result);
            if (isThresholdTriggered(stats, collection, policyThreshold)) {
                EvaluatedPolicyRuleTrigger<?> trigger = new EvaluatedCollectionStatsTrigger(
                        PolicyConstraintKindType.COLLECTION_STATS, collectionStatsPolicy,
                        new LocalizableMessageBuilder()
                                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY)
                                .arg(ObjectTypeUtil.createDisplayInformation(collection, false))
                                .args(/* TODO */)
                                .build(),
                        new LocalizableMessageBuilder()
                                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY)
                                .arg(ObjectTypeUtil.createDisplayInformation(collection, false))
                                .args(/* TODO */)
                                .build());
                evaluatedPolicyRule.addTrigger(trigger);
            }
        }

        return evaluatedPolicyRule;
    }

    private boolean isThresholdTriggered(CollectionStats stats, PrismObject<ObjectCollectionType> collection, PolicyThresholdType policyThreshold) throws ExpressionEvaluationException {
        if (policyThreshold == null) {
            LOGGER.trace("Rule triggered on {} because there is no threshold specification", collection);
            return true;
        }

        WaterMarkType highWaterMark = policyThreshold.getHighWaterMark();
        if (highWaterMark != null) {
            Integer waterMarkCount = highWaterMark.getCount();
            if (waterMarkCount != null) {
                if (stats.getObjectCount() > waterMarkCount) {
                    LOGGER.trace("Rule NOT triggered on {} because high watermark count exceeded (watermark: {}, actual: {})", collection, waterMarkCount, stats.getObjectCount());
                    return false;
                }
            }
            Float waterMarkPercentage = highWaterMark.getPercentage();
            if (waterMarkPercentage != null) {
                Float percentage = stats.computePercentage();
                if (percentage == null) {
                    throw new ExpressionEvaluationException("Cannot determine percentage of " + collection);
                }
                if (percentage > waterMarkPercentage) {
                    LOGGER.trace("Rule NOT triggered on {} because high watermark percentage exceeded (watermark: {}, actual: {})", collection, waterMarkPercentage, percentage);
                    return false;
                }
            }
        }

        WaterMarkType lowWaterMark = policyThreshold.getLowWaterMark();
        if (lowWaterMark != null) {
            Integer waterMarkCount = lowWaterMark.getCount();
            if (waterMarkCount != null) {
                if (stats.getObjectCount() < waterMarkCount) {
                    LOGGER.trace("Rule NOT triggered on {} because low watermark count not reached (watermark: {}, actual: {})", collection, waterMarkCount, stats.getObjectCount());
                    return false;
                }
            }
            Float waterMarkPercentage = lowWaterMark.getPercentage();
            if (waterMarkPercentage != null) {
                Float percentage = stats.computePercentage();
                if (percentage == null) {
                    throw new ExpressionEvaluationException("Cannot determine percentage of " + collection);
                }
                if (percentage < waterMarkPercentage) {
                    LOGGER.trace("Rule NOT triggered on {} because low watermark percentage not reached (watermark: {}, actual: {})", collection, waterMarkPercentage, percentage);
                    return false;
                }
            }
        }

        LOGGER.trace("Rule triggered on {}, thresholds reached: {}", collection, stats);
        return true;
    }

    <O extends ObjectType> CollectionStats determineCollectionStats(CompiledObjectCollectionView collectionView, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
        CollectionStats stats = new CollectionStats();
        Class<O> targetClass = collectionView.getTargetClass();
        stats.setObjectCount(countObjects(targetClass, evaluateExpressionsInFilter(collectionView.getFilter(), result, task), collectionView.getOptions(), task, result));
        stats.setDomainCount(countObjects(targetClass, evaluateExpressionsInFilter(collectionView.getDomainFilter(), result, task), collectionView.getDomainOptions(), task, result));
        return stats;
    }

    private <O extends ObjectType> Integer countObjects(Class<O> targetTypeClass, ObjectFilter filter, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
        if (filter == null) {
            return null;
        }
        return modelService.countObjects(targetTypeClass, prismContext.queryFactory().createQuery(filter), options, task, result);
    }

    CompiledObjectCollectionView compileObjectCollectionView(CollectionRefSpecificationType collectionRef,
            Class<? extends Containerable> targetTypeClass, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        CompiledObjectCollectionView view = new CompiledObjectCollectionView();
        compileObjectCollectionView(view, collectionRef, targetTypeClass, task, result);
        return view;
    }

    private void compileObjectCollectionView(
            CompiledObjectCollectionView existingView,
            CollectionRefSpecificationType collectionSpec,
            Class<?> targetTypeClass,
            Task task,
            OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {

        ObjectReferenceType collectionRef = collectionSpec.getCollectionRef();

        if (collectionRef != null && collectionRef.getOid() != null && collectionSpec.getFilter() != null) {
            throw new IllegalArgumentException(
                    "CollectionRefSpecificationType contains CollectionRef and Filter, please define only one");
        }

        if (collectionRef != null && collectionRef.getOid() != null) {
            QName collectionRefType = collectionRef.getType();

            // TODO: support more cases
            if (QNameUtil.match(ArchetypeType.COMPLEX_TYPE, collectionRefType)) {
                compileArchetypeCollectionView(existingView, targetTypeClass, collectionRef.getOid(), collectionSpec.getBaseCollectionRef(), task, result);
                return;
            }

            if (QNameUtil.match(ObjectCollectionType.COMPLEX_TYPE, collectionRefType)) {
                ObjectCollectionType objectCollectionType;
                try {
                    // TODO: caching?
                    objectCollectionType = objectResolver.resolve(collectionRef, ObjectCollectionType.class, null, "view " + existingView.getViewIdentifier(), task, result);
                } catch (ObjectNotFoundException e) {
                    throw new ConfigurationException(e.getMessage(), e);
                }
                compileObjectCollectionView(existingView, collectionSpec.getBaseCollectionRef(), objectCollectionType,
                        targetTypeClass, task, result);
                return;
            }

            // TODO
            throw new IllegalArgumentException("Unsupported collection type: " + collectionRefType);

        } else if (collectionSpec.getFilter() != null) {

            SearchFilterType filter = collectionSpec.getFilter();

            CollectionRefSpecificationType baseCollectionSpec = collectionSpec.getBaseCollectionRef();
            if (baseCollectionSpec == null) {
                if (targetTypeClass == null) {
                    throw new IllegalArgumentException("UndefinedTypeForCollection type: " + collectionSpec);
                }
                ObjectFilter objectFilter = prismContext.getQueryConverter().parseFilter(filter, targetTypeClass);
                existingView.setFilter(objectFilter);
            } else {
                compileBaseCollectionSpec(filter, existingView, baseCollectionSpec, targetTypeClass, task, result);
            }
        } else {
            // E.g. the case of empty domain specification. Nothing to do. Just return what we have.
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    private void compileArchetypeCollectionView(
            CompiledObjectCollectionView existingView,
            Class<?> targetTypeClass,
            String collectionRefOid,
            CollectionRefSpecificationType baseCollectionRef,
            Task task,
            OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        RefFilter archetypeFilter = (RefFilter) prismContext.queryFor(AssignmentHolderType.class)
                .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(collectionRefOid)
                .buildFilter();
        archetypeFilter.setTargetTypeNullAsAny(true);

        if (baseCollectionRef != null) {
            compileBaseCollectionSpec(archetypeFilter, existingView, null, baseCollectionRef, targetTypeClass, task, result);
        } else {
            existingView.setFilter(archetypeFilter);
        }

        try {
            ArchetypeType archetype = archetypeManager.getArchetype(collectionRefOid, result);
            ArchetypePolicyType archetypePolicy = archetype.getArchetypePolicy();
            if (archetypePolicy != null) {
                DisplayType archetypeDisplay = archetypePolicy.getDisplay();
                if (archetypeDisplay != null) {
                    DisplayType viewDisplay = existingView.getDisplay();
                    if (viewDisplay == null) {
                        viewDisplay = new DisplayType();
                        existingView.setDisplay(viewDisplay);
                    }
                    MiscSchemaUtil.mergeDisplay(viewDisplay, archetypeDisplay);
                }
            }

            if (targetTypeClass == null) {
                // this should be currently case with widgets
                Set<QName> holderTypes = archetype.getAssignment().stream()
                        .map(a -> a.getAssignmentRelation())
                        .flatMap(Collection::stream)
                        .map(ar -> ar.getHolderType())
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
                if (holderTypes.size() == 1) {
                    existingView.setContainerType(holderTypes.iterator().next());
                } else {
                    existingView.setContainerType(AssignmentHolderType.COMPLEX_TYPE);
                }
            }
        } catch (ObjectNotFoundException e) {
            // We do not want to throw exception here. This code takes place at login time.
            // We do not want to stop all logins because of missing archetype.
            LOGGER.warn("Archetype {} referenced from view {} was not found", collectionRefOid, existingView.getViewIdentifier());
        }
    }

    private Class<? extends Containerable> getContainerTypeClass(QName targetTypeQName, ObjectCollectionType objectCollectionType) throws SchemaException {
        if (targetTypeQName == null) {
            throw new SchemaException("Target container type not specified in " + objectCollectionType);
        }
        PrismContainerDefinition<Containerable> def = prismContext.getSchemaRegistry().findContainerDefinitionByType(targetTypeQName);
        if (def == null) {
            throw new IllegalArgumentException("Unsupported container type " + targetTypeQName);
        }
        return def.getTypeClass();
    }

    private void compileObjectCollectionView(CompiledObjectCollectionView existingView, CollectionRefSpecificationType baseCollectionSpec,
            @NotNull ObjectCollectionType objectCollectionType, Class<?> targetTypeClass, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {

        if (targetTypeClass == null) {
            if (existingView.getContainerType() == null) {
                QName targetTypeQName = objectCollectionType.getType();
                targetTypeClass = getContainerTypeClass(targetTypeQName, objectCollectionType);
                existingView.setContainerType(targetTypeQName);
            } else {
                QName targetTypeQName = existingView.getContainerType();
                targetTypeClass = getContainerTypeClass(targetTypeQName, objectCollectionType);
            }
        }

        // Used for help text in search panel
        if (objectCollectionType.getDescription() != null) {
            existingView.setObjectCollectionDescription(objectCollectionType.getDescription());
        }

        // Do this before we compile main filer. We want domain specification from the first (lowest, most specific) collection in the
        // hierarchy. It makes no sense to compile all domain specs from the entire hierarchy just to throw that out.
        if (!existingView.hasDomain()) {
            CollectionRefSpecificationType domainSpec = objectCollectionType.getDomain();
            if (domainSpec != null) {
                CompiledObjectCollectionView domainView = new CompiledObjectCollectionView();
                compileObjectCollectionView(domainView, domainSpec, targetTypeClass, task, result);
                if (domainView.getFilter() == null) {
                    // We have domain specification, but compilation produced no filter. Which means that the domain is "all"
                    // Explicitly set "all" filter here. We want to avoid confusion between no domain spec and "all" domain spec.
                    existingView.setDomainFilter(prismContext.queryFactory().createAll());
                } else {
                    existingView.setDomainFilter(domainView.getFilter());
                }
                existingView.setDomainOptions(domainView.getOptions());
            }
        }

        SearchFilterType collectionFilterType = objectCollectionType.getFilter();
        ObjectFilter collectionFilter;
        if (collectionFilterType != null) {
            collectionFilter = prismContext.getQueryConverter().parseFilter(collectionFilterType, targetTypeClass);
        } else {
            collectionFilter = null;
        }
        List<SelectorOptions<GetOperationOptions>> collectionOptions =
                GetOperationOptionsUtil.optionsBeanToOptions(objectCollectionType.getGetOptions());
        CollectionRefSpecificationType baseCollectionSpecFromCollection = objectCollectionType.getBaseCollection();
        if (baseCollectionSpecFromCollection == null && baseCollectionSpec == null) {
            existingView.setFilter(collectionFilter);
            existingView.setOptions(collectionOptions);
        } else {
            if (baseCollectionSpecFromCollection == null || baseCollectionSpec == null) {
                compileBaseCollectionSpec(collectionFilter, existingView, collectionOptions,
                        baseCollectionSpec == null ? baseCollectionSpecFromCollection : baseCollectionSpec, targetTypeClass, task, result);
            } else {
                compileObjectCollectionView(existingView, baseCollectionSpecFromCollection, targetTypeClass, task, result);
                ObjectFilter baseFilterFromCollection = existingView.getFilter();
                Collection<SelectorOptions<GetOperationOptions>> baseOptionFromCollection = existingView.getOptions();
                compileObjectCollectionView(existingView, baseCollectionSpec, targetTypeClass, task, result);
                ObjectFilter baseFilter = existingView.getFilter();
                ObjectFilter combinedFilter = ObjectQueryUtil.filterAnd(baseFilterFromCollection, baseFilter);
                combinedFilter = ObjectQueryUtil.filterAnd(combinedFilter, collectionFilter);
                existingView.setFilter(combinedFilter);
                GetOperationOptionsBuilder optionsBuilder = schemaService.getOperationOptionsBuilder().setFrom(baseOptionFromCollection);
                optionsBuilder.mergeFrom(existingView.getOptions());
                optionsBuilder.mergeFrom(collectionOptions);
                existingView.setOptions(optionsBuilder.build());
            }
        }

        compileView(existingView, objectCollectionType.getDefaultView(), false);

    }

    private void compileBaseCollectionSpec(
            ObjectFilter objectFilter,
            CompiledObjectCollectionView existingView,
            Collection<SelectorOptions<GetOperationOptions>> options,
            CollectionRefSpecificationType baseCollectionRef,
            Class<?> targetTypeClass,
            Task task,
            OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        compileObjectCollectionView(existingView, baseCollectionRef, targetTypeClass, task, result);
        mergeCollectionFilterAndOptions(objectFilter, existingView, options);
    }

    private void compileBaseCollectionSpec(
            SearchFilterType filter,
            CompiledObjectCollectionView existingView,
            CollectionRefSpecificationType baseCollectionRef,
            Class<?> explicitTargetTypeClass,
            Task task,
            OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (explicitTargetTypeClass != null) {
            ObjectFilter objectFilter = prismContext.getQueryConverter().parseFilter(filter, explicitTargetTypeClass);
            compileBaseCollectionSpec(
                    objectFilter, existingView, null, baseCollectionRef, explicitTargetTypeClass, task, result);
        } else {
            compileObjectCollectionView(existingView, baseCollectionRef, null, task, result);
            var targetTypeClass = existingView.getTargetClass();
            if (targetTypeClass == null) {
                throw new IllegalArgumentException("UndefinedTypeForCollection type: " + baseCollectionRef);
            }
            ObjectFilter objectFilter = prismContext.getQueryConverter().parseFilter(filter, targetTypeClass);
            mergeCollectionFilterAndOptions(objectFilter, existingView, null);
        }
    }

    private void mergeCollectionFilterAndOptions(ObjectFilter objectFilter, CompiledObjectCollectionView existingView, Collection<SelectorOptions<GetOperationOptions>> options) {
        ObjectFilter baseFilter = existingView.getFilter();
        ObjectFilter combinedFilter = ObjectQueryUtil.filterAnd(baseFilter, objectFilter);
        existingView.setFilter(combinedFilter);
        GetOperationOptionsBuilder optionsBuilder = schemaService.getOperationOptionsBuilder().setFrom(existingView.getOptions());
        optionsBuilder.mergeFrom(options);
        existingView.setOptions(optionsBuilder.build());
    }

    void compileView(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        compileView(existingView, objectListViewType, true);
    }

    public void compileView(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        compileView(existingView, objectListViewType);
        compileCollection(existingView, objectListViewType, task, result);
    }

    private void compileCollection(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        CollectionRefSpecificationType collectionSpec = objectListViewType.getCollection();
        if (collectionSpec == null) {
            return;
        }
        if (existingView.getCollection() != null) {
            LOGGER.debug("Redefining collection in view {}", existingView.getViewIdentifier());
        }
        existingView.setCollection(collectionSpec);

        compileCollection(existingView, collectionSpec, task, result);
    }

    private void compileCollection(CompiledObjectCollectionView existingView, CollectionRefSpecificationType collectionSpec, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {

        QName targetObjectType = existingView.getContainerType();
        Class<?> targetTypeClass = ObjectType.class;
        if (targetObjectType != null) {
            if (QNameUtil.match(targetObjectType, ObjectReferenceType.COMPLEX_TYPE)) {
                targetTypeClass = ObjectReferenceType.class;
            } else {
                PrismContainerDefinition<? extends Containerable> containerDefinition = prismContext.getSchemaRegistry().findContainerDefinitionByType(targetObjectType);
                if (containerDefinition != null) {
                    targetTypeClass = containerDefinition.getTypeClass();
                }
            }
        }
        compileObjectCollectionView(existingView, collectionSpec, targetTypeClass, task, result);
    }

    private void compileView(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, boolean replaceIfExist) {
        if (objectListViewType == null) {
            return;
        }
        compileObjectType(existingView, objectListViewType);
        compileActions(existingView, objectListViewType);
        compileColumns(existingView, objectListViewType);
        compileDisplay(existingView, objectListViewType, replaceIfExist);
        compileDistinct(existingView, objectListViewType, replaceIfExist);
        compileSorting(existingView, objectListViewType, replaceIfExist);
        compileCounting(existingView, objectListViewType, replaceIfExist);
        compileDisplayOrder(existingView, objectListViewType, replaceIfExist);
        compileSearchBox(existingView, objectListViewType, replaceIfExist);
        compileRefreshInterval(existingView, objectListViewType, replaceIfExist);
        compilePaging(existingView, objectListViewType, replaceIfExist);
        compilePagingOptions(existingView, objectListViewType, replaceIfExist);
        compileViewIdentifier(existingView, objectListViewType, replaceIfExist);
        compileVisibility(existingView, objectListViewType);
        compileApplicableForOperation(existingView, objectListViewType);
    }

    @Nullable
    private ObjectFilter evaluateExpressionsInFilter(ObjectFilter filterRaw, OperationResult result, Task task)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        VariablesMap variables = new VariablesMap(); // do we want to put any variables here?
        return ExpressionUtil.evaluateFilterExpressions(
                filterRaw, variables, MiscSchemaUtil.getExpressionProfile(),
                expressionFactory, "collection filter", task, result);
    }

    private void compileObjectType(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        if (existingView.getContainerType() == null) {
            existingView.setContainerType(objectListViewType.getType());
        }
        if (objectListViewType.getType() != null) {
            Class<Object> existingType = prismContext.getSchemaRegistry().determineCompileTimeClass(existingView.getContainerType());
            Class<Object> newType = prismContext.getSchemaRegistry().determineCompileTimeClass(objectListViewType.getType());
            if (existingType != null && newType != null && existingType.isAssignableFrom(newType)) {
                existingView.setContainerType(objectListViewType.getType());
            }
        }
    }

    private void compileActions(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        List<GuiActionType> newActions = objectListViewType.getAction();
        for (GuiActionType newAction : newActions) {
            // TODO: check for action override
            if (!alreadyExist(existingView, newAction)) {
                existingView.getActions().add(newAction); // No need to clone, CompiledObjectCollectionView is not prism
            }
        }
    }

    private boolean alreadyExist(CompiledObjectCollectionView view, GuiActionType action) {
        return view.getActions()
                .stream()
                .anyMatch(a -> ObjectReferenceTypeUtil.referencesOidEqual(a.getTaskTemplateRef(), action.getTaskTemplateRef()));
    }

    private void compileColumns(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        List<GuiObjectColumnType> newColumns = objectListViewType.getColumn();
        if (newColumns == null || newColumns.isEmpty()) {
            return;
        }
        existingView.setIncludeDefaultColumns(objectListViewType.isIncludeDefaultColumns());
        // Not very efficient algorithm. But must do for now.
        List<GuiObjectColumnType> existingColumns = existingView.getColumns();
        MiscSchemaUtil.mergeColumns(existingColumns, objectListViewType.getColumn());
        List<GuiObjectColumnType> orderedList = MiscSchemaUtil.orderCustomColumns(existingColumns);
        existingColumns.clear();
        existingColumns.addAll(orderedList);
    }

    private void compileDisplay(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, boolean replaceIfExist) {
        DisplayType newDisplay = objectListViewType.getDisplay();
        if (newDisplay == null) {
            return;
        }
        if (existingView.getDisplay() == null) {
            existingView.setDisplay(newDisplay);
        } else if (replaceIfExist) {
            MiscSchemaUtil.mergeDisplay(existingView.getDisplay(), newDisplay);
        }
    }

    private void compileDistinct(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, boolean replaceIfExist) {
        DistinctSearchOptionType newDistinct = objectListViewType.getDistinct();
        if (newDistinct == null) {
            return;
        }
        if (existingView.getDistinct() == null || replaceIfExist) {
            existingView.setDistinct(newDistinct);
        }
    }

    private void compileSorting(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, boolean replaceIfExist) {
        Boolean newDisableSorting = objectListViewType.isDisableSorting();
        if (newDisableSorting != null && (existingView.getDisableSorting() == null || replaceIfExist)) {
            existingView.setDisableSorting(newDisableSorting);
        }
    }

    private void compileRefreshInterval(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, boolean replaceIfExist) {
        Integer refreshInterval = objectListViewType.getRefreshInterval();
        if (refreshInterval != null && (existingView.getRefreshInterval() == null || replaceIfExist)) {
            existingView.setRefreshInterval(refreshInterval);
        }
    }

    private void compileCounting(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, boolean replaceIfExist) {
        Boolean newDisableCounting = objectListViewType.isDisableCounting();
        if (newDisableCounting != null && (existingView.isDisableCounting() == null || replaceIfExist)) {
            existingView.setDisableCounting(newDisableCounting);
        }
    }

    private void compileDisplayOrder(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, boolean replaceIfExist) {
        Integer newDisplayOrder = objectListViewType.getDisplayOrder();
        if (newDisplayOrder != null && (existingView.getDisplayOrder() == null || replaceIfExist)) {
            existingView.setDisplayOrder(newDisplayOrder);
        }
    }

    private void compileSearchBox(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, boolean replaceIfExist) {
        SearchBoxConfigurationType newSearchBoxConfig = objectListViewType.getSearchBoxConfiguration();
        if (newSearchBoxConfig == null) {
            return;
        }

        SearchBoxConfigurationType oldSearchBoxConfig = existingView.getSearchBoxConfiguration();
        if (oldSearchBoxConfig == null || replaceIfExist) {
            if (oldSearchBoxConfig != null) {
                if (newSearchBoxConfig.getSearchItems() == null) {
                    newSearchBoxConfig.setSearchItems(oldSearchBoxConfig.getSearchItems());
                }
                if (CollectionUtils.isEmpty(newSearchBoxConfig.getAvailableFilter())) {
                    newSearchBoxConfig.getAvailableFilter().addAll(oldSearchBoxConfig.getAvailableFilter());
                }
                if (newSearchBoxConfig.getDefaultMode() == null) {
                    newSearchBoxConfig.setDefaultMode(oldSearchBoxConfig.getDefaultMode());
                }
                if (newSearchBoxConfig.getAllowedMode().isEmpty()) {
                    newSearchBoxConfig.getAllowedMode().addAll(oldSearchBoxConfig.getAllowedMode());
                }
                if (newSearchBoxConfig.getIndirectConfiguration() == null) {
                    newSearchBoxConfig.setIndirectConfiguration(oldSearchBoxConfig.getIndirectConfiguration());
                }
                if (newSearchBoxConfig.getObjectTypeConfiguration() == null) {
                    newSearchBoxConfig.setObjectTypeConfiguration(oldSearchBoxConfig.getObjectTypeConfiguration());
                }
                if (newSearchBoxConfig.getProjectConfiguration() == null) {
                    newSearchBoxConfig.setProjectConfiguration(oldSearchBoxConfig.getProjectConfiguration());
                }
                if (newSearchBoxConfig.getRelationConfiguration() == null) {
                    newSearchBoxConfig.setRelationConfiguration(oldSearchBoxConfig.getRelationConfiguration());
                }
                if (newSearchBoxConfig.getScopeConfiguration() == null) {
                    newSearchBoxConfig.setScopeConfiguration(oldSearchBoxConfig.getScopeConfiguration());
                }
                if (newSearchBoxConfig.getTenantConfiguration() == null) {
                    newSearchBoxConfig.setTenantConfiguration(oldSearchBoxConfig.getTenantConfiguration());
                }
            }
            existingView.setSearchBoxConfiguration(newSearchBoxConfig);
        }
    }

    public void compileDefaultObjectListConfiguration(DefaultGuiObjectListPanelConfigurationType existingConfig,
            DefaultGuiObjectListPanelConfigurationType newConfig) {
        compileDefaultObjectListConfiguration(existingConfig, newConfig, true);
    }

    public void compileDefaultObjectListConfiguration(DefaultGuiObjectListPanelConfigurationType existingConfig,
            DefaultGuiObjectListPanelConfigurationType newConfig, boolean replaceIfExist) {
        if (newConfig == null) {
            return;
        }
        compilePaging(existingConfig, newConfig, replaceIfExist);
        compilePagingOptions(existingConfig, newConfig, replaceIfExist);
    }

    private <DC extends DefaultGuiObjectListPanelConfigurationType> void compilePaging(CompiledObjectCollectionView existingView,
            DC objectListViewType, boolean replaceIfExist) {
        PagingType newPaging = objectListViewType.getPaging();
        if (newPaging == null) {
            return;
        }
        if (existingView.getPaging() == null) {
            existingView.setPaging(newPaging);
        } else if (replaceIfExist) {
            MiscSchemaUtil.replacePaging(existingView.getPaging(), newPaging);
        }
    }

    private <DC extends DefaultGuiObjectListPanelConfigurationType> void compilePaging(DC existingConfig,
            DC newConfig, boolean replaceIfExist) {
        PagingType newPaging = newConfig.getPaging();
        if (newConfig.getPaging() == null) {
            return;
        }
        if (existingConfig.getPaging() == null) {
            existingConfig.setPaging(newConfig.getPaging());
        } else if (replaceIfExist) {
            MiscSchemaUtil.replacePaging(existingConfig.getPaging(), newPaging);
        }
    }

    private <DC extends DefaultGuiObjectListPanelConfigurationType> void compilePagingOptions(
            CompiledObjectCollectionView existingView, DC newConfig, boolean replaceIfExist) {
        PagingOptionsType newPagingOptions = newConfig.getPagingOptions();
        if (newPagingOptions == null) {
            return;
        }
        if (existingView.getPagingOptions() == null) {
            existingView.setPagingOptions(newPagingOptions);
        } else if (replaceIfExist) {
            MiscSchemaUtil.mergePagingOptions(existingView.getPagingOptions(), newPagingOptions);
        }
    }

    private <DC extends DefaultGuiObjectListPanelConfigurationType> void compilePagingOptions(
            DC existingConfig, DC newConfig, boolean replaceIfExist) {
        PagingOptionsType newPagingOptions = newConfig.getPagingOptions();
        if (newPagingOptions == null) {
            return;
        }
        if (existingConfig.getPagingOptions() == null) {
            existingConfig.setPagingOptions(newPagingOptions);
        } else if (replaceIfExist) {
            MiscSchemaUtil.mergePagingOptions(existingConfig.getPagingOptions(), newPagingOptions);
        }
    }

    private void compileVisibility(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        if (objectListViewType.getVisibility() != null) {
            existingView.setVisibility(objectListViewType.getVisibility());
        }
    }

    private void compileApplicableForOperation(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        if (objectListViewType.getApplicableForOperation() != null) {
            existingView.setApplicableForOperation(objectListViewType.getApplicableForOperation());
        }
    }

    private void compileViewIdentifier(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, boolean replaceIfExist) {
        String identifier = determineViewIdentifier(objectListViewType);
        if (identifier == null) {
            return;
        }
        if (existingView.getViewIdentifier() == null || replaceIfExist) {
            existingView.setViewIdentifier(identifier);
        }
    }

    public String determineViewIdentifier(GuiObjectListViewType objectListViewType) {
        return determineViewIdentifier(objectListViewType, ""); //todo how to get default view identifier by type? or return null
    }

    private String determineViewIdentifier(GuiObjectListViewType objectListViewType, String defaultViewIdentifier) {
        String viewIdentifier = objectListViewType.getIdentifier();
        if (viewIdentifier != null) {
            return viewIdentifier;
        }
        CollectionRefSpecificationType collection = objectListViewType.getCollection();
        if (collection != null && collection.getCollectionRef() != null) {
            return collection.getCollectionRef().getOid();
        }
        return defaultViewIdentifier;
    }
}
