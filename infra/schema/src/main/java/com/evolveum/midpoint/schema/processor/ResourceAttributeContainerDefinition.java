/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * TODO
 */
public interface ResourceAttributeContainerDefinition extends PrismContainerDefinition<ShadowAttributesType> {
    @Override
    ObjectClassComplexTypeDefinition getComplexTypeDefinition();

    Collection<? extends ResourceAttributeDefinition> getPrimaryIdentifiers();

    Collection<? extends ResourceAttributeDefinition> getSecondaryIdentifiers();

    Collection<? extends ResourceAttributeDefinition> getAllIdentifiers();

    ResourceAttributeDefinition getDescriptionAttribute();

    ResourceAttributeDefinition getNamingAttribute();

    String getNativeObjectClass();

    boolean isDefaultInAKind();

    String getIntent();

    ShadowKindType getKind();

    ResourceAttributeDefinition getDisplayNameAttribute();

    @NotNull
    ResourceAttributeContainer instantiate();

    @NotNull
    ResourceAttributeContainer instantiate(QName name);

    @NotNull
    ResourceAttributeContainerDefinition clone();

    <T> ResourceAttributeDefinition<T> findAttributeDefinition(QName elementQName, boolean caseInsensitive);

    ResourceAttributeDefinition findAttributeDefinition(ItemPath elementPath);

    ResourceAttributeDefinition findAttributeDefinition(String elementLocalname);

    List<? extends ResourceAttributeDefinition> getAttributeDefinitions();

    // Only attribute definitions should be here.
    @Override
    List<? extends ResourceAttributeDefinition> getDefinitions();

    @NotNull <T extends ShadowType> PrismObjectDefinition<T> toShadowDefinition();
}
