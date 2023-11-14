/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.util.exception.CommonException;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.List;

public final class DummyObjectsCreatorBuilder<O extends DummyObject> {

    private final Class<O> type;
    private int objectCount;
    private String namePattern;
    private DummyObjectsCreator.NameSupplier nameSupplier;
    private DummyObjectsCreator.Customizer<O> customizer;
    private DummyResourceContoller controller;

    DummyObjectsCreatorBuilder(Class<O> type) {
        this.type = type;
    }

    public DummyObjectsCreatorBuilder<O> withObjectCount(int objectCount) {
        this.objectCount = objectCount;
        return this;
    }

    public DummyObjectsCreatorBuilder<O> withNamePattern(String namePattern) {
        this.namePattern = namePattern;
        return this;
    }

    public DummyObjectsCreatorBuilder<O> withNameSupplier(DummyObjectsCreator.NameSupplier nameSupplier) {
        this.nameSupplier = nameSupplier;
        return this;
    }

    public DummyObjectsCreatorBuilder<O> withCustomizer(DummyObjectsCreator.Customizer<O> customizer) {
        this.customizer = customizer;
        return this;
    }

    public DummyObjectsCreatorBuilder<O> withController(DummyResourceContoller controller) {
        this.controller = controller;
        return this;
    }

    public DummyObjectsCreator<O> build() {
        return new DummyObjectsCreator<>(type, objectCount, namePattern, nameSupplier, customizer, controller);
    }

    public List<O> execute()
            throws ConflictException, FileNotFoundException, CommonException, SchemaViolationException,
            ObjectAlreadyExistsException, InterruptedException, ConnectException, ObjectDoesNotExistException {
        return build().execute();
    }
}
