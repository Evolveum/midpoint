/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface PrismReferenceDefinition extends ItemDefinition<PrismReference> {
    QName getTargetTypeName();

    @Deprecated
    QName getCompositeObjectElementName();

    boolean isComposite();

    @NotNull
    @Override
    PrismReference instantiate();

    @NotNull
    @Override
    PrismReference instantiate(QName name);

    @NotNull
    @Override
    PrismReferenceDefinition clone();
}
