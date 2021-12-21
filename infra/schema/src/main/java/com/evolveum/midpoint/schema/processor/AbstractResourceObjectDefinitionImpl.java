/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.impl.ComplexTypeDefinitionImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Common implementation for both {@link ResourceObjectClassDefinition} and {@link ResourceObjectTypeDefinition}.
 *
 * Note about not inheriting from {@link ComplexTypeDefinitionImpl}:
 *
 * As we do not inherit from that class, we have to provide our own implementation of various methods like
 * {@link #getExtensionForType()}, {@link #isContainerMarker()}, and so on. This is basically
 * no problem, as this information is not available from a resource connector, so we are OK with the default values.
 * Should this change, we would need to reconsider this design. The current implementation is more straightforward,
 * less entangled with a hierarchy of ancestor implementations.
 */
public abstract class AbstractResourceObjectDefinitionImpl
        extends AbstractFreezable
        implements ResourceObjectDefinition {

    /**
     * Default value for {@link #currentLayer}.
     */
    static final LayerType DEFAULT_LAYER = LayerType.MODEL;

    /**
     * At what layer do we have the attribute definitions.
     */
    @NotNull final LayerType currentLayer;

    /**
     * Definition of attributes.
     *
     * It seems that elements are strictly of {@link ResourceAttributeDefinitionImpl} class, but this is currently
     * not enforceable in the compile time - see e.g. {@link #copyDefinitionDataFrom(LayerType, ResourceObjectDefinition)}
     * or {@link #addInternal(ItemDefinition)}. TODO reconsider if it's ok this way.
     *
     * Frozen after creation.
     */
    @NotNull protected final DeeplyFreezableList<ResourceAttributeDefinition<?>> attributeDefinitions =
            new DeeplyFreezableList<>();

    /**
     * Names of primary identifiers. They are the same for both raw and refined definitions.
     * (Currently we do not support tweaking of this information.)
     *
     * Frozen after creation.
     */
    @NotNull protected final FreezableList<QName> primaryIdentifiersNames = new FreezableList<>();

    /**
     * Names of secondary identifiers. The refinement using `schemaHandling` may add or remove some identifiers
     * from the raw list.
     *
     * Frozen after creation.
     *
     * @see ResourceAttributeDefinitionType#isSecondaryIdentifier()
     */
    @NotNull protected final FreezableList<QName> secondaryIdentifiersNames = new FreezableList<>();

    /**
     * Object definition for compliant prism objects.
     * The "any" parts are replaced with appropriate schema (e.g. resource schema).
     *
     * Immutable.
     */
    private PrismObjectDefinition<ShadowType> prismObjectDefinition;

    AbstractResourceObjectDefinitionImpl(@NotNull LayerType currentLayer) {
        this.currentLayer = currentLayer;
    }

    @NotNull
    @Override
    public List<? extends ResourceAttributeDefinition<?>> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    @NotNull
    @Override
    public Collection<ResourceAttributeDefinition<?>> getPrimaryIdentifiers() {
        return primaryIdentifiersNames.stream()
                .map(this::findAttributeDefinitionStrictlyRequired)
                .collect(Collectors.toUnmodifiableList());
    }

    @NotNull
    @Override
    public Collection<ResourceAttributeDefinition<?>> getSecondaryIdentifiers() {
        return secondaryIdentifiersNames.stream()
                .map(this::findAttributeDefinitionStrictlyRequired)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Names of primary identifiers. The list is modifiable until the whole definition is frozen.
     */
    public @NotNull List<QName> getPrimaryIdentifiersNames() {
        return primaryIdentifiersNames;
    }

    /**
     * Names of secondary identifiers. The list is modifiable until the whole definition is frozen.
     */
    public @NotNull List<QName> getSecondaryIdentifiersNames() {
        return secondaryIdentifiersNames;
    }

    @Override
    public PrismObjectDefinition<ShadowType> getPrismObjectDefinition() {
        if (prismObjectDefinition == null) {
            PrismObjectDefinition<ShadowType> definition = computePrismObjectDefinition();
            definition.freeze();
            this.prismObjectDefinition = definition;
        }
        return prismObjectDefinition;
    }

    private void invalidatePrismObjectDefinition() {
        prismObjectDefinition = null;
    }

    @NotNull
    public PrismObjectDefinition<ShadowType> computePrismObjectDefinition() {
        return ObjectFactory.constructObjectDefinition(
                toResourceAttributeContainerDefinition());
    }

    @Override
    public PrismObject<ShadowType> createBlankShadow(String resourceOid, String tag) {
        PrismObject<ShadowType> accountShadow;
        try {
            accountShadow = getPrismContext().createObject(ShadowType.class);
        } catch (SchemaException e) {
            // This should not happen
            throw new SystemException("Internal error instantiating account shadow: " + e.getMessage(), e);
        }
        ShadowType accountShadowType = accountShadow.asObjectable();

        accountShadowType
                .tag(tag)
                .objectClass(getObjectClassDefinition().getTypeName())
                .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE);

        // Setup definition
        PrismObjectDefinition<ShadowType> newDefinition = accountShadow.getDefinition().cloneWithReplacedDefinition(
                ShadowType.F_ATTRIBUTES, toResourceAttributeContainerDefinition());
        accountShadow.setDefinition(newDefinition);

        return accountShadow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractResourceObjectDefinitionImpl that = (AbstractResourceObjectDefinitionImpl) o;
        return attributeDefinitions.equals(that.attributeDefinitions)
                && primaryIdentifiersNames.equals(that.primaryIdentifiersNames)
                && secondaryIdentifiersNames.equals(that.secondaryIdentifiersNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeDefinitions, primaryIdentifiersNames, secondaryIdentifiersNames);
    }

    public abstract @NotNull AbstractResourceObjectDefinitionImpl clone();

    @SuppressWarnings("WeakerAccess") // open for subclassing
    protected void copyDefinitionDataFrom(
            @NotNull LayerType layer,
            @NotNull ResourceObjectDefinition source) {
        source.getAttributeDefinitions().forEach(
                def -> attributeDefinitions.add(def.forLayer(layer)));
        primaryIdentifiersNames.addAll(source.getPrimaryIdentifiersNames());
        secondaryIdentifiersNames.addAll(source.getSecondaryIdentifiersNames());
        // prism object definition need not be copied
    }

    @Override
    public PrismContext getPrismContext() {
        return PrismContext.get();
    }

    @Override
    public void revive(PrismContext prismContext) {
    }

    @Override
    public @Nullable Class<?> getCompileTimeClass() {
        return null;
    }

    @Override
    public @Nullable QName getSuperType() {
        return null;
    }

    @Override
    public @NotNull Collection<TypeDefinition> getStaticSubTypes() {
        return List.of();
    }

    @Override
    public Integer getInstantiationOrder() {
        return null;
    }

    @Override
    public boolean canRepresent(QName typeName) {
        return QNameUtil.match(typeName, getObjectClassName());
    }

    @Override
    protected void performFreeze() {
        attributeDefinitions.freeze();
        primaryIdentifiersNames.freeze();
        secondaryIdentifiersNames.freeze();
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        visitor.visit(this);
        getAttributeDefinitions().forEach(def -> def.accept(visitor));
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        if (visitation.alreadyVisited(this)) {
            return false;
        } else {
            visitor.visit(this);
            getAttributeDefinitions().forEach(def -> def.accept(visitor, visitation));
            return true;
        }
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, null, this);
    }

    public static String debugDump(int indent, LayerType layer, AbstractResourceObjectDefinitionImpl _this) {
        StringBuilder sb = new StringBuilder();
        sb.append(INDENT_STRING.repeat(Math.max(0, indent)));
        sb.append(_this.getDebugDumpClassName()).append(_this.getMutabilityFlag()).append("(");
        sb.append(SchemaDebugUtil.prettyPrint(_this.getTypeName()));
        _this.addDebugDumpHeaderExtension(sb);
        if (layer != null) {
            sb.append(",layer=").append(layer);
        }
        sb.append(")");
        for (ResourceAttributeDefinition<?> rAttrDef : _this.getAttributeDefinitions()) {
            sb.append("\n");
            sb.append(rAttrDef.debugDump(indent + 1, layer));
        }
        return sb.toString();
    }

    protected void addDebugDumpHeaderExtension(StringBuilder sb) {
    }

    @Override
    public @NotNull List<? extends ItemDefinition<?>> getDefinitions() {
        return getAttributeDefinitions();
    }

    @Override
    public @Nullable QName getExtensionForType() {
        return null;
    }

    @Override
    public boolean isReferenceMarker() {
        return false;
    }

    @Override
    public boolean isContainerMarker() {
        return true;
    }

    @Override
    public boolean isObjectMarker() {
        return false;
    }

    @Override
    public boolean isXsdAnyMarker() {
        return true;
    }

    @Override
    public boolean isListMarker() {
        return false;
    }

    @Override
    public @Nullable String getDefaultNamespace() {
        return null;
    }

    @Override
    public @NotNull List<String> getIgnoredNamespaces() {
        return List.of();
    }

    @Override
    public void merge(ComplexTypeDefinition otherComplexTypeDef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return attributeDefinitions.isEmpty();
    }

    @Override
    public @NotNull QName getTypeName() {
        return getObjectClassName();
    }

    @Override
    public boolean isRuntimeSchema() {
        return true;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public boolean isExperimental() {
        return false;
    }

    @Override
    public String getPlannedRemoval() {
        return null;
    }

    @Override
    public boolean isElaborate() {
        return false;
    }

    @Override
    public String getDeprecatedSince() {
        return null;
    }

    @Override
    public boolean isEmphasized() {
        return false;
    }

    @Override
    public Integer getDisplayOrder() {
        return null;
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public String getDocumentation() {
        return null;
    }

    @Override
    public String getDocumentationPreview() {
        return null;
    }

    @Override
    public Class<?> getTypeClassIfKnown() {
        return null;
    }

    @Override
    public Class<?> getTypeClass() {
        return null;
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return null;
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Map<QName, Object> getAnnotations() {
        return null;
    }

    @Override
    public List<SchemaMigration> getSchemaMigrations() {
        return null;
    }

    @Override
    public List<ItemDiagramSpecification> getDiagrams() {
        return null;
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        if (path.size() != 1) {
            return null;
        }
        ItemName first = path.firstToNameOrNull();
        if (first == null) {
            return null;
        }
        return findLocalItemDefinition(first.asSingleName(), clazz, false);
    }

    public ResourceObjectDefinition forLayer(@NotNull LayerType layer) {
        if (currentLayer == layer) {
            return this;
        } else {
            return cloneInLayer(layer);
        }
    }

    protected abstract AbstractResourceObjectDefinitionImpl cloneInLayer(@NotNull LayerType layer);

    @Override
    public @NotNull LayerType getCurrentLayer() {
        return currentLayer;
    }

    @Override
    public void replaceDefinition(@NotNull QName itemName, @Nullable ItemDefinition<?> newDefinition) {
        checkMutable();
        invalidatePrismObjectDefinition();
        attributeDefinitions.removeIf(def -> def.getItemName().equals(itemName));
        if (newDefinition != null) {
            add(newDefinition);
        }
    }

    public void add(ItemDefinition<?> definition) {
        addInternal(definition);
    }

    public @NotNull ResourceAttributeDefinition<?> addInternal(@NotNull ItemDefinition<?> definition) {
        ResourceAttributeDefinition<?> definitionToAdd;
        if (definition instanceof ResourceAttributeDefinition<?>) {
            // Can occur during definition replacement.
            definitionToAdd = (ResourceAttributeDefinition<?>) definition;
        } else if (definition instanceof RawResourceAttributeDefinition<?>) {
            // This is the case during parsing. We get the really raw (and mutable) definition.
            // The following call will convert it into usable form, including freezing.
            definitionToAdd = ResourceAttributeDefinitionImpl.create((RawResourceAttributeDefinition<?>) definition);
        } else {
            throw new IllegalArgumentException(
                    "Only ResourceAttributeDefinitions should be put into a ResourceObjectClassDefinition. "
                            + "Item definition = " + definition + " (" + definition.getClass() + "), "
                            + "ResourceObjectClassDefinition = " + this);
        }

        attributeDefinitions.add(definitionToAdd);
        return definitionToAdd;
    }

    @Override
    public ItemProcessing getProcessing() {
        return null; // No information at the level of object class
    }

    @Override
    public Collection<QName> getConfiguredAuxiliaryObjectClassNames() {
        // TODO keep the names, not the resolved definitions
        return getAuxiliaryDefinitions().stream()
                .map(Definition::getTypeName)
                .collect(Collectors.toSet());
    }

    @Override
    public void trimTo(@NotNull Collection<ItemPath> paths) {
        if (isImmutable()) {
            return; // This would fail anyway
        }
        for (Iterator<ResourceAttributeDefinition<?>> iterator = attributeDefinitions.iterator(); iterator.hasNext(); ) {
            ItemDefinition<?> itemDef = iterator.next();
            ItemPath itemPath = itemDef.getItemName();
            if (!ItemPathCollectionsUtil.containsSuperpathOrEquivalent(paths, itemPath)) {
                iterator.remove();
            }
        }
        // TODO what about QName references like primary or secondary identifier names,
        //  or name, display name, or description attribute names?
    }
}
