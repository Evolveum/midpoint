/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.prism.DefinitionFragmentBuilder.fixed;
import static com.evolveum.midpoint.prism.DefinitionFragmentBuilder.unsupported;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.NativeShadowAttributeDefinition.NativeShadowAttributeDefinitionBuilder;

import com.evolveum.midpoint.util.DebugDumpable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ComplexTypeDefinition.ComplexTypeDefinitionLikeBuilder;
import com.evolveum.midpoint.prism.DisplayHint;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.SchemaMigration;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;

/**
 * Object class definition as seen by the connector (or manually configured via XSD).
 *
 * It contains only the native attribute and association definitions.
 */
public interface NativeObjectClassDefinition extends NativeObjectClassUcfDefinition, Cloneable, Serializable, DebugDumpable {

    @NotNull String getName();

    @NotNull QName getQName();

    @NotNull Collection<? extends NativeShadowItemDefinition> getItemDefinitions();

    @NotNull Collection<? extends NativeShadowAttributeDefinition<?>> getAttributeDefinitions();

    @NotNull Collection<? extends NativeShadowAssociationDefinition> getAssociationDefinitions();

    NativeObjectClassDefinition clone();

    default boolean isPrimaryIdentifier(@NotNull ItemName attrName) {
        return attrName.matches(getPrimaryIdentifierName()); // TODO case sensitiveness?
    }

    default boolean isSecondaryIdentifier(@NotNull ItemName attrName) {
        return attrName.matches(getSecondaryIdentifierName()); // TODO case sensitiveness?
    }

    NativeShadowAttributeDefinition<?> findAttributeDefinition(@NotNull QName attrName);

    NativeShadowAssociationDefinition findAssociationDefinition(@NotNull QName assocName);

    interface NativeObjectClassDefinitionBuilder
            extends NativeObjectClassUcfDefinition.Mutable.Delegable, ComplexTypeDefinitionLikeBuilder {

        void setResourceObject(boolean value);

        @Override
        <T> NativeShadowAttributeDefinitionBuilder<T> newPropertyLikeDefinition(QName elementName, QName typeName);

        //region Unsupported setters
        @Override
        default void setAbstract(boolean value) {
            fixed("abstract", value, false);
        }

        @Override
        default void setContainerMarker(boolean value) {
            fixed("containerMarker", value, true);
        }

        @Override
        default void setObjectMarker(boolean value) {
            fixed("objectMarker", value, false);
        }

        @Override
        default void setReferenceMarker(boolean value) {
            fixed("referenceMarker", value, false);
        }

        @Override
        default void setListMarker(boolean value) {
            fixed("listMarker", value, false);
        }

        @Override
        default void setExtensionForType(QName value) {
            unsupported("extensionForType", value);
        }

        @Override
        default void setDefaultItemTypeName(QName value) {
            unsupported("defaultItemTypeName", value);
        }

        @Override
        default void setDefaultNamespace(String value) {
            unsupported("defaultItemTypeName", value);
        }

        @Override
        default void setIgnoredNamespaces(List<String> value) {
            unsupported("defaultItemTypeName", value);
        }

        @Override
        default void setXsdAnyMarker(boolean value) {
            unsupported("xsdAnyMarker", value);
        }

        @Override
        default void setStrictAnyMarker(boolean value) {
            unsupported("strictXsdAnyMarker", value);
        }

        @Override
        default void addXmlAttributeDefinition(PrismPropertyDefinition<?> attributeDef) {
            unsupported("xmlAttributeDefinition", attributeDef);
        }

        @Override
        default void setRuntimeSchema(boolean value) {
            fixed("runtimeSchema", value, true);
        }

        @Override
        default void setDeprecated(boolean value) {
            unsupported("deprecated", value);
        }

        @Override
        default void setRemoved(boolean value) {
            unsupported("removed", value);
        }

        @Override
        default void setRemovedSince(String value) {
            unsupported("removedSince", value);
        }

        @Override
        default void setExperimental(boolean value) {
            unsupported("experimental", value);
        }

        @Override
        default void setPlannedRemoval(String plannedRemoval) {
            unsupported("plannedRemoval", plannedRemoval);
        }

        @Override
        default void setDeprecatedSince(String deprecatedSince) {
            unsupported("deprecatedSince", deprecatedSince);
        }

        @Override
        default void addSchemaMigration(SchemaMigration value) {
            unsupported("schemaMigration", value);
        }

        @Override
        default void setSchemaMigrations(List<SchemaMigration> value) {
            unsupported("schemaMigrations", value);
        }

        @Override
        default void setDisplayHint(DisplayHint displayHint) {
            unsupported("displayHint", displayHint);
        }

        @Override
        default void setEmphasized(boolean emphasized) {
            unsupported("emphasized", emphasized);
        }

        @Override
        default void setHelp(String help) {
            unsupported("help", help);
        }

        @Override
        default void setDocumentation(String documentation) {
            unsupported("documentation", documentation);
        }

        @Override
        default void setDiagrams(List<ItemDiagramSpecification> value) {
            unsupported("diagrams", value);
        }

        @Override
        default void setInstantiationOrder(Integer value) {
            unsupported("instantiationOrder", value);
        }

        @Override
        default void setSuperType(QName value) {
            unsupported("superType", value);
        }
        //endregion
    }
}
