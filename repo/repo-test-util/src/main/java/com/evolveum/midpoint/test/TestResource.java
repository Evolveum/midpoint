/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Representation of any prism object in tests.
 */
@Experimental
public class TestResource<T extends ObjectType> {

    @NotNull public final File dir;
    @NotNull public final File file;
    public final String oid;
    public PrismObject<T> object;

    public TestResource(@NotNull File dir, @NotNull String fileName) {
        this(dir, fileName, null);
    }

    public TestResource(@NotNull File dir, @NotNull String fileName, String oid) {
        this.dir = dir;
        this.file = new File(dir, fileName);
        this.oid = oid;
    }

    public String getNameOrig() {
        return object.getName().getOrig();
    }

    public T getObjectable() {
        return object.asObjectable();
    }

    public PrismObject<T> getObject() {
        return object;
    }

    public Class<T> getObjectClass() {
        return object.getCompileTimeClass();
    }

    @Override
    public String toString() {
        return object != null ? object.toString() : file + " (" + oid + ")";
    }

    public ObjectReferenceType ref() {
        return ObjectTypeUtil.createObjectRef(object, SchemaConstants.ORG_DEFAULT);
    }

    public void read() throws SchemaException, IOException {
        object = PrismContext.get().parserFor(file).parse();
    }

    public Class<T> getType() throws SchemaException, IOException {
        if (object == null) {
            read();
        }
        //noinspection unchecked
        return (Class<T>) object.asObjectable().getClass();
    }

    public void importObject(Task task, OperationResult result) throws CommonException, IOException {
        if (object == null) {
            read();
        }
        TestSpringBeans.getObjectImporter()
                .importObject(object, task, result);
    }

    /**
     * Reloads the object from the repository.
     */
    public void reload(OperationResult result) throws SchemaException, IOException, ObjectNotFoundException {
        object = TestSpringBeans.getCacheRepositoryService()
                .getObject(getType(), oid, null, result);
    }

    public static void read(TestResource<?>... resources) {
        Arrays.asList(resources).forEach(r -> {
            try {
                r.read();
            } catch (SchemaException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
