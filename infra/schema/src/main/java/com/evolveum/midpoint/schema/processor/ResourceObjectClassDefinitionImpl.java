/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.processor.ResourceSchema.qualifyTypeName;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.DeepCloneOperation;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Default implementation of {@link ResourceObjectClassDefinition}.
 *
 * TODO should we have subclasses for raw/refined variants? Maybe.
 *
 * @author semancik
 */
public class ResourceObjectClassDefinitionImpl
        extends AbstractResourceObjectDefinitionImpl
        implements ResourceObjectClassDefinition {

    @Serial private static final long serialVersionUID = 1L;

    @NotNull private final QName objectClassName;

    /**
     * Native resource object class definition.
     */
    @NotNull private final NativeObjectClassDefinition nativeObjectClassDefinition;

    // Some day, we will use ConfigurationItem for definition bean here.
    private ResourceObjectClassDefinitionImpl(
            @NotNull LayerType layer,
            @NotNull BasicResourceInformation basicResourceInformation,
            @NotNull NativeObjectClassDefinition nativeObjectClassDefinition,
            @NotNull ResourceObjectTypeDefinitionType definitionBean)
            throws SchemaException, ConfigurationException {
        super(layer, basicResourceInformation, definitionBean);
        this.objectClassName = qualifyTypeName(nativeObjectClassDefinition.getName());
        this.nativeObjectClassDefinition = nativeObjectClassDefinition;
    }

    public static ResourceObjectClassDefinitionImpl create(
            @NotNull BasicResourceInformation basicResourceInformation,
            @NotNull NativeObjectClassDefinition nativeObjectClassDefinition,
            @Nullable ResourceObjectTypeDefinitionType definitionBean)
            throws ConfigurationException, SchemaException {
        if (definitionBean != null) {
            checkDefinitionSanity(definitionBean);
        }
        return new ResourceObjectClassDefinitionImpl(
                DEFAULT_LAYER,
                basicResourceInformation,
                nativeObjectClassDefinition,
                Objects.requireNonNullElseGet(definitionBean, ResourceObjectTypeDefinitionType::new));
    }

    /**
     * Object class definition reuses {@link ResourceObjectTypeDefinitionType} but has some restrictions on its content.
     * We check them here.
     */
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
    public @NotNull NativeObjectClassDefinition getNativeObjectClassDefinition() {
        return nativeObjectClassDefinition;
    }

    @Override
    public @Nullable QName getNamingAttributeName() {
        return nativeObjectClassDefinition.getNamingAttributeName();
    }

    @Override
    public @Nullable QName getDisplayNameAttributeName() {
        return nativeObjectClassDefinition.getDisplayNameAttributeName();
    }

    @Override
    public @Nullable QName getDescriptionAttributeName() {
        return nativeObjectClassDefinition.getDescriptionAttributeName();
    }

    @Override
    public String getNativeObjectClassName() {
        return nativeObjectClassDefinition.getNativeObjectClassName();
    }

    @Override
    public boolean isAuxiliary() {
        return nativeObjectClassDefinition.isAuxiliary();
    }

    @Override
    public boolean isEmbedded() {
        return nativeObjectClassDefinition.isEmbedded();
    }

    @Override
    public boolean isDefaultAccountDefinition() {
        return nativeObjectClassDefinition.isDefaultAccountDefinition();
    }

    @Override
    public @Nullable SchemaContextDefinition getSchemaContextDefinition() {
        return null;
    }

    @Override
    public @NotNull ResourceObjectClassDefinitionImpl clone() {
        try {
            return clone(currentLayer, basicResourceInformation);
        } catch (SchemaException | ConfigurationException e) {
            // The data should be already checked for correctness, so this should not happen.
            throw SystemException.unexpected(e, "when cloning");
        }
    }

    @Override
    protected @NotNull ResourceObjectClassDefinitionImpl cloneInLayer(@NotNull LayerType layer) {
        try {
            return clone(layer, basicResourceInformation);
        } catch (SchemaException | ConfigurationException e) {
            // The data should be already checked for correctness, so this should not happen.
            throw SystemException.unexpected(e, "when cloning");
        }
    }

    private @NotNull ResourceObjectClassDefinitionImpl clone(
            @NotNull LayerType newLayer, BasicResourceInformation newInformation) throws SchemaException, ConfigurationException {
        ResourceObjectClassDefinitionImpl clone =
                new ResourceObjectClassDefinitionImpl(
                        newLayer, newInformation, nativeObjectClassDefinition.clone(), definitionBean.clone());
        clone.copyDefinitionDataFrom(newLayer, this);
        return clone;
    }

    public void setInstantiationOrder(Integer order) {
        argCheck(order == null, "Instantiation order is not supported for %s", this);
    }

    @NotNull
    @Override
    public ResourceObjectClassDefinition deepClone(@NotNull DeepCloneOperation operation) {
        throw new UnsupportedOperationException();
//        // We do not need to do deep cloning of item definitions, because they are of simple types.
//        return (ResourceObjectClassDefinition) operation.execute(
//                this,
//                this::clone,
//                clone -> clone.getDefinitions()
//                        .forEach(operation::executePostCloneAction));
    }

    @Override
    public ObjectReferenceType getSecurityPolicyRef() {
        return null;
    }

    @Override
    public boolean isRaw() {
        return !hasRefinements();
    }

    @Override
    public boolean hasRefinements() {
        return !definitionBean.asPrismContainerValue().hasNoItems();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ResourceObjectClassDefinitionImpl that = (ResourceObjectClassDefinitionImpl) o;
        return Objects.equals(objectClassName, that.objectClassName) && Objects.equals(nativeObjectClassDefinition, that.nativeObjectClassDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), objectClassName, nativeObjectClassDefinition);
    }

    @Override
    public @Nullable ResourcePasswordDefinitionType getPasswordDefinition() {
        return null; // Not available for raw classes
    }

    @Override
    public @Nullable ResourceLastLoginTimestampDefinitionType getLastLoginTimestampDefinition() {
        return null; // Not available for raw classes
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "native=" + nativeObjectClassDefinition +
                ", attributeDefinitions: " + attributeDefinitions.size() +
                ", associationDefinitions: " + associationDefinitions.size() +
                ", primaryIdentifiersNames: " + primaryIdentifiersNames.size() +
                ", secondaryIdentifiersNames: " + secondaryIdentifiersNames.size() +
                "}";
    }

    @Override
    public String getDebugDumpClassName() {
        return "ROCD";
    }

    @Override
    protected void addDebugDumpHeaderExtension(StringBuilder sb) {
        if (hasRefinements()) {
            sb.append(", with refinements");
        } else {
            sb.append(", no refinements");
        }
    }

    @Override
    protected void addDebugDumpTrailer(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "description attribute name", getDescriptionAttributeName(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "naming attribute name", getNamingAttributeName(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "default account definition", isDefaultAccountDefinition(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "native object class", getNativeObjectClassName(), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "auxiliary", isAuxiliary(), indent + 1);
    }

    @Override
    public @NotNull String getShortIdentification() {
        return getObjectClassName().getLocalPart();
    }

    @Override
    public @NotNull FocusSpecification getFocusSpecification() {
        return FocusSpecification.empty();
    }

    @Override
    public @NotNull Collection<? extends SynchronizationReactionDefinition> getSynchronizationReactions() {
        return List.of();
    }

}
