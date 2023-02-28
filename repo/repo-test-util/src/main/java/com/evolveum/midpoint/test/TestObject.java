/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import com.evolveum.axiom.concepts.Lazy;

import com.evolveum.midpoint.schema.util.SimpleObjectResolver;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SystemException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.commons.io.input.ReaderInputStream;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Representation of any prism object-based test "resource" (file, class path resource, ...) in tests.
 */
public class TestObject<T extends ObjectType> {

    public final TestObjectSource source;

    public final String oid;

    private final Lazy.Supplier<PrismObject<T>> originalSupplier = this::parse;

    /**
     * The object - either literally as it is defined in the resource, or replaced by a processed form: for example,
     * a {@link ResourceType} may be imported into the repository, tested (the schema and capabilities being filled in),
     * and then fetched and stored here.
     */
    @NotNull private final Lazy<PrismObject<T>> object = Lazy.from(originalSupplier);

    protected TestObject(TestObjectSource source, String oid) {
        this.source = source;
        this.oid = oid;
    }

    public static <T extends ObjectType> TestObject<T> classPath(@NotNull String dir, @NotNull String name, String oid) {
        return new TestObject<>(
                new ClassPathBasedTestObjectSource(dir, name),
                oid);
    }

    public static <T extends ObjectType> TestObject<T> file(@NotNull File dir, @NotNull String name) {
        return file(dir, name, null);
    }

    public static <T extends ObjectType> TestObject<T> file(@NotNull File dir, @NotNull String name, String oid) {
        return new TestObject<>(
                new FileBasedTestObjectSource(dir, name),
                oid);
    }

    public @NotNull PrismObject<T> parse() {
        try (InputStream inputStream = getInputStream()) {
            PrismObject<T> parsed = PrismContext.get()
                    .parserFor(inputStream)
                    .xml()
                    .parse();
            customizeParsed(parsed);
            return parsed;
        } catch (SchemaException | IOException e) {
            throw SystemException.unexpected(e, "when parsing " + this);
        }
    }

    /**
     * Forgets any value that could be changed using {@link #set(PrismObject)} method call, e.g. resource definition after
     * being tested and re-fetched from the repo.
     *
     * Necessary e.g. because `connectorRef` should be resolved anew during new test class execution.
     * (Some test resources are shared among more specific test classes.)
     *
     * The recommended way is to use {@link #getFresh()} when we need to be sure to get the freshly parsed version.
     * (An alternative is to call {@link #parse()} directly - but that does not invalidate the cached content.)
     */
    public void reset() {
        object.set(originalSupplier);
    }

    /** Does dynamic changes after parsing. Used in subclasses. */
    protected void customizeParsed(PrismObject<T> parsed) {
    }

    /**
     * Returns cloned value of the object (to avoid unintentional modifications).
     * (Probably it is not necessary to optimize this e.g. by distinguishing read-only from read/write use.)
     */
    public @NotNull PrismObject<T> get() {
        return object.get().clone();
    }

    public void set(@NotNull PrismObject<T> value) {
        object.set(value);
    }

    public @NotNull PrismObject<T> getFresh() {
        reset();
        return get();
    }

    @Deprecated // use get()
    public @NotNull PrismObject<T> getObject() {
        return get();
    }

    public @NotNull T getObjectable() {
        return get().asObjectable();
    }

    public @NotNull String getNameOrig() {
        return getObjectable().getName().getOrig();
    }

    public ObjectReferenceType ref() {
        return ObjectTypeUtil.createObjectRef(get(), SchemaConstants.ORG_DEFAULT);
    }

    public Class<T> getType() {
        //noinspection unchecked
        return (Class<T>) getObjectable().getClass();
    }

    /**
     * Imports the object (via `model` API) but does not load it back.
     */
    public void importObject(Task task, OperationResult result) throws CommonException {
        TestSpringBeans.getObjectImporter()
                .importObject(getFresh(), task, result);
    }

