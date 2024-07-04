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

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

/**
 * The enhanced definition of `attributes` container ({@link ShadowAttributesContainer}) in a {@link ShadowType} object.
 *
 * Being enhanced (relative to {@link PrismContainerDefinition}) means that it provides additional functionality
 * specific to shadows, like {@link #getAllIdentifiers()} and similar methods. Overall, it works with enhanced variants
 * of prism objects, like {@link ShadowAttribute}, {@link ShadowAttributeDefinition}, and so on.
 *
 * @author Radovan Semancik
 */
public interface ShadowAttributesContainerDefinition extends PrismContainerDefinition<ShadowAttributesType> {

    @Override
    ShadowAttributesComplexTypeDefinition getComplexTypeDefinition();

    /**
     * TODO review docs
     *
     * Returns the definition of primary identifier attributes of a resource object.
     *
     * May return empty set if there are no identifier attributes. Must not
     * return null.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * @return definition of identifier attributes
     * @throws IllegalStateException
     *             if there is no definition for the referenced attributed
     */
    Collection<? extends ShadowSimpleAttributeDefinition<?>> getPrimaryIdentifiers();

    /**
     * TODO review docs
     *
     * Returns the definition of secondary identifier attributes of a resource
     * object.
     *
     * May return empty set if there are no secondary identifier attributes.
     * Must not return null.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * @return definition of secondary identifier attributes
     * @throws IllegalStateException
     *             if there is no definition for the referenced attributed
     */
    Collection<? extends ShadowSimpleAttributeDefinition<?>> getSecondaryIdentifiers();

    Collection<? extends ShadowSimpleAttributeDefinition<?>> getAllIdentifiers();

    @NotNull
    ShadowAttributesContainer instantiate();

    @NotNull
    ShadowAttributesContainer instantiate(QName name);

    @NotNull
    ShadowAttributesContainerDefinition clone();

    <T> ShadowSimpleAttributeDefinition<T> findAttributeDefinition(ItemPath elementPath);

    @Override
    @NotNull List<? extends ItemDefinition<?>> getDefinitions();

    @NotNull List<? extends ShadowSimpleAttributeDefinition<?>> getSimpleAttributesDefinitions();

    @NotNull ResourceObjectDefinition getResourceObjectDefinition();

    boolean isUsedInSimpleAssociationObject();
}
