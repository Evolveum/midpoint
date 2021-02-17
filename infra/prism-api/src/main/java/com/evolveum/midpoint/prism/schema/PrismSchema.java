/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.google.common.collect.Multimap;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * Schema as a collection of definitions. This is a midPoint-specific view of
 * schema definition. It is just a collection of definitions grouped under a
 * specific namespace.
 *
 * The schema and all the public classes in this package define a schema
 * meta-model. It is supposed to be used for run-time schema interpretation. It
 * will not be a convenient tool to work with static data model objects such as
 * user or role. But it is needed for interpreting dynamic schemas for resource
 * objects, extensions and so on.
 *
 * @author semancik
 * @author mederly
 */
public interface PrismSchema extends DebugDumpable, GlobalDefinitionsStore, DefinitionSearchImplementation, PrismContextSensitive, Freezable {

    /**
     * Returns schema namespace.
     *
     * All schema definitions are placed in the returned namespace.
     *
     * @return schema namespace
     */
    @NotNull
    String getNamespace();

    /**
     * Returns set of definitions.
     *
     * The set contains all definitions of all types that were parsed. Order of definitions is insignificant.
     *
     * @return set of definitions
     */
    @NotNull
    Collection<Definition> getDefinitions();

    /**
     * Returns set of definitions of a given type.
     *
     * The set contains all definitions of the given type that were parsed. Order of definitions is insignificant.
     *
     * @return set of definitions
     */
    @NotNull
    <T extends Definition> List<T> getDefinitions(@NotNull Class<T> type);

    @NotNull
    default List<PrismObjectDefinition> getObjectDefinitions() {
        return getDefinitions(PrismObjectDefinition.class);
    }

    @NotNull
    default List<ComplexTypeDefinition> getComplexTypeDefinitions() {
        return getDefinitions(ComplexTypeDefinition.class);
    }

    @NotNull
    Document serializeToXsd() throws SchemaException;

    boolean isEmpty();

    Multimap<QName, ItemDefinition<?>> getSubstitutions();
}
