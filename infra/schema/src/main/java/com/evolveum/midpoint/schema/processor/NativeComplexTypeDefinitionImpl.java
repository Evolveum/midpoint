/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.*;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.ItemDefinition.ItemDefinitionLikeBuilder;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.schema.DefinitionFeature;
import com.evolveum.midpoint.prism.schema.SerializableComplexTypeDefinition;
import com.evolveum.midpoint.prism.schema.SerializableItemDefinition;
import com.evolveum.midpoint.schema.processor.NativeObjectClassDefinition.NativeObjectClassDefinitionBuilder;

import com.evolveum.midpoint.schema.processor.NativeShadowSimpleAttributeDefinition.NativeShadowAttributeDefinitionBuilder;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;

import com.sun.xml.xsom.XSComplexType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

/**
 * Represents native object class or association class definition.
 *
 * Similarly to {@link NativeShadowAttributeDefinitionImpl}, it is practical to merge these two into one implementation class.
 * The main reason is that both correspond to XSD complex type definition, and we need to instantiate them as early as
 * `xsd:complexType` is encountered in the schema; without reading the annotations up front.
 */
public class NativeComplexTypeDefinitionImpl
        extends AbstractFreezable
        implements
        NativeObjectClassDefinition,
        NativeReferenceTypeDefinition,
        AbstractTypeDefinition,
        SerializableComplexTypeDefinition,
        NativeObjectClassUcfDefinition.Delegable,
        NativeObjectClassUcfDefinition.Mutable.Delegable,
        NativeObjectClassDefinitionBuilder {

    /**
     * Name of this object class (`AccountObjectClass`, `person`, and so on),
     * or association class (`roleMembership`, `personContract`, and so on).
     */
    @NotNull private final String name;

    /** QName version of {@link #name}, with the constant namespace of `ri`. */
    @NotNull private final QName qName;

    //region The following applies to OBJECT classes
    @NotNull private final NativeObjectClassUcfDefinition.Data ucfData = new NativeObjectClassUcfDefinition.Data();

    // Move the following to PrismPresentationDefinition if there will be more of them
    private String displayName;
    private Integer displayOrder;

    @NotNull private final List<NativeShadowAttributeDefinitionImpl<?>> attributeDefinitions = new ArrayList<>();
    //endregion

    //region The following applies to ASSOCIATION classes
    /** See {@link NativeReferenceTypeDefinition#getSubjects()}. */
    @NotNull private final Set<NativeReferenceTypeDefinition.NativeParticipant> subjects = new HashSet<>();

    /** See {@link NativeReferenceTypeDefinition#getObjects()}. */
    @NotNull private final Set<NativeReferenceTypeDefinition.NativeParticipant> objects = new HashSet<>();
    //endregion

    /** False for object classes, true for association classes. */
    private boolean association;

    NativeComplexTypeDefinitionImpl(@NotNull String name) {
        this.name = name;
        this.qName = new QName(name);
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull QName getQName() {
        return qName;
    }

    public boolean isAssociation() {
        return association;
    }

    /** We use the years-old `a:resourceObject` annotation to distinguish between object and association classes. */
    boolean isResourceObjectClass() {
        return !association;
    }

    public void setAssociation() {
        checkMutable();
        this.association = true;
    }

    public void setResourceObject(boolean isResourceObjectClass) {
        checkMutable();
        this.association = !isResourceObjectClass;
    }

    //region Implementation for OBJECT classes
    @Override
    public NativeObjectClassUcfDefinition.Data ucfData() {
        return ucfData;
    }

    @Override
    public @NotNull List<NativeShadowAttributeDefinitionImpl<?>> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    @Override
    public @NotNull List<? extends NativeShadowSimpleAttributeDefinition<?>> getSimpleAttributeDefinitions() {
        return attributeDefinitions.stream()
                .filter(def -> !def.isReference())
                .toList();
    }

    @Override
    public @NotNull List<? extends NativeShadowReferenceAttributeDefinition> getReferenceAttributeDefinitions() {
        return attributeDefinitions.stream()
                .filter(def -> def.isReference())
                .toList();
    }

    @Override
    public void add(DefinitionFragmentBuilder builder) {
        Object objectBuilt = builder.getObjectBuilt();
        if (objectBuilt instanceof NativeShadowAttributeDefinitionImpl<?> itemDef) {
            addItemDefinition(itemDef);
        } else {
            throw new UnsupportedOperationException("Unsupported definition type: " + objectBuilt);
        }
    }

    private void addItemDefinition(@NotNull NativeShadowAttributeDefinitionImpl<?> definition) {
        attributeDefinitions.add(definition);
    }

    @Override
    public NativeShadowSimpleAttributeDefinition<?> findSimpleAttributeDefinition(@NotNull QName attrName) {
        return attributeDefinitions.stream()
                .filter(def -> !def.isReference())
                .filter(def -> QNameUtil.match(def.getItemName(), attrName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public NativeShadowReferenceAttributeDefinition findReferenceAttributeDefinition(@NotNull QName attrName) {
        return attributeDefinitions.stream()
                .filter(def -> def.isReference())
                .filter(def -> QNameUtil.match(def.getItemName(), attrName))
                .findFirst()
                .orElse(null);
    }
    //endregion

    //region Implementation for ASSOCIATION classes
    @Override
    public @NotNull Set<NativeParticipant> getSubjects() {
        return Collections.unmodifiableSet(subjects);
    }

    @Override
    public @NotNull Set<NativeParticipant> getObjects() {
        return Collections.unmodifiableSet(objects);
    }

    @Override
    public void addParticipant(
            @NotNull String objectClassName,
            @NotNull ItemName associationName,
            @NotNull ShadowReferenceParticipantRole role) {
        switch (role) {
            case SUBJECT:
                subjects.add(new NativeReferenceTypeDefinition.NativeParticipant(objectClassName, associationName));
                break;
            case OBJECT:
                objects.add(new NativeReferenceTypeDefinition.NativeParticipant(objectClassName, associationName));
                break;
            default:
                throw new AssertionError(role);
        }
    }
    //endregion

    @Override
    public @NotNull QName getTypeName() {
        return new QName(NS_RI, name);
    }

    @Override
    public boolean isRuntimeSchema() {
        return true;
    }

    @Override
    public boolean isContainerMarker() {
        return true;
    }

    @Override
    public void setDisplayName(String displayName) {
        checkMutable();
        this.displayName = displayName;
    }

    @Override
    public void setDisplayOrder(Integer displayOrder) {
        checkMutable();
        this.displayOrder = displayOrder;
    }

    @Override
    public <T> NativeShadowAttributeDefinitionBuilder<T> newPropertyLikeDefinition(QName elementName, QName typeName) {
        return new NativeShadowAttributeDefinitionImpl<>(ItemName.fromQName(elementName), typeName);
    }

    @Override
    public ItemDefinitionLikeBuilder newContainerLikeDefinition(QName itemName, AbstractTypeDefinition ctd) {
        return new NativeShadowAttributeDefinitionImpl<>(ItemName.fromQName(itemName), ctd.getTypeName());
    }

    @Override
    public ItemDefinitionLikeBuilder newObjectLikeDefinition(QName itemName, AbstractTypeDefinition ctd) {
        throw new UnsupportedOperationException("Object-like definitions are not supported here; name = " + itemName);
    }

    @Override
    public Collection<DefinitionFeature<?, ?, ? super XSComplexType, ?>> getExtraFeaturesToParse() {
        return List.of(
                ResourceDefinitionFeatures.ForClass.DF_RESOURCE_OBJECT,
                ResourceDefinitionFeatures.ForClass.DF_NATIVE_OBJECT_CLASS_NAME,
                ResourceDefinitionFeatures.ForClass.DF_DEFAULT_ACCOUNT_DEFINITION,
                ResourceDefinitionFeatures.ForClass.DF_AUXILIARY,
                ResourceDefinitionFeatures.ForClass.DF_NAMING_ATTRIBUTE_NAME,
                ResourceDefinitionFeatures.ForClass.DF_DISPLAY_NAME_ATTRIBUTE_NAME,
                ResourceDefinitionFeatures.ForClass.DF_DESCRIPTION_ATTRIBUTE_NAME,
                ResourceDefinitionFeatures.ForClass.DF_PRIMARY_IDENTIFIER_NAME,
                ResourceDefinitionFeatures.ForClass.DF_SECONDARY_IDENTIFIER_NAME);
    }

    @Override
    public Collection<? extends DefinitionFeature<?, ?, ?, ?>> getExtraFeaturesToSerialize() {
        return getExtraFeaturesToParse(); // the same list for now (but that's quite logical)
    }

    @Override
    public @Nullable QName getSuperType() {
        return null;
    }

    @Override
    public @Nullable QName getExtensionForType() {
        return null;
    }

    @Override
    public @NotNull Collection<? extends SerializableItemDefinition> getDefinitionsToSerialize() {
        return attributeDefinitions;
    }

    @Override
    public boolean isXsdAnyMarker() {
        return false;
    }

    @Override
    public boolean isObjectMarker() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public boolean isEmphasized() {
        return false;
    }

    @Override
    public Object getObjectBuilt() {
        return this;
    }

    // TODO toString, DebugDump

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public NativeComplexTypeDefinitionImpl clone() {
        NativeComplexTypeDefinitionImpl clone = new NativeComplexTypeDefinitionImpl(name);
        clone.association = association;
        // objects
        clone.ucfData().copyFrom(ucfData);
        attributeDefinitions.forEach(def -> clone.addItemDefinition(def.clone()));
        // associations
        clone.subjects.addAll(subjects);
        clone.objects.addAll(objects);
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(humanReadableName());
        sb.append("{");
        if (association) {
            sb.append(subjects).append(" <-> ").append(objects);
        } else {
            sb.append(attributeDefinitions.size()).append(" item definitions");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilder(humanReadableName() + "\n", indent);
        if (!association) {
            DebugUtil.debugDumpWithLabelLn(sb, "UCF data", ucfData, indent + 1);
            DebugUtil.debugDumpLabelLn(sb, "Items", indent + 1);
            sb.append(DebugUtil.debugDump(attributeDefinitions, indent + 1));
        } else {
            DebugUtil.debugDumpLabelLn(sb, "Subjects", indent + 1);
            sb.append(DebugUtil.debugDump(subjects, indent + 1));
            sb.append("\n");
            DebugUtil.debugDumpLabelLn(sb, "Objects", indent + 1);
            sb.append(DebugUtil.debugDump(objects, indent + 1));
        }
        return sb.toString();
    }

    @NotNull
    private String humanReadableName() {
        return "Native " + (association ? "association" : "object") + " class '" + name + "'";
    }

    @Override
    protected void performFreeze() {
        super.performFreeze();
        ucfData.freeze();
        attributeDefinitions.forEach(NativeShadowAttributeDefinitionImpl::freeze);
    }
}
