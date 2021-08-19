/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.ObjectCreator.Customizer;
import com.evolveum.midpoint.test.ObjectCreator.NameSupplier;
import com.evolveum.midpoint.test.ObjectCreator.RealCreator;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Builder for configured {@link ObjectCreator} objects.
 *
 * @param <O> type of objects to be created
 */
public final class ObjectCreatorBuilder<O extends ObjectType> {

    // For the description of parameters please see the ObjectCreator class.

    @NotNull private final Class<O> type;
    private int objectCount;
    private String namePattern;
    private NameSupplier nameSupplier;
    private Customizer<O> customizer;
    private RealCreator<O> realCreator; // TODO name

    ObjectCreatorBuilder(@NotNull Class<O> type) {
        this.type = type;
    }

    public ObjectCreatorBuilder<O> withObjectCount(int objectCount) {
        this.objectCount = objectCount;
        return this;
    }

    public ObjectCreatorBuilder<O> withNamePattern(String namePattern) {
        this.namePattern = namePattern;
        return this;
    }

    public ObjectCreatorBuilder<O> withNameSupplier(NameSupplier nameSupplier) {
        this.nameSupplier = nameSupplier;
        return this;
    }

    public ObjectCreatorBuilder<O> withCustomizer(Customizer<O> customizer) {
        this.customizer = customizer;
        return this;
    }

    public ObjectCreatorBuilder<O> withRealCreator(RealCreator<O> realCreator) {
        this.realCreator = realCreator;
        return this;
    }

    public ObjectCreator<O> build() {
        return new ObjectCreator<>(type, objectCount, namePattern, nameSupplier, customizer, realCreator);
    }

    public List<O> execute(OperationResult result) throws CommonException {
        return build().execute(result);
    }
}
