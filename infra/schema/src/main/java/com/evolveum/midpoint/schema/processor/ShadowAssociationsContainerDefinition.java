/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationsType;

/**
 * Definition of a {@link ShadowAssociationsContainer}.
 *
 * Points to a {@link ComplexTypeDefinition} containing {@link ShadowAssociationDefinition}s that describe
 * the {@link ShadowAssociation}s.
 */
public interface ShadowAssociationsContainerDefinition extends PrismContainerDefinition<ShadowAssociationsType> {

    @NotNull
    ShadowAssociationsContainer instantiate();

    @NotNull
    ShadowAssociationsContainer instantiate(QName name);

    @NotNull
    ShadowAssociationsContainerDefinition clone();

    @Override
    @NotNull List<? extends ShadowAssociationDefinition> getDefinitions();

    @NotNull List<? extends ShadowAssociationDefinition> getAssociationsDefinitions();
}
