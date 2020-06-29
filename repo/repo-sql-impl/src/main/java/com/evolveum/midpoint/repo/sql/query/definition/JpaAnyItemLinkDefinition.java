/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;

/**
 * Link from AnyContainer to specific item in this container.
 */
public class JpaAnyItemLinkDefinition extends JpaLinkDefinition<JpaDataNodeDefinition<?>> {

    private final RObjectExtensionType ownerType;
    private final ItemDefinition<?> itemDefinition;

    JpaAnyItemLinkDefinition(ItemDefinition<?> itemDefinition, String jpaName, CollectionSpecification collectionSpecification,
            RObjectExtensionType ownerType, JpaDataNodeDefinition<?> targetDefinition) {
        super(itemDefinition.getItemName(), jpaName, collectionSpecification, false, targetDefinition);
        this.ownerType = ownerType;
        this.itemDefinition = itemDefinition;
    }

    public RObjectExtensionType getOwnerType() {
        return ownerType;
    }

    public ItemDefinition<?> getItemDefinition() {
        return itemDefinition;
    }
}
