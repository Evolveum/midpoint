/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.util.MiscUtil.requireNonNull;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.NativeObjectClassDefinition.NativeObjectClassDefinitionBuilder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * A schema covering the whole resource.
 *
 * It contains object classes and types definitions and reference types definitions.
 * It refers to the {@link NativeResourceSchema} upon which it was built.
 *
 * This interface contains a lot of methods that try to find object type/class definition matching criteria.
 *
 * NOTE: There can be schemas that contain no refined definitions. Either the resource definition
 * contains no `schemaHandling`, or we intentionally want to avoid them. See {@link BareResourceSchema}.
 *
 * NOTE: Resolution of definitions is a complex process. So it's delegated to {@link ResourceObjectDefinitionResolver}.
 *
 * @author semancik
 */
public interface ResourceSchema extends PrismSchema, Cloneable, LayeredDefinition {

    Trace LOGGER = TraceManager.getTrace(ResourceSchema.class);

    //region Simple type/class definitions retrieval
    /** Returns definitions for all the object classes and types (currently that should be all definitions). */
    default @NotNull Collection<ResourceObjectDefinition> getResourceObjectDefinitions() {
        return getDefinitions(ResourceObjectDefinition.class);
    }

    /** Returns definitions for all the object classes. */
    default @NotNull Collection<ResourceObjectClassDefinition> getObjectClassDefinitions() {
        return getDefinitions(ResourceObjectClassDefinition.class);
    }

    default int getObjectClassDefinitionsCount() {
        return getObjectClassDefinitions().size(); // implement more efficiently if needed
    }

    /** Returns definitions for all the object types. */
    default @NotNull Collection<ResourceObjectTypeDefinition> getObjectTypeDefinitions() {
        return getDefinitions(ResourceObjectTypeDefinition.class);
    }

    /** Returns definitions for all types with given kind. (If null, returns all types.) */
    default @NotNull List<? extends ResourceObjectTypeDefinition> getObjectTypeDefinitions(@Nullable ShadowKindType kind) {
        return getObjectTypeDefinitions(def -> def.matchesKind(kind));
    }

    /** Returns all matching object type definitions. */
    default @NotNull List<ResourceObjectTypeDefinition> getObjectTypeDefinitions(
            @NotNull Predicate<ResourceObjectTypeDefinition> predicate) {
        return getObjectTypeDefinitions().stream()
                .filter(predicate)
                .toList();
    }

    /** Returns definition of the given type. No hacks/guesses here. */
    default @Nullable ResourceObjectTypeDefinition getObjectTypeDefinition(
            @NotNull ShadowKindType kind, @NotNull String intent) {
        List<ResourceObjectTypeDefinition> matching = getObjectTypeDefinitions().stream()
                .filter(def -> def.matches(kind, intent))
                .collect(Collectors.toList());
        return MiscUtil.extractSingleton(
                matching,
                () -> new IllegalStateException("Multiple type definitions for " + kind + "/" + intent + ": " + matching));
    }

    /** Returns definition of the given type. No hacks/guesses here. */
    default @Nullable ResourceObjectTypeDefinition getObjectTypeDefinition(
            @NotNull ResourceObjectTypeIdentification identification) {
        return getObjectTypeDefinition(identification.getKind(), identification.getIntent());
    }

    default @NotNull ResourceObjectTypeDefinition getObjectTypeDefinitionRequired(
            @NotNull ResourceObjectTypeIdentification identification) {
        return stateNonNull(
                getObjectTypeDefinition(identification.getKind(), identification.getIntent()),
                "No object type definition for %s in %s", identification, this);
    }
    //endregion

    //region More complex definitions lookup methods
    /**
     * Returns the "default for kind" type definition for given kind.
     *
     * Applies `account/default` hack if nothing relevant can be found.
     */
    default @Nullable ResourceObjectDefinition findDefaultDefinitionForKind(@NotNull ShadowKindType kind) {
        return ResourceObjectDefinitionResolver.findObjectDefinition(
                this, kind, null, null);
    }

    /**
     * As {@link #findDefaultDefinitionForKind(ShadowKindType)} but the definition must exist.
     */
    default @NotNull ResourceObjectDefinition findDefaultDefinitionForKindRequired(@NotNull ShadowKindType kind) {
        return stateNonNull(
                findDefaultDefinitionForKind(kind),
                () -> "No default definition for " + kind + " could be found in " + this);
    }

