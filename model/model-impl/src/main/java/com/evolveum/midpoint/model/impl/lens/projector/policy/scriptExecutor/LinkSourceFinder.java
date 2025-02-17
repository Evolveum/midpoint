/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

import static java.util.Collections.emptyList;

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

    List<PrismObject<? extends ObjectType>> getSourcesAsObjects() throws SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        try {
            return searchForSources(getSourcesAsQuery());
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        }
    }

    List<PrismReferenceValue> getSourcesAsReferences() throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        try {
            return searchForSourceReferences(getSourcesAsQuery());
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        }
    }

    @NotNull
    CompleteQuery<?> getSourcesAsQuery() throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        ObjectQuery query = createQuery();
        Class<? extends AssignmentHolderType> objectType = getSourceType();
        CompleteQuery<? extends AssignmentHolderType> completeQuery = new CompleteQuery<>(objectType, query, null);

        LOGGER.trace("Sources as query:\n{}", completeQuery.debugDumpLazily());
        return completeQuery;
    }

    @NotNull
    private List<LinkedObjectSelectorType> collectSourceSelectors() throws SchemaException, ConfigurationException {
        ScriptExecutionObjectType objectSpec = actx.action.getObject();
        assert objectSpec != null;
        List<LinkedObjectSelectorType> selectors = new ArrayList<>();
        selectors.addAll(resolveLinkTypes(objectSpec.getLinkSource()));
        selectors.addAll(resolveNamedLinkSources(objectSpec.getNamedLinkSource()));
        return selectors;
    }

    private List<LinkedObjectSelectorType> resolveNamedLinkSources(List<String> linkTypeNames)
            throws SchemaException, ConfigurationException {
        List<LinkedObjectSelectorType> resolvedSelectors = new ArrayList<>();
        for (String linkTypeName : linkTypeNames) {
            resolvedSelectors.add(resolveSourceSelector(linkTypeName));
        }
        return resolvedSelectors;
    }

    private List<LinkedObjectSelectorType> resolveLinkTypes(List<LinkSourceObjectSelectorType> selectors)
            throws SchemaException, ConfigurationException {
        List<LinkedObjectSelectorType> resolvedSelectors = new ArrayList<>();
        for (LinkSourceObjectSelectorType selector : selectors) {
            String linkType = selector.getLinkType();
            if (linkType != null) {
                resolvedSelectors.add(
                        mergeSelectors(
                                selector,
                                resolveSourceSelector(linkType)));
            } else {
                resolvedSelectors.add(selector);
            }
        }
        return resolvedSelectors;
    }

    private LinkSourceObjectSelectorType mergeSelectors(LinkSourceObjectSelectorType base, LinkedObjectSelectorType additional)
            throws SchemaException {
        LinkSourceObjectSelectorType merged = base.clone();
        merged.setLinkType(null);
        ((PrismContainerValue<?>) merged.asPrismContainerValue()).mergeContent(additional.asPrismContainerValue(), emptyList());
        return merged;
    }

    private LinkedObjectSelectorType resolveSourceSelector(String linkTypeName)
            throws SchemaException, ConfigurationException {
        LensFocusContext<?> fc = actx.focusContext;
        LinkTypeDefinitionType definition =
                actx.beans.linkManager.getSourceLinkTypeDefinitionRequired(
                        linkTypeName,
                        Arrays.asList(fc.getObjectNew(), fc.getObjectCurrent(), fc.getObjectOld()),
                        result);
        return definition.getSelector() != null ?
                definition.getSelector() : new LinkSourceObjectSelectorType();
    }

    private ObjectQuery createQuery() throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        List<LinkedObjectSelectorType> sourceSelectors = collectSourceSelectors();
        List<ObjectFilter> convertedSelectors = new ArrayList<>(sourceSelectors.size());
        for (LinkedObjectSelectorType sourceSelector : sourceSelectors) {
            convertedSelectors.add(createFilter(sourceSelector));
        }
        return queryFactory.createQuery(
                queryFactory.createOrOptimized(convertedSelectors));
    }

    private ObjectFilter createFilter(@NotNull LinkedObjectSelectorType sourceSelector) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        SelectorToFilterTranslator translator =
                new SelectorToFilterTranslator(
                        sourceSelector, AssignmentHolderType.class,
                        "link source selector in rule script executor", LOGGER, actx.task);
        Class<? extends ObjectType> narrowedSourceType = translator.getNarrowedTargetType();
        narrowedSourceTypes.add(narrowedSourceType);
        return ObjectQueryUtil.simplify(
                queryFactory.createAndOptimized(
                        translator.createFilter(result),
                        beans.prismContext.queryFor(narrowedSourceType)
                                .item(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                                .ref(createExpectedReferenceValues(sourceSelector))
                                .buildFilter()));
    }

    @NotNull
    private List<PrismReferenceValue> createExpectedReferenceValues(LinkedObjectSelectorType sourceSelector) {
        List<QName> relations = sourceSelector.getRelation();
        List<PrismReferenceValue> values = new ArrayList<>(relations.size());
        if (relations.isEmpty()) {
            values.add(new ObjectReferenceType().oid(focusOid).asReferenceValue());
        } else {
            for (QName relation : relations) {
                values.add(new ObjectReferenceType().oid(focusOid).relation(relation).asReferenceValue());
            }
        }
        return values;
    }

    @NotNull
    private List<PrismObject<? extends ObjectType>> searchForSources(CompleteQuery<?> completeQuery) throws SchemaException {
        //noinspection unchecked,rawtypes
        return (List) beans.repositoryService.searchObjects(completeQuery.getType(),
                completeQuery.getQuery(), completeQuery.getOptions(), result);
    }

    @NotNull
    private List<PrismReferenceValue> searchForSourceReferences(CompleteQuery<?> completeQuery) throws SchemaException {
        List<PrismReferenceValue> references = new ArrayList<>();
        // not providing own operation result, as the processing is minimal here
        beans.repositoryService.searchObjectsIterative(
                completeQuery.getType(), completeQuery.getQuery(),
                (object, parentResult) ->
                        references.add(ObjectTypeUtil.createObjectRef(object).asReferenceValue()),
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
