/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Creates a set of objects on a dummy resource.
 */
public class DummyObjectsCreator<O extends DummyObject> {

    private final Class<O> type;

    /** Number of objects to be created. */
    private final int objectCount;

    /** Object name pattern, expecting single %d to be replaced by object number (starting from 0). */
    private final String namePattern;

    /** Supplier of the name. Alternative to {@link #namePattern}. */
    private final NameSupplier nameSupplier;

    /** Customizes instantiated object. */
    private final Customizer<O> customizer;

    /**
     * Controller of the respective dummy resource.
     */
    private final DummyResourceContoller controller;

    DummyObjectsCreator(Class<O> type, int objectCount, String namePattern, NameSupplier nameSupplier,
            Customizer<O> customizer, DummyResourceContoller controller) {
        this.type = type;
        this.objectCount = objectCount;
        this.namePattern = namePattern;
        this.nameSupplier = nameSupplier;
        this.customizer = customizer;
        this.controller = controller;
    }

    public static <O extends DummyObject> DummyObjectsCreatorBuilder<O> forType(@NotNull Class<O> type) {
        return new DummyObjectsCreatorBuilder<>(type);
    }

    public static DummyObjectsCreatorBuilder<DummyAccount> accounts() {
        return forType(DummyAccount.class);
    }

    public List<O> execute()
            throws CommonException, ConflictException, FileNotFoundException, SchemaViolationException,
            ObjectAlreadyExistsException, InterruptedException, ConnectException, ObjectDoesNotExistException {
        List<O> objects = new ArrayList<>(objectCount);

        for (int i = 0; i < objectCount; i++) {
            O object;
            try {
                object = type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new SystemException(e);
            }
            object.setName(createName(i));
            if (customizer != null) {
                customizer.customize(object, i);
            }
            addObject(object);
            objects.add(object);
        }

        System.out.printf("Created %d objects\n", objectCount);

        return objects;
    }

    private void addObject(O object) throws ConflictException, FileNotFoundException, SchemaViolationException,
            ObjectAlreadyExistsException, InterruptedException, ConnectException, ObjectDoesNotExistException {
        DummyResource resource = controller.getDummyResource();
        if (object instanceof DummyAccount) {
            resource.addAccount((DummyAccount) object);
        } else if (object instanceof DummyGroup) {
            resource.addGroup((DummyGroup) object);
        } else if (object instanceof DummyOrg) {
            resource.addOrg((DummyOrg) object);
        } else if (object instanceof DummyPrivilege) {
            resource.addPrivilege((DummyPrivilege) object);
        } else {
            throw new AssertionError(object);
        }
    }

    private String createName(int i) {
        if (nameSupplier != null) {
            return nameSupplier.apply(i);
        } else if (namePattern != null) {
            return String.format(namePattern, i);
        } else {
            return String.valueOf(i);
        }
    }

    /**
     * Supplies a name for an object (based on its number).
     */
    @FunctionalInterface
    public interface NameSupplier {
        String apply(int number);
    }

    /**
     * Customizes an object.
     */
    @FunctionalInterface
    public interface Customizer<O extends DummyObject> {
        void customize(O object, int number) throws CommonException;
    }
}