    /**
     * Returns the definition for known kind and intent.
     *
     * Applies `account/default` hack if nothing relevant can be found.
     */
    default @Nullable ResourceObjectDefinition findObjectDefinition(@NotNull ShadowKindType kind, @NotNull String intent) {
        return ResourceObjectDefinitionResolver.findObjectDefinition(
                this, kind, intent, null);
    }

    /**
     * As {@link #findObjectDefinition(ShadowKindType, String)} but the definition must exist.
     */
    default @NotNull ResourceObjectDefinition findObjectDefinitionRequired(
            @NotNull ShadowKindType kind, @NotNull String intent) {
        return stateNonNull(
                findObjectDefinition(kind, intent),
                () -> "No object type/class definition for " + kind + "/" + intent + " in " + this);
    }

    /**
     * As {@link #findObjectDefinition(ShadowKindType, String)} but with aggregate representation of type identification.
     *
     * Applies `account/default` hack if nothing relevant can be found.
     */
    default @Nullable ResourceObjectDefinition findObjectDefinition(
            @NotNull ResourceObjectTypeIdentification typeIdentification) {
        return findObjectDefinition(typeIdentification.getKind(), typeIdentification.getIntent());
    }

    /**
     * As {@link #findObjectDefinition(ResourceObjectTypeIdentification)} but the definition must exist.
     *
     * Applies `account/default` hack if nothing relevant can be found.
     */
    default @NotNull ResourceObjectDefinition findObjectDefinitionRequired(
            @NotNull ResourceObjectTypeIdentification typeIdentification) {
        return stateNonNull(
                findObjectDefinition(typeIdentification),
                () -> "No object type/class definition for " + typeIdentification + " in " + this);
    }

    /**
     * Returns a type or class definition for a given object class:
     *
     * - if there's a "default for class" type defined, it is returned (this is a kind of pre-4.5 behavior)
     * - otherwise, the object class definition is returned (if there's any)
     */
    default @Nullable ResourceObjectDefinition findDefinitionForObjectClass(@NotNull QName name) {
        ResourceObjectTypeDefinition defaultTypeDef =
                ResourceObjectDefinitionResolver.findDefaultObjectTypeDefinitionForObjectClass(this, name);
        if (defaultTypeDef != null) {
            return defaultTypeDef;
        } else {
            return findObjectClassDefinition(name);
        }
    }

    /**
     * As {@link #findDefinitionForObjectClass(QName)} but throws an exception if there's no suitable definition.
     */
    default @NotNull ResourceObjectDefinition findDefinitionForObjectClassRequired(@NotNull QName name) throws SchemaException {
        return requireNonNull(
                findDefinitionForObjectClass(name),
                () -> "No definition for object class " + name + " in " + this);
    }

    /**
     * Returns {@link ResourceObjectClassDefinition} (raw or refined) for a given object class name.
     */
    default @Nullable ResourceObjectClassDefinition findObjectClassDefinition(@NotNull QName name) {
        return MiscUtil.extractSingleton(
                getObjectClassDefinitions().stream()
                        .filter(def -> QNameUtil.match(def.getTypeName(), name))
                        .collect(Collectors.toList()),
                () -> new IllegalStateException("More than one definition of object class " + name + " in " + this));
    }

    /**
     * The same as {@link #findObjectClassDefinition(QName)} but throws an exception if there's no such definition.
     */
    default @NotNull ResourceObjectClassDefinition findObjectClassDefinitionRequired(@NotNull QName name)
            throws SchemaException {
        return findObjectClassDefinitionRequired(name, () -> "");
    }

    default @NotNull ResourceObjectClassDefinition findObjectClassDefinitionRequired(
            @NotNull QName name, @NotNull Supplier<String> contextSupplier)
            throws SchemaException {
        return MiscUtil.requireNonNull(
                findObjectClassDefinition(name),
                () -> "Object class " + name + " not found in " + this + contextSupplier.get());
    }

    /**
     * As {@link #findObjectDefinition(ShadowKindType, String)} but checks the object class compatibility (if object class
     * name is provided).
     */
    default @Nullable ResourceObjectDefinition findObjectDefinition(
            @NotNull ShadowKindType kind,
            @NotNull String intent,
            @Nullable QName objectClassName) {
        return ResourceObjectDefinitionResolver.findObjectDefinition(this, kind, intent, objectClassName);
    }
    //endregion

