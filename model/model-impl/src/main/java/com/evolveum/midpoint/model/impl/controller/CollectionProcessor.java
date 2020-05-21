/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
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
import com.evolveum.midpoint.model.common.ArchetypeManager;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathImpl;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathSegmentImpl;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluationOrderImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypePolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CollectionRefSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CollectionStatsPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyThresholdType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WaterMarkType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author semancik
 *
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

    public Collection<EvaluatedPolicyRule> evaluateCollectionPolicyRules(PrismObject<ObjectCollectionType> collection, CompiledObjectCollectionView collectionView, Class<? extends ObjectType> targetTypeClass, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (collectionView == null) {
            collectionView = compileObjectCollectionView(collection, targetTypeClass, task, result);
        }
        Collection<EvaluatedPolicyRule> evaluatedPolicyRules = new ArrayList<>();
        for (AssignmentType assignmentType : collection.asObjectable().getAssignment()) {

            PolicyRuleType policyRuleType = assignmentType.getPolicyRule();

            if (policyRuleType == null) {
                continue;
            }

            evaluatedPolicyRules.add(evaluatePolicyRule(collection, collectionView, assignmentType, policyRuleType, targetTypeClass, task, result));
        }
        return evaluatedPolicyRules;
    }

    /**
     * Very simple implementation, needs to be extended later.
     */
    @NotNull
    private EvaluatedPolicyRule evaluatePolicyRule(PrismObject<ObjectCollectionType> collection, CompiledObjectCollectionView collectionView, @NotNull AssignmentType assignmentType, @NotNull PolicyRuleType policyRuleType, Class<? extends ObjectType> targetTypeClass, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
        AssignmentPathImpl assignmentPath = new AssignmentPathImpl(prismContext);
        AssignmentPathSegmentImpl assignmentPathSegment = new AssignmentPathSegmentImpl.Builder()
                .source(collection.asObjectable())
                .sourceDescription("object collection "+collection)
                .assignment(assignmentType)
                .isAssignment(true)
                .relationRegistry(relationRegistry)
                .prismContext(prismContext)
                .evaluationOrder(EvaluationOrderImpl.zero(relationRegistry))
                .evaluationOrderForTarget(EvaluationOrderImpl.zero(relationRegistry))
                .direct(true) // to be reconsidered - but assignment path is empty, so we consider this to be directly assigned
                .pathToSourceValid(true)
                .sourceRelativityMode(PlusMinusZero.ZERO)
                .build();
        assignmentPath.add(assignmentPathSegment);

        EvaluatedPolicyRuleImpl evaluatedPolicyRule = new EvaluatedPolicyRuleImpl(policyRuleType.clone(), assignmentPath, prismContext);

        PolicyConstraintsType policyConstraints = policyRuleType.getPolicyConstraints();
        if (policyConstraints == null) {
            return evaluatedPolicyRule;
        }

        PolicyThresholdType policyThreshold = policyRuleType.getPolicyThreshold();

        for (CollectionStatsPolicyConstraintType collectionStatsPolicy : policyConstraints.getCollectionStats()) {
            CollectionStats stats = determineCollectionStats(collectionView, task, result);
            if (isThresholdTriggered(stats, collection, policyThreshold)) {
                EvaluatedPolicyRuleTrigger<?> trigger = new EvaluatedCollectionStatsTrigger(PolicyConstraintKindType.COLLECTION_STATS,  collectionStatsPolicy,
                        new LocalizableMessageBuilder()
                            .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY)
                            .arg(ObjectTypeUtil.createDisplayInformation(collection, false))
                            .args(/* TODO */)
                            .build(),
                        new LocalizableMessageBuilder()
                            .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY)
                            .arg(ObjectTypeUtil.createDisplayInformation(collection, false))
                            .args(/* TODO */)
                            .build()
                    );
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

    public <O extends ObjectType> CollectionStats determineCollectionStats(CompiledObjectCollectionView collectionView, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
        CollectionStats stats = new CollectionStats();
        Class<O> targetClass = collectionView.getTargetClass();
        stats.setObjectCount(countObjects(targetClass, collectionView.getFilter(), task, result));
        stats.setDomainCount(countObjects(targetClass, collectionView.getDomainFilter(), task, result));
        return stats;
    }


    private <O extends ObjectType> Integer countObjects(Class<O> targetTypeClass, ObjectFilter filter, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
        if (filter == null) {
            return null;
        }
        return modelService.countObjects(targetTypeClass, prismContext.queryFactory().createQuery(filter), null, task, result);
    }

    public CompiledObjectCollectionView compileObjectCollectionView(PrismObject<ObjectCollectionType> collection,
            Class<? extends ObjectType> targetTypeClass, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        CompiledObjectCollectionView view = new CompiledObjectCollectionView();
        compileObjectCollectionView(view, collection.asObjectable(), targetTypeClass, task, result);
        return view;
    }

    public void compileObjectCollectionView(CompiledObjectCollectionView existingView, CollectionRefSpecificationType collectionSpec,
            Class<? extends ObjectType> targetTypeClass, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {

        ObjectReferenceType collectionRef = collectionSpec.getCollectionRef();
        if (collectionRef == null) {
            // E.g. the case of empty domain specification. Nothing to do. Just return what we have.
            return;
        }
        QName collectionRefType = collectionRef.getType();

        // TODO: support more cases
        if (QNameUtil.match(ArchetypeType.COMPLEX_TYPE, collectionRefType)) {
            RefFilter filter = (RefFilter) prismContext.queryFor(AssignmentHolderType.class)
                    .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(collectionRef.getOid())
                    .buildFilter();
            filter.setTargetTypeNullAsAny(true);
            filter.setRelationNullAsAny(true);
            existingView.setFilter(filter);

            try {
                PrismObject<ArchetypeType> archetype = archetypeManager.getArchetype(collectionRef.getOid(), result);
                ArchetypePolicyType archetypePolicy = archetype.asObjectable().getArchetypePolicy();
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
            } catch (ObjectNotFoundException e) {
                // We do not want to throw exception here. This code takes place at login time.
                // We do not want to stop all logins because of missing archetype.
                LOGGER.warn("Archetype {} referenced from view {} was not found", collectionRef.getOid(), existingView.getViewIdentifier());
            }

            return;
        }

        if (QNameUtil.match(ObjectCollectionType.COMPLEX_TYPE, collectionRefType)) {
            ObjectCollectionType objectCollectionType;
            try {
                // TODO: caching?
                objectCollectionType = objectResolver.resolve(collectionRef, ObjectCollectionType.class, null, "view "+existingView.getViewIdentifier(), task, result);
            } catch (ObjectNotFoundException e) {
                throw new ConfigurationException(e.getMessage(), e);
            }
            compileObjectCollectionView(existingView, objectCollectionType, targetTypeClass, task, result);
            return;
        }

        // TODO
        throw new UnsupportedOperationException("Unsupported collection type: " + collectionRefType);
    }

    private void compileObjectCollectionView(CompiledObjectCollectionView existingView, ObjectCollectionType objectCollectionType,
            Class<? extends ObjectType> targetTypeClass, Task task, OperationResult result) throws SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException,
            ObjectNotFoundException {

        if (targetTypeClass == null) {
            if (existingView.getObjectType() == null) {
                QName targetTypeQName = objectCollectionType.getType();
                if (targetTypeQName == null) {
                    throw new SchemaException("Target object type not specified in "+objectCollectionType);
                }
                targetTypeClass = ObjectTypes.getObjectTypeClass(targetTypeQName);
                existingView.setObjectType(targetTypeQName);
            } else {
                QName targetTypeQName = existingView.getObjectType();
                targetTypeClass = ObjectTypes.getObjectTypeClass(targetTypeQName);
            }
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
            }
        }

        SearchFilterType collectionFilterType = objectCollectionType.getFilter();
        ObjectFilter collectionFilter;
        if (collectionFilterType != null) {
            ObjectFilter filterRaw = prismContext.getQueryConverter().parseFilter(collectionFilterType, targetTypeClass);
            collectionFilter = evaluateExpressionsInFilter(filterRaw, result, task);
        } else {
            collectionFilter = null;
        }
        CollectionRefSpecificationType baseCollectionSpec = objectCollectionType.getBaseCollection();
        if (baseCollectionSpec == null) {
            existingView.setFilter(collectionFilter);
        } else {
            compileObjectCollectionView(existingView, baseCollectionSpec, targetTypeClass, task, result);
            ObjectFilter baseFilter = existingView.getFilter();
            ObjectFilter combinedFilter = ObjectQueryUtil.filterAnd(baseFilter, collectionFilter, prismContext);
            existingView.setFilter(combinedFilter);
        }

    }

    @Nullable
    private ObjectFilter evaluateExpressionsInFilter(ObjectFilter filterRaw, OperationResult result, Task task)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        ExpressionVariables variables = new ExpressionVariables();      // do we want to put any variables here?
        return ExpressionUtil.evaluateFilterExpressions(filterRaw, variables, MiscSchemaUtil.getExpressionProfile(),
                expressionFactory, prismContext, "collection filter", task, result);
    }

}
