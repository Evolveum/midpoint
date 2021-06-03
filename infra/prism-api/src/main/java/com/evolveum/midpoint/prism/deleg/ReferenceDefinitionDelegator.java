/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.deleg;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;

public interface ReferenceDefinitionDelegator extends ItemDefinitionDelegator<PrismReference>, PrismReferenceDefinition {

    @Override
    PrismReferenceDefinition delegate();

    @Override
    default QName getTargetTypeName() {
        return delegate().getTargetTypeName();
    }

    @Deprecated
    @Override
    default QName getCompositeObjectElementName() {
        return delegate().getCompositeObjectElementName();
    }

    @Override
    default boolean isComposite() {
        return delegate().isComposite();
    }

    @Override
    default @NotNull PrismReference instantiate() {
        return delegate().instantiate();
    }

    @Override
    default @NotNull PrismReference instantiate(QName name) {
        return delegate().instantiate(name);
    }

}
