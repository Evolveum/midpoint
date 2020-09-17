/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.TypeDefinition;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.QNameUtil;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class TypeDefinitionImpl extends DefinitionImpl implements TypeDefinition {

    private QName superType;
    protected Class<?> compileTimeClass;
    @NotNull final Set<TypeDefinition> staticSubTypes = new HashSet<>();
    protected Integer instantiationOrder;

    TypeDefinitionImpl(QName typeName, PrismContext prismContext) {
        super(typeName, prismContext);
    }

    @Override
    public QName getSuperType() {
        return superType;
    }

    public void setSuperType(QName superType) {
        checkMutable();
        this.superType = superType;
    }

    @Override
    @NotNull
    public Collection<TypeDefinition> getStaticSubTypes() {
        return staticSubTypes;
    }

    public void addStaticSubType(TypeDefinition subtype) {
        staticSubTypes.add(subtype);
    }

    @Override
    public Integer getInstantiationOrder() {
        return instantiationOrder;
    }

    public void setInstantiationOrder(Integer instantiationOrder) {
        checkMutable();
        this.instantiationOrder = instantiationOrder;
    }

    @Override
    public Class<?> getCompileTimeClass() {
        return compileTimeClass;
    }

    public void setCompileTimeClass(Class<?> compileTimeClass) {
        checkMutable();
        this.compileTimeClass = compileTimeClass;
    }

    protected void copyDefinitionData(TypeDefinitionImpl clone) {
        super.copyDefinitionData(clone);
        clone.superType = this.superType;
        clone.compileTimeClass = this.compileTimeClass;
    }

    @Override
    public boolean canRepresent(QName typeName) {
        if (QNameUtil.match(typeName, getTypeName())) {
            return true;
        }
        if (superType != null) {
            ComplexTypeDefinition supertypeDef = getPrismContext().getSchemaRegistry().findComplexTypeDefinitionByType(superType);
            return supertypeDef.canRepresent(typeName);
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)  return true;
        if (!(o instanceof TypeDefinitionImpl)) return false;
        if (!super.equals(o)) return false;
        TypeDefinitionImpl that = (TypeDefinitionImpl) o;
        return Objects.equals(superType, that.superType) &&
                Objects.equals(compileTimeClass, that.compileTimeClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), superType, compileTimeClass);
    }

    /**
     * Crawls up the type hierarchy and looks for type name equivalence.
     */
    @Override
    @Experimental
    public boolean isAssignableFrom(TypeDefinition other, SchemaRegistry schemaRegistry) {
        if (QNameUtil.match(this.getTypeName(), DOMUtil.XSD_ANYTYPE)) {
            return true;
        }
        while (other != null) {
            if (QNameUtil.match(this.getTypeName(), other.getTypeName())) {
                return true;
            }
            if (other.getSuperType() == null) {
                return false;
            }
            other = schemaRegistry.findTypeDefinitionByType(other.getSuperType());
        }
        return false;
    }
}
