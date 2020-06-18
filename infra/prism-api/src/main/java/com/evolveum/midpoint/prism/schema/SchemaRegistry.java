/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author mederly
 */
public interface SchemaRegistry extends PrismContextSensitive, DebugDumpable, GlobalDefinitionsStore {

    DynamicNamespacePrefixMapper getNamespacePrefixMapper();

    void registerInvalidationListener(InvalidationListener listener);

    String getDefaultNamespace();

    void initialize() throws SAXException, IOException, SchemaException;

    javax.xml.validation.Schema getJavaxSchema();

    javax.xml.validation.Validator getJavaxSchemaValidator();

    PrismSchema getPrismSchema(String namespace);

    Collection<PrismSchema> getSchemas();

    Collection<SchemaDescription> getSchemaDescriptions();

    Collection<Package> getCompileTimePackages();

    ItemDefinition locateItemDefinition(@NotNull QName itemName,
            @Nullable ComplexTypeDefinition complexTypeDefinition,
            @Nullable Function<QName, ItemDefinition> dynamicDefinitionResolver);

    // TODO fix this temporary and inefficient implementation
    QName resolveUnqualifiedTypeName(QName type) throws SchemaException;

    QName qualifyTypeName(QName typeName) throws SchemaException;

    // current implementation tries to find all references to the child CTD and select those that are able to resolve path of 'rest'
    // fails on ambiguity
    // it's a bit fragile, as adding new references to child CTD in future may break existing code
    ComplexTypeDefinition determineParentDefinition(@NotNull ComplexTypeDefinition child, @NotNull ItemPath rest);

    PrismObjectDefinition determineReferencedObjectDefinition(@NotNull QName targetTypeName, ItemPath rest);

    Class<? extends ObjectType> getCompileTimeClassForObjectType(QName objectType);

    ItemDefinition findItemDefinitionByElementName(QName elementName, @Nullable List<String> ignoredNamespaces);

    <T> Class<T> determineCompileTimeClass(QName typeName);

    <T> Class<T> getCompileTimeClass(QName xsdType);

    PrismSchema findSchemaByCompileTimeClass(@NotNull Class<?> compileTimeClass);

    /**
     * Tries to determine type name for any class (primitive, complex one).
     * Does not use schemas (TODO explanation)
     * @param clazz
     * @return
     */
    QName determineTypeForClass(Class<?> clazz);

    @NotNull
    default QName determineTypeForClassRequired(Class<?> clazz) {
        QName typeName = determineTypeForClass(clazz);
        if (typeName != null) {
            return typeName;
        } else {
            throw new IllegalStateException("No type for " + clazz);
        }
    }

    /**
     * This method will try to locate the appropriate object definition and apply it.
     * @param container
     * @param type
     */
    <C extends Containerable> void applyDefinition(PrismContainer<C> container, Class<C> type) throws SchemaException;

    <C extends Containerable> void applyDefinition(PrismContainer<C> prismObject, Class<C> type, boolean force) throws SchemaException;

    <T extends Objectable> void applyDefinition(ObjectDelta<T> objectDelta, Class<T> type, boolean force) throws SchemaException;

    <C extends Containerable, O extends Objectable> void applyDefinition(PrismContainerValue<C> prismContainerValue,
            Class<O> type,
            ItemPath path, boolean force) throws SchemaException;

    <C extends Containerable> void applyDefinition(PrismContainerValue<C> prismContainerValue, QName typeName,
            ItemPath path, boolean force) throws SchemaException;

    <T extends ItemDefinition> T findItemDefinitionByFullPath(Class<? extends Objectable> objectClass, Class<T> defClass,
            QName... itemNames)
                            throws SchemaException;

    PrismSchema findSchemaByNamespace(String namespaceURI);

    SchemaDescription findSchemaDescriptionByNamespace(String namespaceURI);

    PrismSchema findSchemaByPrefix(String prefix);

    SchemaDescription findSchemaDescriptionByPrefix(String prefix);

    PrismObjectDefinition determineDefinitionFromClass(Class type);

//    boolean hasImplicitTypeDefinitionOld(QName elementName, QName typeName);

    boolean hasImplicitTypeDefinition(@NotNull QName itemName, @NotNull QName typeName);

    ItemDefinition resolveGlobalItemDefinition(QName itemName, @Nullable ComplexTypeDefinition complexTypeDefinition);

    // Takes XSD types into account as well
    <T> Class<T> determineClassForType(QName type);

    default <T> Class<T> determineClassForTypeRequired(QName type, Class<T> expected) {
        Class<?> clazz = determineClassForTypeRequired(type);
        if (!expected.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Expected to get " + expected + " but got " + clazz + " instead, for " + type);
        } else {
            //noinspection unchecked
            return (Class<T>) clazz;
        }
    }

    default <T> Class<T> determineClassForTypeRequired(QName type) {
        Class<T> clazz = determineClassForType(type);
        if (clazz != null) {
            return clazz;
        } else {
            throw new IllegalArgumentException("No class for " + type);
        }
    }

    // Takes XSD types into account as well
    Class<?> determineClassForItemDefinition(ItemDefinition<?> itemDefinition);

    <ID extends ItemDefinition> ID selectMoreSpecific(ID def1, ID def2)
            throws SchemaException;

    QName selectMoreSpecific(QName type1, QName type2) throws SchemaException;

    boolean isContainer(QName typeName);

    // TODO move to GlobalSchemaRegistry
    @NotNull
    <TD extends TypeDefinition> Collection<TD> findTypeDefinitionsByElementName(@NotNull QName name, @NotNull Class<TD> clazz);

    enum IsList {
        YES, NO, MAYBE
    }

    /**
     * Checks whether element with given (declared) xsi:type and name can be a heterogeneous list.
     *
     * @return YES if it is a list,
     *         NO if it's not,
     *         MAYBE if it probably is a list but some further content-based checks are needed
     */
    @NotNull
    IsList isList(@Nullable QName xsiType, @NotNull QName elementName);

    enum ComparisonResult {
        EQUAL,                    // types are equal
        NO_STATIC_CLASS,        // static class cannot be determined
        FIRST_IS_CHILD,            // first definition is a child (strict subtype) of the second
        SECOND_IS_CHILD,        // second definition is a child (strict subtype) of the first
        INCOMPATIBLE            // first and second are incompatible
    }
    /**
     * @return null means we cannot decide (types are different, and no compile time class for def1 and/or def2)
     */
    <ID extends ItemDefinition> ComparisonResult compareDefinitions(@NotNull ID def1, @NotNull ID def2)
            throws SchemaException;

    boolean isAssignableFrom(@NotNull Class<?> superType, @NotNull QName subType);

    boolean isAssignableFrom(@NotNull QName superType, @NotNull QName subType);

    /**
     * Returns most specific common supertype for these two. If any of input params is null, it means it is ignored
     * @return null if unification cannot be done (or if both input types are null)
     */
    QName unifyTypes(QName type1, QName type2);

    ItemDefinition<?> createAdHocDefinition(QName elementName, QName typeName, int minOccurs, int maxOccurs);

    interface InvalidationListener {
        void invalidate();
    }
}
