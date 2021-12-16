/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.prism.wrapper;

import java.util.Map;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.DeepCloneOperation;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;

import org.jetbrains.annotations.NotNull;

/**
 * @author skublik
 *
 */
public interface ResourceAttributeWrapper<T> extends PrismPropertyWrapper<T>, ResourceAttributeDefinition<T> {

    @Override
    ResourceAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation);
}
