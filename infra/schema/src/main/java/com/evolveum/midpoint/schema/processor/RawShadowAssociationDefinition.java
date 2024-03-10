/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.AbstractFreezable;
import com.evolveum.midpoint.prism.impl.ItemDefinitionImpl;

/**
 * "Raw" definition of a shadow association, i.e. the one obtained from the connector.
 *
 * To be used _solely_ within {@link ShadowAssociationDefinition}.
 *
 * Unlike {@link RawResourceAttributeDefinition}, we do not extend {@link ItemDefinitionImpl} here.
 * This is because there are a lot of its features we don't need here. This decision may be reconsidered.
 */
public class RawShadowAssociationDefinition
        extends AbstractFreezable
        implements ResourceItemUcfDefinition.Delegable, ResourceItemUcfDefinition.Mutable.Delegable,
        ResourceItemPrismDefinition, ResourceItemPrismDefinition.Mutable.Delegable {

    @Serial private static final long serialVersionUID = 1259898180994611076L;

    /** MidPoint name. */
    @NotNull private final QName itemName;

    @NotNull private final ResourceItemUcfDefinitionData ucfData = new ResourceItemUcfDefinitionData();

    @NotNull private final ResourceItemPrismDefinitionData prismData = new ResourceItemPrismDefinitionData();

    public RawShadowAssociationDefinition(@NotNull QName itemName) {
        this.itemName = itemName;
    }

    @Override
    public ResourceItemUcfDefinitionData ucfData() {
        return ucfData;
    }

    @Override
    public ResourceItemPrismDefinitionData prismData() {
        return prismData;
    }

    public @NotNull QName getItemName() {
        return itemName;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(this);
    }

    @Override
    public String toString() {
        return "rawSAD: " + itemName.getLocalPart() + prismData + ucfData;
    }
}
