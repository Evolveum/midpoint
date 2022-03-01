/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.schema.route.ItemRoute;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationItemTargetDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsCorrelatorType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves routes in correlation item definitions, trying to find referenced definitions
 * in "items" and "places".
 *
 * Very experimental.
 */
@Experimental
public class CorrelationItemRouteFinder {

    /**
     * Resolves a relative route (relative to the place for source items) to given source item.
     *
     * The route is taken either from the local item definition bean, or from referenced named item definition.
     */
    static ItemRoute findForSource(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext) throws ConfigurationException {
        if (itemBean.getRef() != null) {
            return ItemRoute.fromBean(
                    correlatorContext.getNamedItemSourceDefinition(itemBean.getRef()));
        } else if (itemBean.getSourcePath() != null) {
            return ItemRoute.fromPath(
                    itemBean.getSourcePath().getItemPath());
        } else if (itemBean.getPath() != null) {
            return ItemRoute.fromPath(
                    itemBean.getPath().getItemPath());
        } else {
            throw new ConfigurationException("Neither ref, nor path, nor sourcePath present in " + itemBean);
        }
    }

    /**
     * Resolves a relative route (relative to the place for primary target items) to given primary target item.
     *
     * The route is taken either from the local item definition bean, or from referenced named item definition.
     */
    static @NotNull ItemRoute findForPrimaryTargetRelative(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext) throws ConfigurationException {
        if (itemBean.getRef() != null) {
            return ItemRoute.fromBean(
                    correlatorContext.getNamedItemPrimaryTargetDefinition(itemBean.getRef()));
        } else if (itemBean.getPrimaryTargetPath() != null) {
            return ItemRoute.fromPath(
                    itemBean.getPrimaryTargetPath().getItemPath());
        } else if (itemBean.getPath() != null) {
            return ItemRoute.fromPath(
                    itemBean.getPath().getItemPath());
        } else {
            throw new ConfigurationException("Neither ref, nor path, nor primaryTargetPath present in " + itemBean);
        }
    }

    /**
     * Resolves a relative route (relative to the place for secondary target items) to given secondary target item.
     *
     * The route is taken either from the local item definition bean, or from referenced named item definition.
     *
     * Returns null if the item has no secondary target. (Either all items in given correlation should have one,
     * or none of them!)
     */
    static @Nullable ItemRoute findForSecondaryTargetRelative(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext) throws ConfigurationException {
        if (itemBean.getRef() != null) {
            CorrelationItemTargetDefinitionType definition =
                    correlatorContext.getNamedItemSecondaryTargetDefinition(itemBean.getRef());
            if (definition == null) {
                return null;
            } else {
                return ItemRoute.fromBean(definition);
            }
        } else if (itemBean.getSecondaryTargetPath() != null) {
            return ItemRoute.fromPath(
                    itemBean.getSecondaryTargetPath().getItemPath());
        } else {
            return null;
        }
    }
}
