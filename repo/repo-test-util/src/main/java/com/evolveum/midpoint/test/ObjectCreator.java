/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a set of objects in repository or on resource.
 *
 * @param <O> type of objects to be created
 */
public class ObjectCreator<O extends ObjectType> {

    /** Type of objects to be created. */
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
     * Executes the real creation in repository or on resource.
     *
     * TODO devise a better class/property name
     */
    private final RealCreator<O> realCreator;

    ObjectCreator(Class<O> type, int objectCount, String namePattern, NameSupplier nameSupplier,
            Customizer<O> customizer, RealCreator<O> realCreator) {
        this.type = type;
        this.objectCount = objectCount;
        this.namePattern = namePattern;
        this.nameSupplier = nameSupplier;
        this.customizer = customizer;
        this.realCreator = realCreator;
    }

    public static <O extends ObjectType> ObjectCreatorBuilder<O> forType(@NotNull Class<O> type) {
        return new ObjectCreatorBuilder<>(type);
    }

    public List<O> execute(OperationResult result) throws CommonException {
        List<O> objects = new ArrayList<>(objectCount);

        long start = System.currentTimeMillis();

        for (int i = 0; i < objectCount; i++) {
            O object;
            try {
                object = type.getDeclaredConstructor(PrismContext.class).newInstance(PrismContext.get());
            } catch (Exception e) {
                throw new SystemException(e);
            }
            object.setName(PolyStringType.fromOrig(createName(i)));
            if (customizer != null) {
                customizer.customize(object, i);
            }
            realCreator.create(object, result);
            objects.add(object);
        }

        long duration = System.currentTimeMillis() - start;

        System.out.printf("Created %d objects in %d milliseconds (%,.1f ms per object)\n",
                objectCount, duration, (float) duration / (float) objectCount);

        return objects;
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
    public interface Customizer<O extends ObjectType> {
        void customize(O object, int number) throws CommonException;
    }

    /**
     * Creates an object in repo or on resource.
     */
    @FunctionalInterface
    public interface RealCreator<O extends ObjectType> {
        void create(O object, OperationResult result) throws CommonException;
    }
}
