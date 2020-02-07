/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.ComplexTypeDefinitionImpl;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author semancik
 *
 */
public class ObjectClassComplexTypeDefinitionImpl extends ComplexTypeDefinitionImpl implements MutableObjectClassComplexTypeDefinition {
    private static final long serialVersionUID = 1L;

    @NotNull private final Collection<ResourceAttributeDefinition<?>> identifiers = new ArrayList<>(1);
    @NotNull private final Collection<ResourceAttributeDefinition<?>> secondaryIdentifiers = new ArrayList<>(1);
    private ResourceAttributeDefinition descriptionAttribute;
    private ResourceAttributeDefinition displayNameAttribute;
    private ResourceAttributeDefinition namingAttribute;
    private boolean defaultInAKind = false;
    private ShadowKindType kind;
    private String intent;
    private String nativeObjectClass;
    private boolean auxiliary;

    public ObjectClassComplexTypeDefinitionImpl(QName typeName, PrismContext prismContext) {
        super(typeName, prismContext);
    }

    @NotNull
    @Override
    public Collection<? extends ResourceAttributeDefinition<?>> getAttributeDefinitions() {
        return Collections.unmodifiableList(
                getDefinitions().stream()
                        .map(def -> (ResourceAttributeDefinition<?>) def)
                        .collect(Collectors.toList()));
    }

    @Override
    public void add(ItemDefinition<?> definition) {
        if (ResourceAttributeDefinition.class.isAssignableFrom(definition.getClass())) {
            super.add(definition);
        } else {
            throw new IllegalArgumentException("Only ResourceAttributeDefinitions should be put into"
                    + " a ObjectClassComplexTypeDefinition. Item definition = " + definition + ","
                    + " ObjectClassComplexTypeDefinition = " + this);
        }
    }

    @NotNull
    @Override
    public Collection<? extends ResourceAttributeDefinition<?>> getPrimaryIdentifiers() {
        return identifiers;
    }

    @Override
    public void addPrimaryIdentifier(ResourceAttributeDefinition<?> identifier) {
        checkMutable();
        identifiers.add(identifier);
    }

    @NotNull
    @Override
    public Collection<? extends ResourceAttributeDefinition<?>> getSecondaryIdentifiers() {
        return secondaryIdentifiers;
    }

    @Override
    public void addSecondaryIdentifier(ResourceAttributeDefinition<?> identifier) {
        checkMutable();
        secondaryIdentifiers.add(identifier);
    }

    @Override
    public <X> ResourceAttributeDefinition<X> getDescriptionAttribute() {
        return descriptionAttribute;
    }

    @Override
    public void setDescriptionAttribute(ResourceAttributeDefinition<?> descriptionAttribute) {
        checkMutable();
        this.descriptionAttribute = descriptionAttribute;
    }

    @Override
    public <X> ResourceAttributeDefinition<X> getNamingAttribute() {
        return namingAttribute;
    }

    @Override
    public void setNamingAttribute(ResourceAttributeDefinition<?> namingAttribute) {
        checkMutable();
        this.namingAttribute = namingAttribute;
    }

