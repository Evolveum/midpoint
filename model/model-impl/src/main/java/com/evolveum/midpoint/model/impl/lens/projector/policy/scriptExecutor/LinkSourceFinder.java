/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LinkSourceObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import javax.xml.namespace.QName;

/**
 * Finds link sources based on a collection of selectors.
 *
 * Preliminary implementation. It is not optimizing link source search.
 * Instead of creating a sophisticated query filter based on object constraints it simply
 * finds all the sources and applies the constraints afterwards.
 * So: TODO optimize this class
 *
 * TODO think if we should use model or repository when looking for objects
 */
class LinkSourceFinder implements AutoCloseable {

    private static final Trace LOGGER = TraceManager.getTrace(LinkSourceFinder.class);

    private static final String OP_GET_SOURCES = LinkSourceFinder.class.getName() + ".getSources";

    private final PolicyRuleScriptExecutor beans;
    private final LensContext<?> context;
    private final OperationResult result;

    LinkSourceFinder(PolicyRuleScriptExecutor policyRuleScriptExecutor, LensContext<?> context,
            OperationResult parentResult) {
        this.beans = policyRuleScriptExecutor;
        this.context = context;
        this.result = parentResult.createMinorSubresult(OP_GET_SOURCES);
    }

    List<PrismObject<? extends ObjectType>> getSources(List<LinkSourceObjectSelectorType> sourceSelectors) throws SchemaException,
            ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ExpressionEvaluationException {
        try {
            List<PrismObject<? extends AssignmentHolderType>> allSources = getAllSources(context.getFocusContextRequired().getOid());
            // noinspection unchecked
            return (List) filterObjects(allSources, sourceSelectors);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        }
    }

    @NotNull
    private List<PrismObject<? extends AssignmentHolderType>> getAllSources(String focusOid) throws SchemaException {
        if (focusOid == null) {
            LOGGER.warn("No focus object OID, no assignees can be found");
            return Collections.emptyList();
        } else {
            ObjectQuery query = beans.prismContext.queryFor(AssignmentHolderType.class)
                    .item(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF).ref(focusOid)
                    .build();
            //noinspection unchecked
            return (List) beans.repositoryService.searchObjects(AssignmentHolderType.class, query, null, result);
        }
    }

    private List<PrismObject<? extends AssignmentHolderType>> filterObjects(List<PrismObject<? extends AssignmentHolderType>> objects, List<LinkSourceObjectSelectorType> selectors)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        List<PrismObject<? extends AssignmentHolderType>> all = new ArrayList<>();
        for (LinkSourceObjectSelectorType selector : selectors) {
            all.addAll(filterObjects(objects, selector));
        }
        return all;
    }

    private List<PrismObject<? extends AssignmentHolderType>> filterObjects(List<PrismObject<? extends AssignmentHolderType>> objects, LinkSourceObjectSelectorType selector)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        List<PrismObject<? extends AssignmentHolderType>> matching = new ArrayList<>();
        for (PrismObject<? extends AssignmentHolderType> object : objects) {
            if (beans.repositoryService.selectorMatches(selector, object,
                    null, LOGGER, "script object evaluation") &&
                    relationMatches(object, selector.getRelation())) {
                matching.add(object);
            }
        }
        return matching;
    }

    private boolean relationMatches(PrismObject<? extends AssignmentHolderType> linkSource, List<QName> relations) {
        return relations.isEmpty() || linkSource.asObjectable().getRoleInfluenceRef().stream()
                .anyMatch(ref -> beans.prismContext.relationMatches(relations, ref.getRelation()));
    }

    @Override
    public void close() {
        result.computeStatusIfUnknown();
    }
}
