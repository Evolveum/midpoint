/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Objects;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.impl.PrismPropertyDefinitionImpl;

/**
 * A structure holding "raw" definition of a resource attribute, i.e. definition obtained from the connector.
 *
 * To be used _solely_ within {@link ResourceAttributeDefinitionImpl}.
 */
public class RawResourceAttributeDefinitionImpl<T>
        extends PrismPropertyDefinitionImpl<T>
        implements MutableRawResourceAttributeDefinition<T> {
    private static final long serialVersionUID = -1756347754109326906L;

    private String nativeAttributeName;
    private String frameworkAttributeName;
    private Boolean returnedByDefault;

    RawResourceAttributeDefinitionImpl(QName elementName, QName typeName) {
        super(elementName, typeName);
    }

    @NotNull
    @Override
    public ResourceAttribute<T> instantiate() {
        return instantiate(getItemName());
    }

    @NotNull
    @Override
    public ResourceAttribute<T> instantiate(QName name) {
        throw new UnsupportedOperationException("Cannot instantiate raw attribute definition: " + this);
    }

    @Override
    public Boolean getReturnedByDefault() {
        return returnedByDefault;
    }

    @Override
    public void setReturnedByDefault(Boolean returnedByDefault) {
        checkMutable();
        this.returnedByDefault = returnedByDefault;
    }

    @Override
    public String getNativeAttributeName() {
        return nativeAttributeName;
    }

    @Override
    public void setNativeAttributeName(String nativeAttributeName) {
        checkMutable();
        this.nativeAttributeName = nativeAttributeName;
    }

    @Override
    public String getFrameworkAttributeName() {
        return frameworkAttributeName;
    }

    @Override
    public void setFrameworkAttributeName(String frameworkAttributeName) {
        checkMutable();
        this.frameworkAttributeName = frameworkAttributeName;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @NotNull
    @Override
    public RawResourceAttributeDefinitionImpl<T> clone() {
        return copyFrom(this);
    }

    /**
     * Creates a copy of the original definition, with applied the provided customizer.
     */
    @Experimental
    static <T> @NotNull RawResourceAttributeDefinitionImpl<T> spawn(
            @NotNull RawResourceAttributeDefinition<T> rawDefinition,
            @NotNull Consumer<MutableRawResourceAttributeDefinition<T>> customizer) {
        RawResourceAttributeDefinitionImpl<T> copy = copyFrom(rawDefinition);
        customizer.accept(copy);
        return copy;
    }

    @Experimental
    private static <T> RawResourceAttributeDefinitionImpl<T> copyFrom(RawResourceAttributeDefinition<T> source) {
        RawResourceAttributeDefinitionImpl<T> clone =
                new RawResourceAttributeDefinitionImpl<>(source.getItemName(), source.getTypeName());
        clone.copyDefinitionDataFrom(source);
        return clone;
    }

    private void copyDefinitionDataFrom(RawResourceAttributeDefinition<T> source) {
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
        RawResourceAttributeDefinitionImpl<?> that = (RawResourceAttributeDefinitionImpl<?>) o;
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
}
