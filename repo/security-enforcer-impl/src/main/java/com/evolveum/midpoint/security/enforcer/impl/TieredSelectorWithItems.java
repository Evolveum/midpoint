/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.schema.selector.spec.ParentClause;
import com.evolveum.midpoint.security.enforcer.impl.prism.PrismEntityCoverageInformation;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.enforcer.api.PrismEntityOpConstraints;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a {@link SelectorWithItems} converted into a form suitable for top-down application to a {@link PrismObjectValue}.
 * It is decomposed to a chain of {@link TieredSelectorWithItems} objects with the following properties:
 *
 * . The {@link #selectorWithItems} is always parent-less, i.e. it has no {@link ParentClause}.
 * . An optional child selector (that can be only one at this moment) is linked to by {@link #linkToChild}.
 *
 * The primary use of this class is to facilitate creating of "decision trees" for items/values visibility,
 * i.e. {@link PrismEntityCoverageInformation} objects that support {@link PrismEntityOpConstraints}.
 *
 * Its existence as a separate Java class is to allow easy transformation: for example, if we have an {@link Authorization}
 * destined to e.g. {@link AccessCertificationCaseType}, we need to transform the selector(s) to ones to be easily applicable
 * to e.g. {@link AccessCertificationCampaignType} objects that contain these cases in `case` container.
 */
public class TieredSelectorWithItems {

    /** Always parent-less. */
    @NotNull private final SelectorWithItems selectorWithItems;

    @Nullable private final Link linkToChild;

    private TieredSelectorWithItems(@NotNull SelectorWithItems selectorWithItems, @Nullable Link linkToChild) {
        Preconditions.checkArgument(selectorWithItems.isParentLess());
        this.selectorWithItems = selectorWithItems;
        this.linkToChild = linkToChild;
    }

    static TieredSelectorWithItems withNoChild(@NotNull SelectorWithItems enhancedSelector) {
        assert enhancedSelector.isParentLess();
        return new TieredSelectorWithItems(enhancedSelector, null);
    }

    static TieredSelectorWithItems withChild(
            @NotNull ValueSelector selector,
            @NotNull ItemPath pathToChild,
            @NotNull TieredSelectorWithItems child) {
        assert selector.isParentLess();
        return new TieredSelectorWithItems(
                SelectorWithItems.of(selector),
                new Link(pathToChild, child));
    }

    /**
     * Creates "tiered selectors" derived from the currently-processed authorization that are relevant to given `value`.
     */
    public static Collection<TieredSelectorWithItems> forAutzAndValue(
            @NotNull PrismObjectValue<?> value, @NotNull AuthorizationEvaluation evaluation)
            throws ConfigurationException {

        Authorization autz = evaluation.getAuthorization();

        var autzObjectSelectors = autz.getParsedObjectSelectors();
        // Autz selectors can be empty e.g. for #all autz or for weird ones like role-prop-read-some-modify-some.xml.
        List<ValueSelector> objectSelectors =
                !autzObjectSelectors.isEmpty() ? autzObjectSelectors : List.of(ValueSelector.empty());

        List<TieredSelectorWithItems> selectorsWithItems = new ArrayList<>();
        for (ValueSelector objectSelector : objectSelectors) {
            if (objectSelector.isSubObject() && evaluation.shouldSkipSubObjectSelectors()) {
                continue;
            }
            SelectorWithItems base =
                    SelectorWithItems.of(objectSelector, autz.getItems(), autz.getExceptItems(), evaluation.getDesc());
            TieredSelectorWithItems tiered = base.asTieredSelectors(value.asObjectable().getClass());
            if (tiered != null) {
                selectorsWithItems.add(tiered);
            }
        }
        return selectorsWithItems;
    }

    public @Nullable Link getLinkToChild() {
        return linkToChild;
    }

    public @NotNull ValueSelector getSelector() {
        return selectorWithItems.getSelector();
    }

    public @NotNull PathSet getPositives() {
        return selectorWithItems.getPositives();
    }

    public @NotNull PathSet getNegatives() {
        return selectorWithItems.getNegatives();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "selector=" + selectorWithItems +
                ", link=" + linkToChild +
                '}';
    }

    boolean hasOverlapWith(@NotNull Class<? extends ObjectType> requiredType) {
        return selectorWithItems.hasOverlapWith(requiredType);
    }

    /**
     * Links two {@link TieredSelectorWithItems} objects via a parent-child link.
     */
    public static class Link {

        /** Name of the item that presents the link. It is an item in the parent. */
        @NotNull private final ItemPath itemPath;

        @NotNull private final TieredSelectorWithItems child;

        Link(@NotNull ItemPath itemPath, @NotNull TieredSelectorWithItems child) {
            this.itemPath = itemPath;
            this.child = child;
        }

        public @NotNull ItemPath getItemPath() {
            return itemPath;
        }

        public @NotNull TieredSelectorWithItems getChild() {
            return child;
        }

        @Override
        public String toString() {
            return "Link{" +
                    "itemPath=" + itemPath +
                    ", child=" + child +
                    '}';
        }
    }
}
