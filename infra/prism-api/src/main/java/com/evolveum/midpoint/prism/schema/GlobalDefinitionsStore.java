/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.schema;

import com.evolveum.midpoint.prism.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * FIXME Creation of this interface was most probably a design mistake. We should decide
 *  what its future should be.
 *
 * Used to retrieve definition from 'global definition store' - i.e. store that contains a group of related definition(s),
 * sharing e.g. a common namespace. Such stores are prism schemas and schema registry itself.
 *
 * Note: although all of these methods are '@Nullable', we don't mark them as such, to avoid false 'may produce NPE'
 * warnings for cases that will never produce nulls (like searching for known items/CTDs).
 */
@SuppressWarnings("unused")
public interface GlobalDefinitionsStore extends DefinitionsStore {

    // core methods

    /**
     * Looking up item definitions by compile-time class. So, for example having AssignmentType.class
     * we try to find a definition of "assignment" item.
     *
     * BEWARE. This method is unsound. There might be many items of AssignmentType.class.
     */
    @NotNull
    <ID extends ItemDefinition> List<ID> findItemDefinitionsByCompileTimeClass(
            @NotNull Class<?> compileTimeClass, @NotNull Class<ID> definitionClass);

    /**
     * Looking up item definition by type name. So, for example having c:AssignmentType
     * we try to find a definition of "assignment" item.
     *
     * BEWARE. This method is unsound. There might be many items with c:AssignmentType type.
     */
    <ID extends ItemDefinition> ID findItemDefinitionByType(@NotNull QName typeName, @NotNull Class<ID> definitionClass);

    /**
     * Looking up item definitions by element name. The name can be qualified or unqualified.
     * In the latter case there can be more than one definition returned.
     */
    @NotNull
    <ID extends ItemDefinition> List<ID> findItemDefinitionsByElementName(@NotNull QName elementName, @NotNull Class<ID> definitionClass);

    default <C extends Containerable> ComplexTypeDefinition findComplexTypeDefinitionByCompileTimeClass(@NotNull Class<C> compileTimeClass) {
        return findTypeDefinitionByCompileTimeClass(compileTimeClass, ComplexTypeDefinition.class);
    }

    <TD extends TypeDefinition> TD findTypeDefinitionByCompileTimeClass(@NotNull Class<?> compileTimeClass, @NotNull Class<TD> definitionClass);

    <TD extends TypeDefinition> TD findTypeDefinitionByType(@NotNull QName typeName, @NotNull Class<TD> definitionClass);

    @NotNull
    <TD extends TypeDefinition> Collection<? extends TD> findTypeDefinitionsByType(@NotNull QName typeName, @NotNull Class<TD> definitionClass);

    // non-core (derived) methods

    @NotNull
    default Collection<? extends TypeDefinition> findTypeDefinitionsByType(@NotNull QName typeName) {
        return findTypeDefinitionsByType(typeName, TypeDefinition.class);
    }

    @NotNull
    default List<ItemDefinition> findItemDefinitionsByElementName(@NotNull QName elementName) {
        return findItemDefinitionsByElementName(elementName, ItemDefinition.class);
    }

    default <ID extends ItemDefinition> ID findItemDefinitionByElementName(@NotNull QName elementName, @NotNull Class<ID> definitionClass) {
        return DefinitionStoreUtils.getOne(findItemDefinitionsByElementName(elementName, definitionClass));
    }

    default <ID extends ItemDefinition> ID findItemDefinitionByCompileTimeClass(
            @NotNull Class<?> compileTimeClass, @NotNull Class<ID> definitionClass) {
        return DefinitionStoreUtils.getOne(findItemDefinitionsByCompileTimeClass(compileTimeClass, definitionClass));
    }

    @SuppressWarnings("unchecked")
    default <O extends Objectable> PrismObjectDefinition<O> findObjectDefinitionByCompileTimeClass(@NotNull Class<O> compileTimeClass) {
        return findItemDefinitionByCompileTimeClass(compileTimeClass, PrismObjectDefinition.class);
    }

    @SuppressWarnings("unchecked")
    default <O extends Objectable> PrismObjectDefinition<O> findObjectDefinitionByType(@NotNull QName typeName) {
        return findItemDefinitionByType(typeName, PrismObjectDefinition.class);
    }

    @SuppressWarnings("unchecked")
    default <O extends Objectable> PrismObjectDefinition<O> findObjectDefinitionByElementName(@NotNull QName elementName) {
        return findItemDefinitionByElementName(elementName, PrismObjectDefinition.class);
    }

    // PrismContainer-related

    @SuppressWarnings("unchecked")
    default <C extends Containerable> PrismContainerDefinition<C> findContainerDefinitionByCompileTimeClass(@NotNull Class<C> compileTimeClass) {
        return findItemDefinitionByCompileTimeClass(compileTimeClass, PrismContainerDefinition.class);
    }

    @SuppressWarnings("unchecked")
    default <C extends Containerable> PrismContainerDefinition<C> findContainerDefinitionByType(@NotNull QName typeName) {
        return findItemDefinitionByType(typeName, PrismContainerDefinition.class);
    }

    @SuppressWarnings("unchecked")
    default <C extends Containerable> PrismContainerDefinition<C> findContainerDefinitionByElementName(@NotNull QName elementName) {
        return findItemDefinitionByElementName(elementName, PrismContainerDefinition.class);
    }

    // PrismReference-related

    default PrismReferenceDefinition findReferenceDefinitionByElementName(@NotNull QName elementName) {
        return findItemDefinitionByElementName(elementName, PrismReferenceDefinition.class);
    }

    // PrismProperty-related

    default PrismPropertyDefinition findPropertyDefinitionByElementName(@NotNull QName elementName) {
        return findItemDefinitionByElementName(elementName, PrismPropertyDefinition.class);
    }

    // Item-related

    default ItemDefinition findItemDefinitionByType(@NotNull QName typeName) {
        return findItemDefinitionByType(typeName, ItemDefinition.class);
    }

    default ItemDefinition findItemDefinitionByElementName(@NotNull QName elementName) {
        return findItemDefinitionByElementName(elementName, ItemDefinition.class);
    }

    // TypeDefinition-related

    default ComplexTypeDefinition findComplexTypeDefinitionByType(@NotNull QName typeName) {
        return findTypeDefinitionByType(typeName, ComplexTypeDefinition.class);
    }

    default SimpleTypeDefinition findSimpleTypeDefinitionByType(@NotNull QName typeName) {
        return findTypeDefinitionByType(typeName, SimpleTypeDefinition.class);
    }

    default TypeDefinition findTypeDefinitionByType(@NotNull QName typeName) {
        return findTypeDefinitionByType(typeName, TypeDefinition.class);
    }
}
