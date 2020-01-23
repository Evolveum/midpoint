/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.PrismPropertyDefinitionImpl;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import java.util.Objects;

import static com.evolveum.midpoint.prism.util.DefinitionUtil.addNamespaceIfApplicable;

/**
 * Resource Object Attribute Definition.
 *
 * Resource Object Attribute is a Property of Resource Object. All that applies
 * to property applies also to attribute, e.g. only a whole attributes can be
 * changed, they may be simple or complex types, they should be representable in
 * XML, etc. In addition, attribute definition may have some annotations that
 * suggest its purpose and use on the Resource.
 *
 * Resource Object Attribute understands resource-specific annotations such as
 * native attribute name.
 *
 * This class represents schema definition for resource object attribute. See
 * {@link Definition} for more details.
 *
 * @author Radovan Semancik
 *
 */
public class ResourceAttributeDefinitionImpl<T> extends PrismPropertyDefinitionImpl<T> implements MutableResourceAttributeDefinition<T> {
    private static final long serialVersionUID = -1756347754109326906L;

    private String nativeAttributeName;
    private String frameworkAttributeName;
    private Boolean returnedByDefault;

    public ResourceAttributeDefinitionImpl(QName elementName, QName typeName, PrismContext prismContext) {
        super(elementName, typeName, prismContext);
    }

    @NotNull
    @Override
    public ResourceAttribute<T> instantiate() {
        return instantiate(getItemName());
    }

    @NotNull
    @Override
    public ResourceAttribute<T> instantiate(QName name) {
        name = addNamespaceIfApplicable(name, this.itemName);
        return new ResourceAttributeImpl<>(name, this, prismContext);
    }

    @Override
    public Boolean getReturnedByDefault() {
        return returnedByDefault;
    }

    @Override
    public boolean isReturnedByDefault() {
        if (returnedByDefault == null) {
            return true;
        } else {
            return returnedByDefault;
        }
    }

    @Override
    public void setReturnedByDefault(Boolean returnedByDefault) {
        checkMutable();
        this.returnedByDefault = returnedByDefault;
    }

    /**
     * Returns true if the attribute is a (primary) identifier.
     *
     * Convenience method.
     *
     * @return true if the attribute is a (primary) identifier.
     */
    @Override
    public boolean isPrimaryIdentifier(ResourceAttributeContainerDefinition objectDefinition) {
        return isPrimaryIdentifier(objectDefinition.getComplexTypeDefinition());
    }

    @Override
    public boolean isPrimaryIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
        for (ResourceAttributeDefinition<?> identifier : objectDefinition.getPrimaryIdentifiers()) {
            if (this == identifier) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSecondaryIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
        for (ResourceAttributeDefinition<?> secondaryIdentifier : objectDefinition.getSecondaryIdentifiers()) {
            if (this == secondaryIdentifier) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns native attribute name.
     *
     * Native name of the attribute is a name as it is used on the resource or
     * as seen by the connector. It is used for diagnostics purposes and may be
     * used by the connector itself. As the attribute names in XSD have to
     * comply with XML element name limitations, this may be the only way how to
     * determine original attribute name.
     *
     * Returns null if native attribute name is not set or unknown.
     *
     * The name should be the same as the one used by the resource, if the
     * resource supports naming of attributes. E.g. in case of LDAP this
     * annotation should contain "cn", "givenName", etc. If the resource is not
     * that flexible, the native attribute names may be hardcoded (e.g.
     * "username", "homeDirectory") or may not be present at all.
     *
     * @return native attribute name
     */
    @Override
    public String getNativeAttributeName() {
        return nativeAttributeName;
    }

    @Override
    public void setNativeAttributeName(String nativeAttributeName) {
        checkMutable();
        this.nativeAttributeName = nativeAttributeName;
    }

    /**
     * Returns name of the attribute as given in the connector framework.
     * This is not used for any significant logic. It is mostly for diagnostics.
     *
     * @return name of the attribute as given in the connector framework.
     */
    @Override
    public String getFrameworkAttributeName() {
        return frameworkAttributeName;
    }

    @Override
    public void setFrameworkAttributeName(String frameworkAttributeName) {
        checkMutable();
        this.frameworkAttributeName = frameworkAttributeName;
    }

    @NotNull
    @Override
    public ResourceAttributeDefinitionImpl<T> clone() {
        ResourceAttributeDefinitionImpl<T> clone = new ResourceAttributeDefinitionImpl<>(getItemName(), getTypeName(), getPrismContext());
        copyDefinitionData(clone);
        return clone;
    }

    protected void copyDefinitionData(ResourceAttributeDefinitionImpl<T> clone) {
        super.copyDefinitionData(clone);
        clone.nativeAttributeName = this.nativeAttributeName;
        clone.frameworkAttributeName = this.frameworkAttributeName;
        clone.returnedByDefault = this.returnedByDefault;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ResourceAttributeDefinitionImpl<?> that = (ResourceAttributeDefinitionImpl<?>) o;
        return Objects.equals(nativeAttributeName, that.nativeAttributeName) && Objects.equals(frameworkAttributeName, that.frameworkAttributeName) && Objects.equals(returnedByDefault, that.returnedByDefault);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nativeAttributeName, frameworkAttributeName, returnedByDefault);
    }

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
    protected String getDebugDumpClassName() {
        return "RAD";
    }

    @Override
    public MutableResourceAttributeDefinition<T> toMutable() {
        checkMutableOnExposing();
        return this;
    }
}
