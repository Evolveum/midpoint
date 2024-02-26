/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.query.restriction;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.resolution.HqlDataInstance;

/**
 * Base for all item path-related restrictions, e.g. those that are based on item path that points to a JPA data node.
 */
public abstract class ItemRestriction<T extends ObjectFilter> extends Restriction<T> {

    /**
     * Item path (relative to parent restriction), copied from the appropriate filter.
     */
    protected final @NotNull ItemPath itemPath;

    /**
     * Item definition - necessary only for Any items.
     */
    protected final ItemDefinition<?> itemDefinition;

    /**
     * Information about resolved itemPath. Needed when accessing the data.
     * Contains also information on previous steps, useful to enable looking upwards via ".." operator.
     * Filled-in within interpret() method.
     */
    HqlDataInstance<?> hqlDataInstance;

    ItemRestriction(InterpretationContext context, T filter, @NotNull ItemPath itemPath,
            ItemDefinition<?> itemDefinition, JpaEntityDefinition baseEntityDefinition, Restriction<?> parent) {
        super(context, filter, baseEntityDefinition, parent);
        this.itemPath = Objects.requireNonNull(itemPath);
        this.itemDefinition = itemDefinition;
    }

    public @NotNull ItemPath getItemPath() {
        return itemPath;
    }

    HqlDataInstance<?> getHqlDataInstance() {
        return hqlDataInstance;
    }

    void setHqlDataInstance(HqlDataInstance<?> hqlDataInstance) {
        Objects.requireNonNull(hqlDataInstance);
        this.hqlDataInstance = hqlDataInstance;
    }
}
