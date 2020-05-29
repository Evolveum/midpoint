/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.repo.api.query.CompleteQuery;
import com.evolveum.midpoint.repo.common.query.SelectorToFilterTranslator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LinkSourceObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Finds link sources based on a collection of selectors.
 *
 * TODO Think if we should use model or repository when looking for objects. Currently we use the repo.
 */
class LinkSourceFinder implements AutoCloseable {

    private static final Trace LOGGER = TraceManager.getTrace(LinkSourceFinder.class);

    private static final String OP_GET_SOURCES = LinkSourceFinder.class.getName() + ".getSources";

    @NotNull private final ActionContext actx;
    @NotNull private final PolicyRuleScriptExecutor beans;
    private final QueryFactory queryFactory;
    private final String focusOid;
    private final OperationResult result;
    private final Set<Class<?>> narrowedSourceTypes = new HashSet<>();

    LinkSourceFinder(ActionContext actx, OperationResult parentResult) {
        this.actx = actx;
        this.beans = actx.beans;
        this.queryFactory = beans.prismContext.queryFactory();
        this.focusOid = actx.focusContext.getOid();
        this.result = parentResult.createMinorSubresult(OP_GET_SOURCES);
    }

    List<PrismObject<? extends ObjectType>> getSourcesAsObjects(List<LinkSourceObjectSelectorType> sourceSelectors) throws SchemaException,
            ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ExpressionEvaluationException {
        try {
            return searchForSources(getSourcesAsQuery(sourceSelectors));
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        }
    }

    List<PrismReferenceValue> getSourcesAsReferences(List<LinkSourceObjectSelectorType> sourceSelectors) throws SchemaException,
            ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ExpressionEvaluationException {
        try {
            return searchForSourceReferences(getSourcesAsQuery(sourceSelectors));
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        }
    }

    @NotNull
    CompleteQuery<?> getSourcesAsQuery(List<LinkSourceObjectSelectorType> sourceSelectors) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        ObjectQuery query = createQuery(sourceSelectors);
        Class<? extends AssignmentHolderType> objectType = getSourceType();
        CompleteQuery<? extends AssignmentHolderType> completeQuery = new CompleteQuery<>(objectType, query, null);

        LOGGER.trace("Sources as query:\n{}", completeQuery.debugDumpLazily());
        return completeQuery;
    }

    private ObjectQuery createQuery(List<LinkSourceObjectSelectorType> sourceSelectors) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        List<ObjectFilter> convertedSelectors = new ArrayList<>(sourceSelectors.size());
        for (LinkSourceObjectSelectorType sourceSelector : sourceSelectors) {
            convertedSelectors.add(createFilter(sourceSelector));
        }
        return queryFactory.createQuery(
                queryFactory.createOrOptimized(convertedSelectors));
    }

    private ObjectFilter createFilter(LinkSourceObjectSelectorType sourceSelector) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        SelectorToFilterTranslator translator = new SelectorToFilterTranslator(sourceSelector, AssignmentHolderType.class,
                "link source selector in rule script executor", beans.prismContext, beans.expressionFactory,
                actx.task, result);
        ObjectFilter selectorFilter = translator.createFilter();
        Class<? extends ObjectType> narrowedSourceType = translator.getNarrowedTargetType();
        ObjectFilter allSourcesFilter = beans.prismContext.queryFor(narrowedSourceType)
                .item(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF).ref(createExpectedReferenceValues(sourceSelector))
                .buildFilter();

        narrowedSourceTypes.add(narrowedSourceType);
        return ObjectQueryUtil.simplify(queryFactory.createAnd(allSourcesFilter, selectorFilter), beans.prismContext);
    }

    @NotNull
    private List<PrismReferenceValue> createExpectedReferenceValues(LinkSourceObjectSelectorType sourceSelector) {
        List<PrismReferenceValue> values = new ArrayList<>(sourceSelector.getRelation().size());
        if (sourceSelector.getRelation().isEmpty()) {
            values.add(new ObjectReferenceType().oid(focusOid).asReferenceValue());
        } else {
            for (QName relation : sourceSelector.getRelation()) {
                values.add(new ObjectReferenceType().oid(focusOid).relation(relation).asReferenceValue());
            }
        }
        return values;
    }

    @NotNull
    private List<PrismObject<? extends ObjectType>> searchForSources(CompleteQuery<?> completeQuery) throws SchemaException {
        //noinspection unchecked
        return (List) beans.repositoryService.searchObjects(completeQuery.getType(),
                completeQuery.getQuery(), completeQuery.getOptions(), result);
    }

    @NotNull
    private List<PrismReferenceValue> searchForSourceReferences(CompleteQuery<?> completeQuery) throws SchemaException {
        List<PrismReferenceValue> references = new ArrayList<>();
        beans.repositoryService.searchObjectsIterative(completeQuery.getType(), completeQuery.getQuery(),
                (object, parentResult) ->
                        references.add(ObjectTypeUtil.createObjectRef(object, beans.prismContext).asReferenceValue()),
                completeQuery.getOptions(), false, result);
        return references;
    }

    private Class<? extends AssignmentHolderType> getSourceType() {
        Class<?> ancestor = MiscUtil.determineCommonAncestor(narrowedSourceTypes);
        if (ancestor == null || !AssignmentHolderType.class.isAssignableFrom(ancestor)) {
            return AssignmentHolderType.class;
        } else {
            //noinspection unchecked
            return (Class<? extends AssignmentHolderType>) ancestor;
        }
    }

    @Override
    public void close() {
        result.computeStatusIfUnknown();
    }
}