    /** As {@link #reload(SimpleObjectResolver, OperationResult)} but uses default repo-based object resolver. */
    public void reload(OperationResult result) throws SchemaException, ObjectNotFoundException {
        reload(RepoSimpleObjectResolver.get(), result);
    }

    /** Reloads the object from the repository. (Updates also other relevant structures in subclasses.) */
    public void reload(SimpleObjectResolver resolver, OperationResult result) throws SchemaException, ObjectNotFoundException {
        PrismObject<T> reloaded = resolver.getObject(getType(), oid, null, result);
        set(reloaded);
        afterReload(result);
    }

    /** Executes any custom actions after the resource is reloaded. */
    protected void afterReload(OperationResult result) {
    }

    @Deprecated // TODO remove
    public void importAndReload(Task task, OperationResult result) throws CommonException {
        importAndReload(RepoSimpleObjectResolver.get(), task, result);
    }

    /**
     * Imports the object (using appropriate importer e.g. model importer) and reloads it - to have all the metadata.
     *
     * Currently requires `model` to be present.
     *
     * @param resolver used for reloading the object (with its own task!)
     */
    @Deprecated // TODO remove
    public void importAndReload(SimpleObjectResolver resolver, Task task, OperationResult result) throws CommonException {
        importObject(task, result);
        reload(resolver, result);
    }

    public static void getAll(TestObject<?>... resources) {
        Arrays.asList(resources).forEach(r -> r.get());
    }

    public @NotNull InputStream getInputStream() throws IOException {
        return source.getInputStream();
    }

    @Override
    public String toString() {
        return object.isUnwrapped() ? String.valueOf(object.get()) : getDescription();
    }

    public @NotNull String getDescription() {
        return source + " (" + oid + ")";
    }

    /**
     * Normally we should throw {@link CommonException} only. But {@link DummyTestResource} throws
     * all kinds of exceptions, hence {@link Exception} is declared here.
     */
    public void init(AbstractIntegrationTest test, Task task, OperationResult result) throws Exception {
        commonInit(test, task, result);
    }

    void commonInit(AbstractIntegrationTest test, Task task, OperationResult result) throws CommonException {
        test.registerTestObjectUsed(this);
        importObject(task, result);
    }

    public interface TestObjectSource {
        @NotNull InputStream getInputStream() throws IOException;
    }

    public static class ClassPathBasedTestObjectSource implements TestObjectSource {

        @NotNull private final String name;

        ClassPathBasedTestObjectSource(@NotNull String dir, @NotNull String name) {
            this.name = dir + "/" + name;
        }

        @Override
        public @NotNull InputStream getInputStream() throws IOException {
            return MiscUtil.requireNonNull(
                    getClass().getClassLoader().getResourceAsStream(name),
                    () -> new IllegalStateException("No resource '" + name + "' was found"));
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class FileBasedTestObjectSource implements TestObjectSource {

        @NotNull private final File file;

        public FileBasedTestObjectSource(@NotNull File dir, @NotNull String fileName) {
            this.file = new File(dir, fileName);
        }

        public @NotNull File getFile() {
            return file;
        }

        @Override
        public @NotNull InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public String toString() {
            return file.toString();
        }
    }

    @Experimental
    public static class InMemoryTestObjectSource implements TestObjectSource {

        @NotNull private final String description;
        @NotNull private final String string;

        InMemoryTestObjectSource(@NotNull ObjectType object) {
            if (object.getOid() == null) {
                // Some objects do need OIDs, for example when reloading a task.
                // As a temporary workaround, let us provide one here if there's none.
                // TODO reconsider this
                object.setOid(UUID.randomUUID().toString());
            }
            this.description = object.toString();
            try {
                this.string = PrismContext.get().xmlSerializer().serialize(object.asPrismObject());
            } catch (SchemaException e) {
                throw SystemException.unexpected(e, "when serializing test object " + description);
            }
        }

        @Override
        public @NotNull InputStream getInputStream() throws IOException {
            return new ReaderInputStream(
                    new StringReader(string), StandardCharsets.UTF_8);
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