    @Override
    public void setNamingAttribute(QName namingAttribute) {
        setNamingAttribute(findAttributeDefinition(namingAttribute));
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
    public ShadowKindType getKind() {
        return kind;
    }

    @Override
    public void setKind(ShadowKindType kind) {
        checkMutable();
        this.kind = kind;
    }

    @Override
    public boolean isDefaultInAKind() {
        return defaultInAKind;
    }

    @Override
    public void setDefaultInAKind(boolean defaultAccountType) {
        checkMutable();
        this.defaultInAKind = defaultAccountType;
    }

    @Override
    public String getIntent() {
        return intent;
    }

    @Override
    public void setIntent(String intent) {
        checkMutable();
        this.intent = intent;
    }

    @Override
    public ResourceAttributeDefinition<?> getDisplayNameAttribute() {
        return displayNameAttribute;
    }

    @Override
    public void setDisplayNameAttribute(ResourceAttributeDefinition<?> displayName) {
        checkMutable();
        this.displayNameAttribute = displayName;
    }

    /**
     * TODO
     *
     * Convenience method. It will internally look up the correct definition.
     */
    @Override
    public void setDisplayNameAttribute(QName displayName) {
        setDisplayNameAttribute(findAttributeDefinition(displayName));
    }

    @Override
    public <X> ResourceAttributeDefinitionImpl<X> createAttributeDefinition(QName name, QName typeName) {
        ResourceAttributeDefinitionImpl<X> propDef = new ResourceAttributeDefinitionImpl<>(name, typeName, prismContext);
        add(propDef);
        return propDef;
    }

    @Override
    public <X> ResourceAttributeDefinitionImpl<X> createAttributeDefinition(String localName, QName typeName) {
        QName name = new QName(getSchemaNamespace(),localName);
        return createAttributeDefinition(name,typeName);
    }

    @Override
    public <X> ResourceAttributeDefinition<X> createAttributeDefinition(String localName, String localTypeName) {
        QName name = new QName(getSchemaNamespace(),localName);
        QName typeName = new QName(getSchemaNamespace(),localTypeName);
        return createAttributeDefinition(name,typeName);
    }

    @Override
    public boolean matches(ShadowType shadowType) {
        if (shadowType == null) {
            return false;
        }
        if (!QNameUtil.match(getTypeName(), shadowType.getObjectClass())) {
            return false;
        }
        return true;
    }

    /**
     * This may not be really "clean" as it actually does two steps instead of one. But it is useful.
     */
    @Override
    public ResourceAttributeContainer instantiate(QName elementName) {
        return instantiate(elementName, this);
    }

    public static ResourceAttributeContainer instantiate(QName elementName, ObjectClassComplexTypeDefinition ocdef) {
        ResourceAttributeContainerDefinition racDef = ocdef.toResourceAttributeContainerDefinition(elementName);
        return new ResourceAttributeContainerImpl(elementName, racDef, ocdef.getPrismContext());
    }

    @NotNull
    @Override
    public ObjectClassComplexTypeDefinitionImpl clone() {
        ObjectClassComplexTypeDefinitionImpl clone = new ObjectClassComplexTypeDefinitionImpl(
                getTypeName(), prismContext);
        copyDefinitionData(clone);
        clone.shared = false;
        return clone;
    }

    @NotNull
    @Override
    public ObjectClassComplexTypeDefinition deepClone(Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
        return (ObjectClassComplexTypeDefinition) super.deepClone(ctdMap, onThisPath, postCloneAction);
    }

    protected void copyDefinitionData(ObjectClassComplexTypeDefinitionImpl clone) {
        super.copyDefinitionData(clone);
        clone.kind = this.kind;
        clone.intent = this.intent;
        clone.defaultInAKind = this.defaultInAKind;
        clone.descriptionAttribute = this.descriptionAttribute;
        clone.displayNameAttribute = this.displayNameAttribute;
        clone.identifiers.addAll(this.identifiers);
        clone.namingAttribute = this.namingAttribute;
        clone.nativeObjectClass = this.nativeObjectClass;
        clone.secondaryIdentifiers.addAll(this.secondaryIdentifiers);
        clone.auxiliary = this.auxiliary;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (auxiliary ? 1231 : 1237);
        result = prime * result + (defaultInAKind ? 1231 : 1237);
        result = prime * result + ((descriptionAttribute == null) ? 0 : descriptionAttribute.hashCode());
        result = prime * result + ((displayNameAttribute == null) ? 0 : displayNameAttribute.hashCode());
        result = prime * result + identifiers.hashCode();
        result = prime * result + ((intent == null) ? 0 : intent.hashCode());
        result = prime * result + ((kind == null) ? 0 : kind.hashCode());
        result = prime * result + ((namingAttribute == null) ? 0 : namingAttribute.hashCode());
        result = prime * result + ((nativeObjectClass == null) ? 0 : nativeObjectClass.hashCode());
        result = prime * result + secondaryIdentifiers.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ObjectClassComplexTypeDefinitionImpl other = (ObjectClassComplexTypeDefinitionImpl) obj;
        if (auxiliary != other.auxiliary) {
            return false;
        }
        if (defaultInAKind != other.defaultInAKind) {
            return false;
        }
        if (descriptionAttribute == null) {
            if (other.descriptionAttribute != null) {
                return false;
            }
        } else if (!descriptionAttribute.equals(other.descriptionAttribute)) {
            return false;
        }
        if (displayNameAttribute == null) {
            if (other.displayNameAttribute != null) {
                return false;
            }
        } else if (!displayNameAttribute.equals(other.displayNameAttribute)) {
            return false;
        }
        if (!identifiers.equals(other.identifiers)) {
            return false;
        }
        if (intent == null) {
            if (other.intent != null) {
                return false;
            }
        } else if (!intent.equals(other.intent)) {
            return false;
        }
        if (kind != other.kind) {
            return false;
        }
        if (namingAttribute == null) {
            if (other.namingAttribute != null) {
                return false;
            }
        } else if (!namingAttribute.equals(other.namingAttribute)) {
            return false;
        }
        if (nativeObjectClass == null) {
            if (other.nativeObjectClass != null) {
                return false;
            }
        } else if (!nativeObjectClass.equals(other.nativeObjectClass)) {
            return false;
        }
        if (!secondaryIdentifiers.equals(other.secondaryIdentifiers)) {
            return false;
        }
        return true;
    }

    protected String getDebugDumpClassName() {
        return "OCD";
    }

    @Override
    protected void extendDumpHeader(StringBuilder sb) {
        super.extendDumpHeader(sb);
        if (defaultInAKind) {
            sb.append(" def");
        }
        if (auxiliary) {
            sb.append(" aux");
        }
        if (kind != null) {
            sb.append(" ").append(kind.value());
        }
        if (intent != null) {
            sb.append(" intent=").append(intent);
        }
    }

    @Override
    protected void extendItemDumpDefinition(StringBuilder sb, ItemDefinition<?> def) {
        super.extendItemDumpDefinition(sb, def);
        if (getPrimaryIdentifiers().contains(def)) {
            sb.append(",primID");
        }
        if (getSecondaryIdentifiers().contains(def)) {
            sb.append(",secID");
        }
    }

    @Override
    public MutableObjectClassComplexTypeDefinition toMutable() {
        checkMutableOnExposing();
        return this;
    }
}
