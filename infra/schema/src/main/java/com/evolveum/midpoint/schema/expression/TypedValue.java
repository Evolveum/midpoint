/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * Value and definition pair. E.g. used in expression variable maps.
 * We need to have explicit type here. It may happen that there will be
 * variables without any value. But we need to know the type of the
 * variable to compile the scripts properly.
 *
 * The definition, typeClass and the T parameter of this class refer to the
 * type of the value as it should appear in the expression. The actual
 * value may be different when TypedValue is created. The value may
 * get converted as it is passing down the stack.
 *
 * E.g. if we want script variable to contain a user, the type should be
 * declared as UserType, not as object reference - even though the value
 * is object reference when the TypedValue is created. But the reference
 * is resolved down the way to place user in the value.
 *
 * @author Radovan Semancik
 */
public class TypedValue<T> implements ShortDumpable {

    /**
     * Value may be null. This means variable without a value.
     * But even in that case definition should be provided.
     * The value is not T, it is Object. The value may not be in its
     * final form yet. It may get converted later.
     */
    private Object value;

    /**
     * Definition should be filled in for all value that can be described using Prism definitions.
     */
    private ItemDefinition<?> definition;

    /**
     * Type class. Should be filled in for values that are not prism values.
     */
    private Class<T> typeClass;

    public TypedValue() {
        super();
    }

    public TypedValue(Item<?, ?> prismItem) {
        super();
        this.value = (T) prismItem;
        this.definition = prismItem.getDefinition();
        if (definition == null) {
            throw new IllegalArgumentException("No definition when setting variable value to "+prismItem);
        }
    }

    public TypedValue(Object value, ItemDefinition<?> definition) {
        super();
        validateValue(value);
        this.value = value;
        this.definition = definition;
    }

    public TypedValue(Object value, Class<T> typeClass) {
        super();
        validateValue(value);
        this.value = value;
        this.typeClass = typeClass;
    }

    public TypedValue(Object value, ItemDefinition<?> definition, Class<T> typeClass) {
        super();
        validateValue(value);
        this.value = value;
        this.definition = definition;
        this.typeClass = typeClass;
    }

    /**
     * Creates a {@link TypedValue} with a collection of real values.
     *
     * Simple implementation (for now):
     *
     * . assumes the values have the same class;
     * . does NOT unwrap the values into "real values", using them wrapped.
     *
     * (Moved here from `report-impl` module, can be improved later, if needed.)
     *
     * @param typeName Name of the type, if known
     */
    @Experimental
    public static TypedValue<?> of(@NotNull Collection<? extends PrismValue> prismValues, @Nullable QName typeName) {
        Class<?> clazz = typeName != null ? PrismContext.get().getSchemaRegistry().determineClassForType(typeName) : null;
        if (clazz == null && !prismValues.isEmpty()) {
            clazz = prismValues.iterator().next().getRealClass();
        }
        if (clazz == null) {
            clazz = Object.class;
        }
        return new TypedValue<>(prismValues, clazz);
    }

    @Experimental
    public static TypedValue<?> of(@Nullable Object value, @NotNull Class<?> defaultClass) {
        if (value != null) {
            return new TypedValue<>(value, value.getClass());
        } else {
            return new TypedValue<>(null, defaultClass);
        }
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        validateValue(value);
        this.value = value;
    }

    private void validateValue(Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof TypedValue) {
            throw new IllegalArgumentException("TypedValue in TypedValue in "+this);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <D extends ItemDefinition> D getDefinition() {
        return (D) definition;
    }

    public void setDefinition(ItemDefinition<?> definition) {
        this.definition = definition;
    }

    public Class<T> getTypeClass() {
        return typeClass;
    }

    public void setTypeClass(Class<T> typeClass) {
        this.typeClass = typeClass;
    }

    public boolean canDetermineType() {
        return definition != null || typeClass != null;
    }

    public Class<T> determineClass() throws SchemaException {
        if (definition == null) {
            if (typeClass == null) {
                throw new SchemaException("Cannot determine class for variable, neither definition nor class specified");
            } else {
                return typeClass;
            }
        } else {
            Class determinedClass;
            if (definition instanceof PrismReferenceDefinition) {
                // Stock prism reference would return ObjectReferenceType from prism schema.
                // But we have exteded type for this.
                // TODO: how to make this more elegant?
                determinedClass = ObjectReferenceType.class;
            } else {
                determinedClass = definition.getTypeClass();
            }
            if (determinedClass == null) {
                throw new SchemaException("Cannot determine class from definition "+definition);
            }
            return determinedClass;
        }
    }

    /**
     * Returns new TypedValue that has a new (transformed) value, but has the same definition.
     */
    public TypedValue<T> createTransformed(Object newValue) {
        return new TypedValue<>(newValue, definition, typeClass);
    }

    /**
     * Returns the shallow clone of the original object. Value nor definition is copied, NOT cloned.
     */
    public TypedValue<T> shallowClone() {
        return new TypedValue<>(value, definition, typeClass);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((definition == null) ? 0 : definition.hashCode());
        result = prime * result + ((typeClass == null) ? 0 : typeClass.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TypedValue other = (TypedValue) obj;
        if (definition == null) {
            if (other.definition != null) {
                return false;
            }
        } else if (!definition.equals(other.definition)) {
            return false;
        }
        if (typeClass == null) {
            if (other.typeClass != null) {
                return false;
            }
        } else if (!typeClass.equals(other.typeClass)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TypedValue(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(value);
        if (definition != null) {
            sb.append(", definition=");
            definition.debugDumpShortToString(sb);
        }
        if (typeClass != null) {
            sb.append(", class=").append(typeClass.getSimpleName());
        }
        if (definition == null && typeClass == null) {
            sb.append(", definition/class=null");
        }
    }

    // TODO - or revive? Or make sure prismContext is set here?
    public void setPrismContext(PrismContext prismContext) {
        if (value instanceof PrismValue) {
            ((PrismValue) value).setPrismContext(prismContext);
        } else if (value instanceof Item) {
            ((Item) value).setPrismContext(prismContext);
        }
    }
}
