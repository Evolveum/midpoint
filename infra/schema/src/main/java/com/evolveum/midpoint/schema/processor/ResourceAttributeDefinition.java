/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface ResourceAttributeDefinition<T> extends PrismPropertyDefinition<T> {

    @NotNull
    ResourceAttribute<T> instantiate();

    @NotNull
    ResourceAttribute<T> instantiate(QName name);

    Boolean getReturnedByDefault();

    boolean isReturnedByDefault();

    boolean isPrimaryIdentifier(ResourceAttributeContainerDefinition objectDefinition);

    boolean isPrimaryIdentifier(ObjectClassComplexTypeDefinition objectDefinition);

    boolean isSecondaryIdentifier(ObjectClassComplexTypeDefinition objectDefinition);

    String getNativeAttributeName();

    String getFrameworkAttributeName();

    @NotNull
    @Override
    ResourceAttributeDefinition<T> clone();

    MutableResourceAttributeDefinition<T> toMutable();
}