    //region Definition lookup in specific contexts (construction bean, shadow, ...)
    /**
     * Finds a definition for {@link ConstructionType}. The method is different from the ones looking for
     * kind/intent because here is a special handling of the default values.
     */
    default ResourceObjectDefinition findDefinitionForConstruction(@NotNull ConstructionType construction) {
        return ResourceObjectDefinitionResolver.findForConstruction(this, construction);
    }

    /**
     * As {@link #findDefinitionForConstruction(ConstructionType)} but throws an exception if the definition is not there.
     */
    default @NotNull ResourceObjectDefinition findDefinitionForConstructionRequired(
            @NotNull ConstructionType constructionBean,
            @NotNull Supplier<String> contextSupplier) throws SchemaException {

        ResourceObjectDefinition definition = findDefinitionForConstruction(constructionBean);
        if (definition != null) {
            return definition;
        }

        ShadowKindType kind = defaultIfNull(constructionBean.getKind(), ShadowKindType.ACCOUNT);
        String intent = constructionBean.getIntent(); // Null value is interpreted as default-for-kind here.
        if (intent != null) {
            throw new SchemaException("No " + kind + " type with intent '" + intent + "' found in " + contextSupplier.get());
        } else {
            throw new SchemaException("No default " + kind + " type found in " + contextSupplier.get());
        }
    }

    /**
     * Returns appropriate {@link ResourceObjectDefinition} for given shadow. We are not too strict here.
     * Unknown kind/intent values are ignored (treated like null). Incomplete classification is considered
     * as kind=null, intent=null.
     *
     * Takes auxiliary object classes defined in the shadow, in the structural object definition, and those explicitly
     * provided itself into account - by creating {@link CompositeObjectDefinition} in such cases.
     */
    default @Nullable ResourceObjectDefinition findDefinitionForShadow(
            @NotNull ShadowType shadow,
            @NotNull Collection<QName> additionalAuxObjectClassNames) throws SchemaException {
        return ResourceObjectDefinitionResolver.findDefinitionForShadow(this, shadow, additionalAuxObjectClassNames);
    }

    /**
     * Convenience variant of {@link #findDefinitionForShadow(ShadowType, Collection)}.
     */
    default @Nullable ResourceObjectDefinition findDefinitionForShadow(@NotNull ShadowType shadow) throws SchemaException {
        return findDefinitionForShadow(shadow, List.of());
    }
    //endregion

    /**
     * Returns names of all object classes mentioned in the "raw" resource definition.
     */
    default @NotNull Collection<QName> getObjectClassNames() {
        return getObjectClassDefinitions().stream()
                .map(Definition::getTypeName)
                .collect(Collectors.toSet());
    }

    /** Returns an interface to mutate this schema. */
    ResourceSchemaMutator mutator();

    /** Returns a representation of the schema for given layer (immutable). */
    ResourceSchema forLayerImmutable(LayerType layer);

    @Override
    default @NotNull String getNamespace() {
        return MidPointConstants.NS_RI;
    }

    /** TODO description */
    void validate() throws SchemaException;

    /** TODO description */
    default @NotNull Collection<String> getIntentsForKind(ShadowKindType kind) {
        return getObjectTypeDefinitions(kind).stream()
                .map(ResourceObjectTypeDefinition::getIntent)
                .collect(Collectors.toSet());
    }

    Document serializeNativeToXsd() throws SchemaException;

    ResourceSchema clone();

    static @NotNull QName qualifyTypeName(@NotNull String localPart) {
        return new QName(MidPointConstants.NS_RI, localPart);
    }

    /**
     * Returns true if the schema contains no "refined" (type) definitions.
     *
     * BEWARE! Even schemas obtained via {@link ResourceSchemaFactory#getCompleteSchema(ResourceType)} method
     * may seem raw, if there's no `schemaHandling` section. This should be perhaps fixed.
     */
    default boolean isRaw() {
        return getObjectTypeDefinitions().isEmpty();
    }

    @NotNull NativeResourceSchema getNativeSchema();

    interface ResourceSchemaMutator extends PrismSchemaMutator {

        @NotNull NativeObjectClassDefinitionBuilder newComplexTypeDefinitionLikeBuilder(String localName);
    }
}
