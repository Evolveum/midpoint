/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.aspect;

import static com.evolveum.midpoint.prism.PrismObject.asObjectable;
import static com.evolveum.midpoint.prism.PrismObject.asObjectableList;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.ObjectSet;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalSchemaHelper;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ReferenceResolver;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.RelationResolver;
import com.evolveum.midpoint.wf.impl.processors.ConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.ModelHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public abstract class BasePrimaryChangeAspect implements PrimaryChangeAspect, BeanNameAware {

    private static final Trace LOGGER = TraceManager.getTrace(BasePrimaryChangeAspect.class);

    private String beanName;

    @Autowired
    @Qualifier("cacheRepositoryService")
    protected RepositoryService repositoryService;

    @Autowired protected PrimaryChangeProcessor changeProcessor;
    @Autowired protected PrimaryChangeAspectHelper primaryChangeAspectHelper;
    @Autowired protected ConfigurationHelper configurationHelper;
    @Autowired protected PrismContext prismContext;
    @Autowired protected RelationRegistry relationRegistry;
    @Autowired protected ModelHelper modelHelper;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private MappingFactory mappingFactory;
    @Autowired protected ApprovalSchemaHelper approvalSchemaHelper;

    @PostConstruct
    public void init() {
        changeProcessor.registerChangeAspect(this, isFirst());
    }

    protected boolean isFirst() {
        return false;
    }

    @Override
    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(@NotNull String name) {
        this.beanName = name;
    }

    public PrimaryChangeProcessor getChangeProcessor() {
        return changeProcessor;
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;       // overridden in selected aspects
    }

    @Override
    public boolean isEnabled(PrimaryChangeProcessorConfigurationType processorConfiguration) {
        return primaryChangeAspectHelper.isEnabled(processorConfiguration, this);
    }

    private <O extends ObjectType, F extends ObjectType> List<ObjectReferenceType> resolveReferenceFromFilter(
            Class<O> clazz,
            SearchFilterType filter,
            String sourceDescription,
            LensContext<F> lensContext,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                new ModelExpressionEnvironment.ExpressionEnvironmentBuilder<F, PrismValue, ItemDefinition<?>>()
                        .lensContext(lensContext)
                        .currentResult(result)
                        .currentTask(task)
                        .build());
        try {

            PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
            VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                    getFocusObjectable(lensContext),
                    null,
                    null,
                    asObjectable(systemConfiguration));

            ObjectFilter origFilter = prismContext.getQueryConverter().parseFilter(filter, clazz);
            ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(
                    origFilter, variables, MiscSchemaUtil.getExpressionProfile(), mappingFactory.getExpressionFactory(),
                     " evaluating approverRef filter expression ", task, result);

            if (evaluatedFilter == null) {
                throw new SchemaException("Filter could not be evaluated in approverRef in "+sourceDescription+"; original filter = "+origFilter);
            }

            SearchResultList<PrismObject<O>> targets = repositoryService.searchObjects(clazz, prismContext.queryFactory().createQuery(evaluatedFilter), null, result);

            return targets.stream()
                    .map(object -> ObjectTypeUtil.createObjectRef(object))
                    .collect(Collectors.toList());

        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    private FocusType getFocusObjectable(LensContext<?> lensContext) {
        if (lensContext.getFocusContext() == null) {
            return null;        // shouldn't occur, probably
        }
        PrismObject<?> focus = lensContext.getFocusContext().getObjectAny();
        return focus != null ? (FocusType) focus.asObjectable() : null;
    }

    public ReferenceResolver createReferenceResolver(LensContext<?> modelContext, Task taskFromModel, OperationResult result) {
        return (ref, sourceDescription) -> {
            if (ref == null) {
                return Collections.emptyList();
            } else if (ref.getOid() != null) {
                return Collections.singletonList(ref.clone());
            } else {
                Class<? extends ObjectType> clazz;
                if (ref.getType() != null) {
                    clazz = prismContext.getSchemaRegistry().determineCompileTimeClass(ref.getType());
                    if (clazz == null) {
                        throw new SchemaException(
                                "Cannot determine type from " + ref.getType() + " in approver reference in " + sourceDescription);
                    }
                } else {
                    throw new SchemaException("Missing type in target reference in " + sourceDescription);
                }
                return resolveReferenceFromFilter(clazz, ref.getFilter(), sourceDescription, modelContext, taskFromModel, result);
            }
        };
    }

    public RelationResolver createRelationResolver(PrismObject<?> object, OperationResult result) {
        return relations -> {
            if (object == null
                    || object.getOid() == null
                    || relations.isEmpty()) {
                return List.of();
            }
            ObjectSet<ObjectType> approvers = new ObjectSet<>();
            for (QName relation : relations) {
                approvers.addAll(
                        findApproversByRelation(object, relation, result));
            }
            LOGGER.trace("Query evaluation resulted in {} approver(s): {}", approvers.size(), DebugUtil.toStringLazily(approvers));
            return approvers.stream()
                    .map(o -> ObjectTypeUtil.createObjectRef(o))
                    .collect(Collectors.toList());
        };
    }

    private @NotNull List<FocusType> findApproversByRelation(
            PrismObject<?> object, QName relation, OperationResult result) {

        PrismReferenceValue approverReference = prismContext.itemFactory().createReferenceValue(object.getOid());
        approverReference.setRelation(relationRegistry.normalizeRelation(relation));
        var query = prismContext.queryFor(FocusType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(approverReference)
                .build();
        LOGGER.trace("Looking for approvers for {} using query:\n{}", object, DebugUtil.debugDumpLazily(query));
        try {
            return asObjectableList(
                    repositoryService.searchObjects(FocusType.class, query, null, result));
        } catch (SchemaException e) {
            throw new SystemException("Couldn't retrieve approvers for " + object + ": " + e.getMessage(), e);
        }
    }
}
