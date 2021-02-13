/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemSqlMapper;

/**
 * Common mapping functionality that covers the need for mapping from item paths
 * to table columns, but also to nested embedded mappings (e.g. metadata).
 */
public interface QueryModelMapping {

    /**
     * Returns {@link ItemSqlMapper} for provided {@link ItemName}.
     * This is later used to create {@link ItemFilterProcessor}
     */
    @NotNull ItemSqlMapper itemMapper(QName itemName) throws QueryException;

    @NotNull ItemRelationResolver<?> relationResolver(ItemName itemName)
            throws QueryException;
}
