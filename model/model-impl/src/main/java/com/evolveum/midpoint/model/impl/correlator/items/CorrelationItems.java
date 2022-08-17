/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import java.util.*;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Collection of correlation items (for given correlation or correlation-like operation.)
 */
class CorrelationItems implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationItems.class);

    @NotNull private final List<CorrelationItem> items;

    private CorrelationItems(@NotNull List<CorrelationItem> items) {
        this.items = items;
        LOGGER.trace("CorrelationItems created:\n{}", DebugUtil.debugDumpLazily(items, 1));
    }

    public static @NotNull CorrelationItems create(
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext,
            @NotNull CorrelationContext correlationContext,
            @NotNull ModelBeans beans) throws ConfigurationException {

        List<CorrelationItem> items = new ArrayList<>();
        for (ItemCorrelationType itemBean : correlatorContext.getConfigurationBean().getItem()) {
            items.add(
                    CorrelationItem.create(itemBean, correlatorContext, correlationContext.getPreFocus(), beans));
        }
        stateCheck(!items.isEmpty(), "No correlation items in %s", correlatorContext);
        return new CorrelationItems(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    public Collection<CorrelationItem> getItems() {
        return items;
    }

    ObjectQuery createIdentityQuery(
            @NotNull Class<? extends ObjectType> focusType,
            @Nullable String archetypeOid,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException {

        assert !items.isEmpty();

        S_FilterEntry nextStart = PrismContext.get().queryFor(focusType);
        S_FilterExit currentEnd = null;
        for (int i = 0; i < items.size(); i++) {
            CorrelationItem correlationItem = items.get(i);
            currentEnd = correlationItem.addClauseToQueryBuilder(nextStart, task, result);
            if (i < items.size() - 1) {
                nextStart = currentEnd.and();
            } else {
                // We shouldn't modify the builder if we are at the end.
                // (The builder API does not mention it, but the state of the objects are modified on each operation.)
            }
        }

        assert currentEnd != null;

        // Finally, we add a condition for archetype (if needed)
        S_FilterExit end =
                archetypeOid != null ?
                        currentEnd.and().item(FocusType.F_ARCHETYPE_REF).ref(archetypeOid) :
                        currentEnd;

        return end.build();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "items", items, indent + 1);
        return sb.toString();
    }
}
