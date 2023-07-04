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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
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
import static com.evolveum.midpoint.util.MiscUtil.configCheck;

/**
 * Default implementation of {@link ResourceObjectClassDefinition}.
 *
 * TODO should we have subclasses for raw/refined variants? Maybe.
 *
 * @author semancik
 */
public class ResourceObjectClassDefinitionImpl
        extends AbstractResourceObjectDefinitionImpl
        implements MutableResourceObjectClassDefinition {

    private static final long serialVersionUID = 1L;

    @NotNull private final QName objectClassName;

    /**
     * Definition of the raw resource object class definition.
     * Null if the object itself is raw.
     * Immutable.
     */
    @Nullable private final ResourceObjectClassDefinition rawObjectClassDefinition;

    /** See {@link ResourceObjectDefinition#getDescriptionAttribute()}. Set only for raw schema! */
    private QName descriptionAttributeName;

    /** See {@link ResourceObjectDefinition#getNamingAttribute()}. Set only for raw schema! */
    private QName namingAttributeName;

    /** See {@link ResourceObjectClassDefinition#isDefaultAccountDefinition()}. Set only for raw schema! */
    private boolean defaultAccountDefinition;

    /** Set only for raw schema! */
    private String nativeObjectClass;

    /** Set only for raw schema! */
    private boolean auxiliary;

    private ResourceObjectClassDefinitionImpl(
            @NotNull LayerType layer,
            @NotNull QName objectClassName,
            @NotNull ResourceObjectTypeDefinitionType definitionBean,
            @Nullable ResourceObjectClassDefinition rawObjectClassDefinition) {
        super(layer, definitionBean);
        this.objectClassName = objectClassName;
        this.rawObjectClassDefinition = rawObjectClassDefinition;
        if (rawObjectClassDefinition == null) {
            // This definition itself is a raw one.
            // Looks like a hack - TODO resolve this
            setDelineation(
                    ResourceObjectTypeDelineation.of(objectClassName));
        }
    }

    public static ResourceObjectClassDefinitionImpl raw(@NotNull QName objectClassName) {
        return new ResourceObjectClassDefinitionImpl(
                DEFAULT_LAYER,
                objectClassName,
                new ResourceObjectTypeDefinitionType(),
                null);
    }

    public static ResourceObjectClassDefinitionImpl refined(
            @NotNull ResourceObjectClassDefinition raw, @Nullable ResourceObjectTypeDefinitionType definitionBean)
            throws ConfigurationException {
        if (definitionBean != null) {
            checkDefinitionSanity(definitionBean);
        }
        return new ResourceObjectClassDefinitionImpl(
                DEFAULT_LAYER,
                raw.getObjectClassName(),
                Objects.requireNonNullElseGet(definitionBean, ResourceObjectTypeDefinitionType::new),
                raw);
    }

    private static void checkDefinitionSanity(@NotNull ResourceObjectTypeDefinitionType bean) throws ConfigurationException {
        QName name = getObjectClassName(bean);
        configCheck(name != null,
                "Object class name must be specified in object class refinement: %s", bean);
        configCheck(bean.getKind() == null,
                "Kind must not be specified for object class refinement: %s", name);
        configCheck(bean.getIntent() == null,
                "Intent must not be specified for object class refinement: %s", name);
        configCheck(bean.getSuper() == null,
                "Super-type must not be specified for object class refinement: %s", name);
        configCheck(bean.isDefault() == null,
                "'default' flag must not be specified for object class refinement: %s", name);
        configCheck(bean.isDefaultForKind() == null,
                "'defaultForKind' flag must not be specified for object class refinement: %s", name);
        configCheck(bean.isDefaultForObjectClass() == null,
                "'defaultForObjectClass' flag must not be specified for object class refinement: %s", name);
        configCheck(bean.isAbstract() == null,
                "'abstract' flag must not be specified for object class refinement: %s", name);
    }

    private static QName getObjectClassName(@NotNull ResourceObjectTypeDefinitionType bean) {
        QName main = bean.getObjectClass();
        if (main != null) {
            return main;
        }
        // We accept the value from delineation, but ... it should not be used that way.
        ResourceObjectTypeDelineationType delineation = bean.getDelineation();
        return delineation != null ? delineation.getObjectClass() : null;
    }

    @Override
    public @NotNull QName getObjectClassName() {
        return objectClassName;
    }

    @Override
    public @NotNull ResourceObjectClassDefinition getObjectClassDefinition() {
        return this;
    }

    @Override
    public @NotNull ResourceObjectClassDefinition getRawObjectClassDefinition() {
        return Objects.requireNonNullElse(rawObjectClassDefinition, this);
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
        ResourceObjectClassDefinition rawDef = rawObjectClassDefinition;
        return rawDef != null ? rawDef.getDescriptionAttributeName() : descriptionAttributeName;
    }

    @Override
    public void setDescriptionAttributeName(QName name) {
        checkMutable();
        this.descriptionAttributeName = name;
    }

    @Override
    public @Nullable QName getNamingAttributeName() {
        ResourceObjectClassDefinition rawDef = rawObjectClassDefinition;
        return rawDef != null ? rawDef.getNamingAttributeName() : namingAttributeName;
    }

    @Override
    public void setNamingAttributeName(QName name) {
        checkMutable();
        this.namingAttributeName = name;
    }

    @Override
    public @Nullable QName getDisplayNameAttributeName() {
        ResourceObjectClassDefinition rawDef = rawObjectClassDefinition;
        return rawDef != null ? rawDef.getDisplayNameAttributeName() : displayNameAttributeName;
    }

    @Override
    public String getNativeObjectClass() {
        ResourceObjectClassDefinition rawDef = rawObjectClassDefinition;
        return rawDef != null ? rawDef.getNativeObjectClass() : nativeObjectClass;
    }

    @Override
    public void setNativeObjectClass(String nativeObjectClass) {
        checkMutable();
        this.nativeObjectClass = nativeObjectClass;
    }

    @Override
    public boolean isAuxiliary() {
        ResourceObjectClassDefinition rawDef = rawObjectClassDefinition;
        return rawDef != null ? rawDef.isAuxiliary() : auxiliary;
    }

    @Override
    public void setAuxiliary(boolean auxiliary) {
        checkMutable();
        this.auxiliary = auxiliary;
    }

    @Override
    public boolean isDefaultAccountDefinition() {
        ResourceObjectClassDefinition rawDef = rawObjectClassDefinition;
        return rawDef != null ? rawDef.isDefaultAccountDefinition() : defaultAccountDefinition;
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

    @Override
    public void accept(Visitor<Definition> visitor) {
        super.accept(visitor);
        if (rawObjectClassDefinition != null) {
            rawObjectClassDefinition.accept(visitor);
        }
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        if (!super.accept(visitor, visitation)) {
            return false;
        } else {
            if (rawObjectClassDefinition != null) {
                rawObjectClassDefinition.accept(visitor, visitation);
            }
            return true;
        }
    }

    @Override
    public void trimTo(@NotNull Collection<ItemPath> paths) {
        if (isImmutable()) {
            return; // This would fail anyway
        }
        super.trimTo(paths);
        if (rawObjectClassDefinition != null) {
            // It is most probably immutable, but let us give it a chance.
            rawObjectClassDefinition.trimTo(paths);
        }
    }

    @NotNull
    @Override
    public ResourceObjectClassDefinitionImpl clone() {
        return cloneInLayer(currentLayer);
    }

    @Override
    protected ResourceObjectClassDefinitionImpl cloneInLayer(@NotNull LayerType layer) {
        ResourceObjectClassDefinitionImpl clone =
                new ResourceObjectClassDefinitionImpl(layer, objectClassName, definitionBean, rawObjectClassDefinition);
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
    public void setRemoved(boolean removed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRemovedSince(String removedSince) {
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
    public boolean isRaw() {
        return rawObjectClassDefinition == null;
    }

    @Override
    public boolean hasRefinements() {
        return !isRaw() && !definitionBean.asPrismContainerValue().hasNoItems();
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
        ResourceObjectClassDefinition rawDef = rawObjectClassDefinition;
        if (rawDef != null) {
            return "ResourceObjectClassDefinitionImpl (refined) {" +
                    "raw=" + rawDef +
                    ", attributeDefinitions: " + attributeDefinitions.size() +
                    ", primaryIdentifiersNames: " + primaryIdentifiersNames.size() +
                    ", secondaryIdentifiersNames: " + secondaryIdentifiersNames.size() +
                    "}";
        } else {
            return "ResourceObjectClassDefinitionImpl (raw) {" +
                    "objectClassName=" + objectClassName.getLocalPart() +
                    ", attributeDefinitions: " + attributeDefinitions.size() +
                    ", primaryIdentifiersNames: " + primaryIdentifiersNames.size() +
                    ", secondaryIdentifiersNames: " + secondaryIdentifiersNames.size() +
                    ", defaultAccountDefinition=" + defaultAccountDefinition +
                    ", nativeObjectClass='" + nativeObjectClass + '\'' +
                    ", auxiliary=" + auxiliary +
                    "}";
        }
    }

    @Override
    public String getDebugDumpClassName() {
        return "ROCD";
    }

    @Override
    protected void addDebugDumpHeaderExtension(StringBuilder sb) {
        if (isRaw()) {
            sb.append(",raw");
        } else {
            if (hasRefinements()) {
                sb.append(",refined (with refinements)");
            } else {
                sb.append(",refined (no refinements)");
            }
        }
    }

    @Override
    protected void addDebugDumpTrailer(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "description attribute name", getDescriptionAttributeName(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "naming attribute name", getNamingAttributeName(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "default account definition", isDefaultAccountDefinition(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "native object class", getNativeObjectClass(), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "auxiliary", isAuxiliary(), indent + 1);
    }

    @Override
    public String getResourceOid() {
        return null; // TODO remove this
    }

    @Override
    public @Nullable ResourceObjectTypeIdentification getTypeIdentification() {
        return null;
    }

    @Override
    public @Nullable ResourceObjectTypeDefinition getTypeDefinition() {
        return null;
    }

    @Override
    public boolean isDefaultFor(@NotNull ShadowKindType kind) {
        // Normally, object class definitions know nothing about kind/intent. This is the only exception.
        return kind == ShadowKindType.ACCOUNT && isDefaultAccountDefinition();
    }
}
