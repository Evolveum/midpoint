/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

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

/**
 * Represents a {@link Specification} converted into a form suitable for top-down application to a {@link PrismObjectValue}.
 *
 * Used to create "decision trees" for items visibility, i.e. `PrismEntityCoverageInformation` objects
 * that support {@link PrismEntityOpConstraints}.
 *
 * The ultimate reason for its existence is to allow an {@link Authorization} destined to e.g.
 * {@link AccessCertificationCaseType} provide relevant information when the client is trying to
 * process e.g. {@link AccessCertificationCampaignType} objects that contain these cases in `case` container.
 *
 * TODO better name
 */
public class TopDownSpecification {

    @NotNull private final Specification specification;

    @Nullable private final Link linkToChild;

    private TopDownSpecification(@NotNull Specification specification, @Nullable Link linkToChild) {
        this.specification = specification;
        this.linkToChild = linkToChild;
    }

    TopDownSpecification(@NotNull Specification specification) {
        this(specification, null);
    }

    static TopDownSpecification withChild(
            @NotNull ValueSelector selector,
            @NotNull ItemPath pathToChild,
            @NotNull TopDownSpecification child) {
        return new TopDownSpecification(
                Specification.of(selector),
                new Link(pathToChild, child));
    }

    public @Nullable Link getLinkToChild() {
        return linkToChild;
    }

    public @NotNull ValueSelector getSelector() {
        return specification.getSelector();
    }

    public @NotNull PathSet getPositives() {
        return specification.getPositives();
    }

    public @NotNull PathSet getNegatives() {
        return specification.getNegatives();
    }

    @Override
    public String toString() {
        return "TopDownSpecification{" +
                "spec=" + specification +
                ", link=" + linkToChild +
                '}';
    }

    /**
     * Links two {@link TopDownSpecification} objects via a parent-child link.
     */
    public static class Link {

        /** Name of the item that presents the link. It is an item in the parent. */
        @NotNull private final ItemPath itemPath;

        @NotNull private final TopDownSpecification child;

        Link(@NotNull ItemPath itemPath, @NotNull TopDownSpecification child) {
            this.itemPath = itemPath;
            this.child = child;
        }

        public @NotNull ItemPath getItemPath() {
            return itemPath;
        }

        public @NotNull TopDownSpecification getChild() {
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
