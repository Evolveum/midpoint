/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author mederly
 */
public abstract class SerializerTarget<T> {

    @NotNull public final PrismContextImpl prismContext;

    protected SerializerTarget(@NotNull PrismContextImpl prismContext) {
        this.prismContext = prismContext;
    }

    @NotNull
    abstract public T write(@NotNull RootXNodeImpl xroot, SerializationContext context) throws SchemaException;

    @NotNull
    abstract public T write(@NotNull List<RootXNodeImpl> roots, @Nullable QName aggregateElementName,
            @Nullable SerializationContext context) throws SchemaException;
}
