/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.DeepCloneOperation;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.normalization.Normalizer;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.impl.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.Nullable;

/**
 * A structure holding "raw" definition of a resource attribute, i.e. definition obtained from the connector.
 *
 * To be used _solely_ within {@link ResourceAttributeDefinitionImpl}.
 */
public class RawResourceAttributeDefinition<T>
        extends PrismPropertyDefinitionImpl<T>
        implements MutableRawResourceAttributeDefinition<T>, ResourceAttributeDefinition<T> {

    @Serial private static final long serialVersionUID = -1756347754109326906L;

    /**
     * The definition is the same for all layers. However, we need to remember the current layer for {@link #getCurrentLayer()}
     * method.
     */
    @NotNull private final LayerType currentLayer;

    /**
     * Default value for {@link #currentLayer}.
     */
    private static final LayerType DEFAULT_LAYER = LayerType.MODEL;

    private String nativeAttributeName;
    private String frameworkAttributeName;
    private Boolean returnedByDefault;

    RawResourceAttributeDefinition(QName elementName, QName typeName) {
        this(elementName, typeName, DEFAULT_LAYER);
    }

    private RawResourceAttributeDefinition(QName elementName, QName typeName, @NotNull LayerType currentLayer) {
        super(elementName, typeName);
        this.currentLayer = currentLayer;
    }

    @NotNull
    @Override
    public ResourceAttribute<T> instantiate() {
        return instantiate(getItemName());
    }

    @NotNull
    @Override
    public ResourceAttribute<T> instantiate(QName name) {
        return new ResourceAttributeImpl<>(name, this);
    }

    public Boolean getReturnedByDefault() {
        return returnedByDefault;
    }

    @Override
    public void setReturnedByDefault(Boolean returnedByDefault) {
        checkMutable();
        this.returnedByDefault = returnedByDefault;
    }

    public String getNativeAttributeName() {
        return nativeAttributeName;
    }

    @Override
    public void setNativeAttributeName(String nativeAttributeName) {
        checkMutable();
        this.nativeAttributeName = nativeAttributeName;
    }

    public String getFrameworkAttributeName() {
        return frameworkAttributeName;
    }

    @Override
    public boolean hasRefinements() {
        return false;
    }

    @Override
    public void setFrameworkAttributeName(String frameworkAttributeName) {
        checkMutable();
        this.frameworkAttributeName = frameworkAttributeName;
    }

    //region Dummy methods

    @Override
    public PropertyLimitations getLimitations(LayerType layer) {
        return null;
    }

    @Override
    public ItemProcessing getProcessing(LayerType layer) {
        return getProcessing();
    }

    @Override
    public int getMaxOccurs(LayerType layer) {
        return getMaxOccurs();
    }

    @Override
    public int getMinOccurs(LayerType layer) {
        return getMinOccurs();
    }

    @Override
    public boolean canAdd(LayerType layer) {
        return canAdd();
    }

    @Override
    public boolean canRead(LayerType layer) {
        return canRead();
    }

    @Override
    public boolean canModify(LayerType layer) {
        return canModify();
    }

    @Override
    public AttributeFetchStrategyType getFetchStrategy() {
        return null;
    }

    @Override
    public @NotNull AttributeStorageStrategyType getStorageStrategy() {
        return AttributeStorageStrategyType.NORMAL;
    }

    @Override
    public Boolean isCached() {
        return null;
    }

    @Override
    public boolean isVolatilityTrigger() {
        return false;
    }

    @Override
    public Integer getModificationPriority() {
        return null;
    }

    @Override
    public Boolean getReadReplaceMode() {
        return null;
    }

    @Override
    public @NotNull RawResourceAttributeDefinition<T> forLayer(@NotNull LayerType layer) {
        if (layer == currentLayer) {
            return this;
        } else {
            RawResourceAttributeDefinition<T> newDef = new RawResourceAttributeDefinition<>(getItemName(), getTypeName(), layer);
            newDef.setNativeAttributeName(nativeAttributeName);
            newDef.setFrameworkAttributeName(frameworkAttributeName);
            newDef.setReturnedByDefault(returnedByDefault);
            newDef.freeze(); // TODO ok?
            return newDef;
        }
    }

    @Override
    public void setOverrideCanRead(Boolean value) {
        throw new UnsupportedOperationException("Cannot override canRead on raw attribute definition: " + this);
    }

    @Override
    public void setOverrideCanAdd(Boolean value) {
        throw new UnsupportedOperationException("Cannot override canAdd on raw attribute definition: " + this);
    }

    @Override
    public void setOverrideCanModify(Boolean value) {
        throw new UnsupportedOperationException("Cannot override canModify on raw attribute definition: " + this);
    }

    @Override
    public boolean isTolerant() {
        return false;
    }

    @Override
    public Boolean isSecondaryIdentifierOverride() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public RawResourceAttributeDefinition<T> getRawAttributeDefinition() {
        return this;
    }

    @Override
    public @Nullable MappingType getOutboundMappingBean() {
        return null;
    }

    @Override
    public @NotNull List<InboundMappingType> getInboundMappingBeans() {
        return List.of();
    }

    @Override
    public boolean isExclusiveStrong() {
        return false;
    }

    @Override
    public @NotNull List<String> getTolerantValuePatterns() {
        return List.of();
    }

    @Override
    public @NotNull List<String> getIntolerantValuePatterns() {
        return List.of();
    }

    @Override
    public boolean isDisplayNameAttribute() {
        return false;
    }

    @Override
    public @Nullable ItemCorrelatorDefinitionType getCorrelatorDefinition() {
        return null;
    }

    @Override
    public @Nullable ItemChangeApplicationModeType getChangeApplicationMode() {
        return null;
    }

    @Override
    public @Nullable String getLifecycleState() {
        return null;
    }

    @Override
    public @NotNull LayerType getCurrentLayer() {
        return currentLayer;
    }

    //endregion

    @Override
    public @NotNull Class<T> getTypeClass() {
        return MiscUtil.requireNonNull(
                super.getTypeClass(),
                () -> new IllegalStateException("No Java type for " + typeName + " in " + this));
    }

    public String getDisplayName() {
        // Not sure why, but this is the way it always was.
        return MiscUtil.getFirstNonNull(displayName, nativeAttributeName);
    }

    @Override
    public @NotNull Normalizer<String> getStringNormalizerForPolyStringProperty() {
        // These polystrings should not get normalized in midPoint style. TODO ok?
        return PrismContext.get().getNoOpNormalizer();
    }

    @Override
    public RawResourceAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation) {
        return (RawResourceAttributeDefinition<T>) super.deepClone(operation);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @NotNull
    @Override
    public RawResourceAttributeDefinition<T> clone() {
        return copyFrom(this);
    }

    /**
     * Creates a copy of the original definition, with applied the provided customizer.
     */
    @Experimental
    static <T> @NotNull RawResourceAttributeDefinition<T> spawn(
            @NotNull RawResourceAttributeDefinition<T> rawDefinition,
            @NotNull Consumer<RawResourceAttributeDefinition<T>> customizer) {
        RawResourceAttributeDefinition<T> copy = copyFrom(rawDefinition);
        customizer.accept(copy);
        return copy;
    }

    @Experimental
    private static <T> RawResourceAttributeDefinition<T> copyFrom(RawResourceAttributeDefinition<T> source) {
        RawResourceAttributeDefinition<T> clone =
                new RawResourceAttributeDefinition<>(source.getItemName(), source.getTypeName(), source.currentLayer);
        clone.copyDefinitionDataFrom(source);
        return clone;
    }

    private void copyDefinitionDataFrom(ResourceAttributeDefinition<T> source) {
        super.copyDefinitionDataFrom(source);
        nativeAttributeName = source.getNativeAttributeName();
        frameworkAttributeName = source.getFrameworkAttributeName();
        returnedByDefault = source.getReturnedByDefault();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RawResourceAttributeDefinition<?> that = (RawResourceAttributeDefinition<?>) o;
        return Objects.equals(nativeAttributeName, that.nativeAttributeName)
                && Objects.equals(frameworkAttributeName, that.frameworkAttributeName)
                && Objects.equals(returnedByDefault, that.returnedByDefault);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nativeAttributeName, frameworkAttributeName, returnedByDefault);
    }

    @Override
    protected void extendToString(StringBuilder sb) {
        super.extendToString(sb);
        if (getNativeAttributeName()!=null) {
            sb.append(" native=");
            sb.append(getNativeAttributeName());
        }
        if (getFrameworkAttributeName()!=null) {
            sb.append(" framework=");
            sb.append(getFrameworkAttributeName());
        }
        if (returnedByDefault != null) {
            sb.append(" returnedByDefault=");
            sb.append(returnedByDefault);
        }
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "rawRAD";
    }

    @Override
    public @NotNull MutableRawResourceAttributeDefinition<T> toMutable() {
        checkMutableOnExposing();
        return this;
    }

    @Override
    public String debugDump(int indent, LayerType layer) {
        var sb = new StringBuilder();
        DebugUtil.debugDumpWithLabelToString(sb, "attribute " + getItemName().getLocalPart(), this, indent);
        return sb.toString();
    }
}
