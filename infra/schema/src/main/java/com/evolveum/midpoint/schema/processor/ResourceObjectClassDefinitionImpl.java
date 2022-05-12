/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.Objects;
import java.util.function.Consumer;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Default implementation of {@link ResourceObjectClassDefinition}.
 *
 * @author semancik
 */
public class ResourceObjectClassDefinitionImpl
        extends AbstractResourceObjectDefinitionImpl
        implements MutableResourceObjectClassDefinition {

    private static final long serialVersionUID = 1L;

    @NotNull private final QName objectClassName;

    /** See {@link ResourceObjectDefinition#getDescriptionAttribute()} */
    private QName descriptionAttributeName;

    /** See {@link ResourceObjectDefinition#getDisplayNameAttribute()} */
    private QName displayNameAttributeName;

    /** See {@link ResourceObjectDefinition#getNamingAttribute()} ()} */
    private QName namingAttributeName;

    /** See {@link ResourceObjectClassDefinition#isDefaultAccountDefinition()} */
    private boolean defaultAccountDefinition;

    private String nativeObjectClass;

    private boolean auxiliary;

    public ResourceObjectClassDefinitionImpl(
            @NotNull QName objectClassName) {
        this(DEFAULT_LAYER, objectClassName);
    }

    /** Used for cloning and layering. */
    private ResourceObjectClassDefinitionImpl(
            @NotNull LayerType layer,
            @NotNull QName objectClassName) {
        super(layer);
        this.objectClassName = objectClassName;
    }

    @Override
    public @NotNull QName getObjectClassName() {
        return objectClassName;
    }

    @Override
    public String getDisplayName() {
        // Currently not supported for raw object classes
        return null;
    }

    @Override
    public void delete(QName itemName) {
        attributeDefinitions.removeIf(
                def -> QNameUtil.match(def.getItemName(), itemName));
    }

    @Override
    public MutablePrismPropertyDefinition<?> createPropertyDefinition(QName name, QName typeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MutablePrismPropertyDefinition<?> createPropertyDefinition(String name, QName typeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPrimaryIdentifierName(QName name) {
        checkMutable();
        primaryIdentifiersNames.add(name);
    }

    @Override
    public void addSecondaryIdentifierName(QName name) {
        checkMutable();
        secondaryIdentifiersNames.add(name);
    }

    @Override
    public @Nullable QName getDescriptionAttributeName() {
        return descriptionAttributeName;
    }

    @Override
    public void setDescriptionAttributeName(QName name) {
        checkMutable();
        this.descriptionAttributeName = name;
    }

    @Override
    public @Nullable QName getNamingAttributeName() {
        return namingAttributeName;
    }

    @Override
    public void setNamingAttributeName(QName name) {
        checkMutable();
        this.namingAttributeName = name;
    }

    @Override
    public @Nullable QName getDisplayNameAttributeName() {
        return displayNameAttributeName;
    }

    @Override
    public void setDisplayNameAttributeName(QName name) {
        checkMutable();
        this.displayNameAttributeName = name;
    }

    @Override
    public String getNativeObjectClass() {
        return nativeObjectClass;
    }

    @Override
    public void setNativeObjectClass(String nativeObjectClass) {
        checkMutable();
        this.nativeObjectClass = nativeObjectClass;
    }

    @Override
    public boolean isAuxiliary() {
        return auxiliary;
    }

    @Override
    public void setAuxiliary(boolean auxiliary) {
        checkMutable();
        this.auxiliary = auxiliary;
    }

    @Override
    public boolean isDefaultAccountDefinition() {
        return defaultAccountDefinition;
    }

    @Override
    public void setDefaultAccountDefinition(boolean value) {
        checkMutable();
        this.defaultAccountDefinition = value;
    }

    @VisibleForTesting
    @Override
    public <T> ResourceAttributeDefinition<T> createAttributeDefinition(
            @NotNull QName name,
            @NotNull QName typeName,
            @NotNull Consumer<MutableRawResourceAttributeDefinition<?>> consumer) {
        RawResourceAttributeDefinitionImpl<T> rawDefinition = new RawResourceAttributeDefinitionImpl<>(name, typeName);
        consumer.accept(rawDefinition);
        //noinspection unchecked
        return (ResourceAttributeDefinition<T>) addInternal(rawDefinition);
    }

    @Override
    public ResourceAttributeContainer instantiate(@NotNull ItemName elementName) {
        return instantiate(elementName, this);
    }

    // TODO is this needed to be exposed publicly?
    public static ResourceAttributeContainer instantiate(
            @NotNull QName elementName,
            @NotNull ResourceObjectClassDefinition objectClassDefinition) {
        return new ResourceAttributeContainerImpl(
                elementName,
                objectClassDefinition.toResourceAttributeContainerDefinition(elementName));
    }

    @NotNull
    @Override
    public ResourceObjectClassDefinitionImpl clone() {
        return cloneInLayer(currentLayer);
    }

    @Override
    protected ResourceObjectClassDefinitionImpl cloneInLayer(@NotNull LayerType layer) {
        ResourceObjectClassDefinitionImpl clone = new ResourceObjectClassDefinitionImpl(layer, objectClassName);
        clone.copyDefinitionDataFrom(layer, this);
        return clone;
    }

    @Override
    public void setExtensionForType(QName type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAbstract(boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSuperType(QName superType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setObjectMarker(boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContainerMarker(boolean value) {
        argCheck(value, "Container marker cannot be turned off for %s", this);
    }

    @Override
    public void setReferenceMarker(boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDefaultNamespace(String namespace) {
        argCheck(namespace == null, "Default namespace cannot be set for %s", this);
    }

    @Override
    public void setIgnoredNamespaces(@NotNull List<String> ignoredNamespaces) {
        argCheck(ignoredNamespaces.isEmpty(), "Ignored namespaces cannot be set for %s", this);
    }

    @Override
    public void setXsdAnyMarker(boolean value) {
        argCheck(value, "Cannot set xsd:any flag to 'false' for %s", this);
    }

    @Override
    public void setListMarker(boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCompileTimeClass(Class<?> compileTimeClass) {
        argCheck(compileTimeClass == null, "Compile-time class cannot be set for %s", this);
    }

    @Override
    public void addSubstitution(ItemDefinition<?> itemDef, ItemDefinition<?> maybeSubst) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProcessing(ItemProcessing processing) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDeprecated(boolean deprecated) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExperimental(boolean experimental) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEmphasized(boolean emphasized) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDisplayName(String displayName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDisplayOrder(Integer displayOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHelp(String help) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRuntimeSchema(boolean value) {
        argCheck(value, "Cannot set runtime schema flag to 'false' for %s", this);
    }

    @Override
    public void setTypeName(QName typeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDocumentation(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addSchemaMigration(SchemaMigration schemaMigration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDiagram(ItemDiagramSpecification diagram) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setInstantiationOrder(Integer order) {
        argCheck(order == null, "Instantiation order is not supported for %s", this);
    }

    @NotNull
    @Override
    public ResourceObjectClassDefinition deepClone(@NotNull DeepCloneOperation operation) {
        // We do not need to do deep cloning of item definitions, because they are of simple types.
        return (ResourceObjectClassDefinition) operation.execute(
                this,
                this::clone,
                clone -> clone.getDefinitions()
                        .forEach(operation::executePostCloneAction));
    }

    @Override
    public ObjectReferenceType getSecurityPolicyRef() {
        return null;
    }

    @Override
    public boolean hasSubstitutions() {
        // TODO
        return false;
    }

    @Override
    public Optional<ItemDefinition<?>> substitution(QName name) {
        // TODO
        return Optional.empty();
    }

    @SuppressWarnings("WeakerAccess") // open for subclassing
    protected void copyDefinitionDataFrom(@NotNull LayerType layer, ResourceObjectClassDefinition source) {
        super.copyDefinitionDataFrom(layer, source);
        descriptionAttributeName = source.getDescriptionAttributeName();
        displayNameAttributeName = source.getDisplayNameAttributeName();
        namingAttributeName = source.getNamingAttributeName();
        defaultAccountDefinition = source.isDefaultAccountDefinition();
        nativeObjectClass = source.getNativeObjectClass();
        auxiliary = source.isAuxiliary();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ResourceObjectClassDefinitionImpl that = (ResourceObjectClassDefinitionImpl) o;
        return defaultAccountDefinition == that.defaultAccountDefinition
                && auxiliary == that.auxiliary
                && objectClassName.equals(that.objectClassName)
                && Objects.equals(descriptionAttributeName, that.descriptionAttributeName)
                && Objects.equals(displayNameAttributeName, that.displayNameAttributeName)
                && Objects.equals(namingAttributeName, that.namingAttributeName)
                && Objects.equals(nativeObjectClass, that.nativeObjectClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), objectClassName, descriptionAttributeName, displayNameAttributeName,
                namingAttributeName, defaultAccountDefinition, nativeObjectClass, auxiliary);
    }

    @Override
    public MutableResourceObjectClassDefinition toMutable() {
        checkMutable();
        return this;
    }

    @Override
    public @Nullable ResourcePasswordDefinitionType getPasswordDefinition() {
        return null; // Not available for raw classes
    }

    @Override
    public @NotNull ResourceObjectClassDefinition getObjectClassDefinition() {
        return this;
    }

    @Override
    public <T extends CapabilityType> T getEnabledCapability(@NotNull Class<T> capabilityClass, ResourceType resource) {
        // we have no refinements here, so we look only at the level of resource
        return ResourceTypeUtil.getEnabledCapability(resource, capabilityClass);
    }

    @Override
    public String getHumanReadableName() {
        if (getDisplayName() != null) {
            return getDisplayName();
        } else {
            return getObjectClassName().getLocalPart();
        }
    }

    @Override
    public String toString() {
        return "ResourceObjectClassDefinitionImpl{" +
                "objectClassName=" + objectClassName.getLocalPart() +
                ", attributeDefinitions: " + attributeDefinitions.size() +
                ", primaryIdentifiersNames: " + primaryIdentifiersNames.size() +
                ", secondaryIdentifiersNames: " + secondaryIdentifiersNames.size() +
                ", defaultInAKind=" + defaultAccountDefinition +
                ", nativeObjectClass='" + nativeObjectClass + '\'' +
                ", auxiliary=" + auxiliary +
                "}";
    }

    @Override
    public @NotNull Collection<ResourceAssociationDefinition> getAssociationDefinitions() {
        return List.of();
    }

    @Override
    public String getDebugDumpClassName() {
        return "ROCD";
    }

    @Override
    public String getDescription() {
        return null; // no information in raw object class
    }

    @Override
    public String getDocumentation() {
        return null; // no information in raw object class
    }

    @Override
    public String getResourceOid() {
        return null; // TODO remove this
    }

    @Override
    public ResourceObjectMultiplicityType getObjectMultiplicity() {
        return null; // no information in raw object class
    }

    @Override
    public ProjectionPolicyType getProjectionPolicy() {
        return null; // no information in raw object class
    }

    @Override
    public @NotNull Collection<ResourceObjectDefinition> getAuxiliaryDefinitions() {
        return List.of(); // no information in raw object class
    }

    @Override
    public boolean hasAuxiliaryObjectClass(QName expectedObjectClassName) {
        return false; // no information in raw object class
    }

    @Override
    public ResourceBidirectionalMappingAndDefinitionType getAuxiliaryObjectClassMappings() {
        return null; // no information in raw object class
    }

    @Override
    public @NotNull Collection<ResourceObjectPattern> getProtectedObjectPatterns() {
        return List.of(); // no information in raw object class
    }

    @Override
    public ResourceActivationDefinitionType getActivationSchemaHandling() {
        return null; // no information in raw object class
    }

    @Override
    public @NotNull ResourceObjectTypeDelineation getDelineation() {
        return ResourceObjectTypeDelineation.none(); // no specific delineation in raw object class
    }

    @Override
    public ResourceObjectReferenceType getBaseContext() {
        return null; // no information in raw object class
    }

    @Override
    public SearchHierarchyScope getSearchHierarchyScope() {
        return null; // no information in raw object class
    }

    @Override
    public @NotNull ResourceObjectVolatilityType getVolatility() {
        return ResourceObjectVolatilityType.NONE; // no information in raw object class
    }

    @Override
    public @Nullable DefaultInboundMappingEvaluationPhasesType getDefaultInboundMappingEvaluationPhases() {
        return null; // no information in raw object class
    }
}
