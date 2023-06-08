/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.controller.transformer;

import java.util.IdentityHashMap;
import java.util.List;

import com.evolveum.midpoint.model.impl.controller.SchemaTransformer;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutableItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Parts of {@link SchemaTransformer} devoted to modifying prism definitions according to access allowed.
 */
@Component
public class DefinitionAccessProcessor {

    /** Using this logger for compatibility reasons. */
    private static final Trace LOGGER = TraceManager.getTrace(SchemaTransformer.class);

    public <D extends ItemDefinition<?>> void applySecurityConstraintsToItemDef(
            @NotNull D itemDefinition,
            @NotNull ObjectSecurityConstraints securityConstraints,
            @Nullable AuthorizationPhaseType phase) {
        if (phase == null) {
            applySecurityConstraintsToItemDefPhase(itemDefinition, securityConstraints, AuthorizationPhaseType.REQUEST);
            applySecurityConstraintsToItemDefPhase(itemDefinition, securityConstraints, AuthorizationPhaseType.EXECUTION);
        } else {
            applySecurityConstraintsToItemDefPhase(itemDefinition, securityConstraints, phase);
        }
    }

    private <D extends ItemDefinition<?>> void applySecurityConstraintsToItemDefPhase(
            @NotNull D itemDefinition,
            @NotNull ObjectSecurityConstraints securityConstraints,
            @NotNull AuthorizationPhaseType phase) {
        Validate.notNull(phase);
        LOGGER.trace("applySecurityConstraints(itemDefs): def={}, phase={}", itemDefinition, phase);
        applySecurityConstraintsToItemDef(
                itemDefinition, new IdentityHashMap<>(), ItemPath.EMPTY_PATH, securityConstraints,
                null, null, null, phase);
    }

    private <D extends ItemDefinition<?>> void applySecurityConstraintsToItemDef(
            @NotNull D itemDefinition,
            @NotNull IdentityHashMap<ItemDefinition<?>, Object> definitionsSeen,
            @NotNull ItemPath nameOnlyItemPath,
            @NotNull ObjectSecurityConstraints securityConstraints,
            @Nullable AuthorizationDecisionType defaultReadDecision,
            @Nullable AuthorizationDecisionType defaultAddDecision,
            @Nullable AuthorizationDecisionType defaultModifyDecision,
            @NotNull AuthorizationPhaseType phase) {

        boolean thisWasSeen = definitionsSeen.containsKey(itemDefinition);
        definitionsSeen.put(itemDefinition, null);

        AuthorizationDecisionType readDecision = securityConstraints.computeItemDecision(
                nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, defaultReadDecision, phase);
        AuthorizationDecisionType addDecision = securityConstraints.computeItemDecision(
                nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_ADD, defaultAddDecision, phase);
        AuthorizationDecisionType modifyDecision = securityConstraints.computeItemDecision(
                nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_MODIFY, defaultModifyDecision, phase);

        boolean anySubElementRead = false;
        boolean anySubElementAdd = false;
        boolean anySubElementModify = false;
        if (itemDefinition instanceof PrismContainerDefinition<?>) {
            if (thisWasSeen) {
                LOGGER.trace("applySecurityConstraintsToItemDef: {}: skipping (already seen)", nameOnlyItemPath);
            } else if (itemDefinition.isElaborate()) {
                LOGGER.trace("applySecurityConstraintsToItemDef: {}: skipping (elaborate)", nameOnlyItemPath);
            } else {
                PrismContainerDefinition<?> containerDefinition = (PrismContainerDefinition<?>) itemDefinition;
                List<? extends ItemDefinition<?>> subDefinitions = containerDefinition.getDefinitions();
                for (ItemDefinition<?> subDef : subDefinitions) {
                    ItemPath subPath = ItemPath.create(nameOnlyItemPath, subDef.getItemName());
                    if (!subDef.getItemName().equals(ShadowType.F_ATTRIBUTES)) { // Shadow attributes have special handling
                        applySecurityConstraintsToItemDef(
                                subDef, definitionsSeen, subPath, securityConstraints,
                                readDecision, addDecision, modifyDecision, phase);
                    }
                    if (subDef.canRead()) {
                        anySubElementRead = true;
                    }
                    if (subDef.canAdd()) {
                        anySubElementAdd = true;
                    }
                    if (subDef.canModify()) {
                        anySubElementModify = true;
                    }
                }
            }
        }

        LOGGER.trace("applySecurityConstraintsToItemDef: {}: decisions R={}, A={}, M={}; sub-elements R={}, A={}, M={}",
                nameOnlyItemPath, readDecision, addDecision, modifyDecision, anySubElementRead, anySubElementAdd, anySubElementModify);

        if (readDecision != AuthorizationDecisionType.ALLOW) {
            mutable(itemDefinition).setCanRead(false);
        }
        if (addDecision != AuthorizationDecisionType.ALLOW) {
            mutable(itemDefinition).setCanAdd(false);
        }
        if (modifyDecision != AuthorizationDecisionType.ALLOW) {
            mutable(itemDefinition).setCanModify(false);
        }

        if (anySubElementRead) {
            mutable(itemDefinition).setCanRead(true);
        }
        if (anySubElementAdd) {
            mutable(itemDefinition).setCanAdd(true);
        }
        if (anySubElementModify) {
            mutable(itemDefinition).setCanModify(true);
        }
    }

    private MutableItemDefinition<?> mutable(ItemDefinition<?> itemDef) {
        return itemDef.toMutable();
    }
}
