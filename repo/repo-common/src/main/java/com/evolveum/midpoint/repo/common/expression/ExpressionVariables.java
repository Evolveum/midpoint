/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import static java.util.Collections.emptySet;

/**
 * @author Radovan Semancik
 */
public class ExpressionVariables extends VariablesMap {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionVariables.class);

    /**
     * Adds map of extra variables to the expression.
     * If there are variables with deltas (ObjectDeltaObject) the operation fail because
     * it cannot decide which version to use.
     */
    public void addVariableDefinitions(VariablesMap extraVariables) {
        addVariableDefinitions(extraVariables, emptySet());
    }

    public void addVariableDefinitions(VariablesMap extraVariables, @NotNull Collection<String> exceptFor) {
        for (Entry<String, TypedValue> entry : extraVariables.entrySet()) {
            if (exceptFor.contains(entry.getKey())) {
                continue;
            }
            TypedValue valueDef = entry.getValue();
            Object value = valueDef.getValue();
            if (!areDeltasAllowed() && value instanceof ObjectDeltaObject<?>) {
                ObjectDeltaObject<?> odo = (ObjectDeltaObject<?>)value;
                if (odo.getObjectDelta() != null) {
                    throw new IllegalArgumentException("Cannot use variables with deltas in addVariableDefinitions. Use addVariableDefinitionsOld or addVariableDefinitionsNew.");
                }
                value = odo.getOldObject();
            }
            put(entry.getKey(), valueDef.createTransformed(value));
            if (extraVariables.isAlias(entry.getKey())) {
                registerAlias(entry.getKey(), extraVariables.getAliasResolution(entry.getKey()));
            }
        }
    }

    // TODO There are situations where we do not want to be any relative data (ObjectDeltaObject, ItemDeltaItem) here.
    // Namely, when this class is used in lower layers of evaluation (e.g. script evaluation). However, as of 3.4.1,
    // we don't want to start a big cleanup of this functionality, so - for now - let us just put a placeholder here.
    // The plan is to distinguish "real" ExpressionVariables that may contain deltas and ScriptVariables that may not.
    private boolean areDeltasAllowed() {
        return true;
    }

    /**
     * Adds map of extra variables to the expression.
     * If there are variables with deltas (ObjectDeltaObject) it takes the "old" version
     * of the object.
     */
    public void addVariableDefinitionsOld(VariablesMap extraVariables) {
        for (Entry<String, TypedValue> entry : extraVariables.entrySet()) {
            TypedValue valueDef = entry.getValue();
            Object value = valueDef.getValue();
            if (value instanceof ObjectDeltaObject<?>) {
                ObjectDeltaObject<?> odo = (ObjectDeltaObject<?>)value;
                value = odo.getOldObject();
            } else if (value instanceof ItemDeltaItem<?,?>) {
                ItemDeltaItem<?,?> idi = (ItemDeltaItem<?,?>)value;
                value = idi.getItemOld();
            }
            put(entry.getKey(), valueDef.createTransformed(value));
        }
        registerAliasesFrom(extraVariables);
    }

    /**
     * Adds map of extra variables to the expression.
     * If there are variables with deltas (ObjectDeltaObject) it takes the "new" version
     * of the object.
     */
    public void addVariableDefinitionsNew(VariablesMap extraVariables) {
        for (Entry<String, TypedValue> entry : extraVariables.entrySet()) {
            TypedValue valueDef = entry.getValue();
            Object value = valueDef.getValue();
            if (value instanceof ObjectDeltaObject<?>) {
                ObjectDeltaObject<?> odo = (ObjectDeltaObject<?>)value;
                value = odo.getNewObject();
            } else if (value instanceof ItemDeltaItem<?,?>) {
                ItemDeltaItem<?,?> idi = (ItemDeltaItem<?,?>)value;
                value = idi.getItemNew();
            }
            put(entry.getKey(), valueDef.createTransformed(value));
        }
        registerAliasesFrom(extraVariables);
    }

    public void setRootNode(ObjectReferenceType objectRef, PrismReferenceDefinition def) {
        put(null, objectRef, def);
    }

    // TODO: maybe replace by put?
    public <D extends ItemDefinition> void addVariableDefinition(String name, Object value, D definition) {
        if (containsKey(name)) {
            LOGGER.warn("Duplicate definition of variable {}", name);
            return;
        }
        replaceVariableDefinition(name, value, definition);
    }

    // TODO: maybe replace by put?
    public <D extends ItemDefinition> void replaceVariableDefinition(String name, Object value, D definition) {
        put(name, value, definition);
    }

    public Object getValue(String name) {
        TypedValue typedValue = get(name);
        if (typedValue == null) {
            return null;
        }
        return typedValue.getValue();
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String name, Class<T> type) throws SchemaException {
        Object object = getValue(name);
        if (object == null) {
            return null;
        }
        if (type.isAssignableFrom(object.getClass())) {
            return (T) object;
        }
        throw new SchemaException("Expected type "+type.getSimpleName()+" in variable "+name+", but found type "+object.getClass());
    }

    // TODO: do we need this?
    public <O extends ObjectType> PrismObject<O> getValueNew(String name) throws SchemaException {
        Object object = getValue(name);
        if (object == null) {
            return null;
        }
        if (object instanceof PrismObject) {
            return (PrismObject<O>) object;
        }
        if (object instanceof ObjectDeltaObject<?>) {
            ObjectDeltaObject<O> odo = (ObjectDeltaObject<O>)object;
            return odo.getNewObject();
        }
        throw new SchemaException("Expected object in variable "+name+", but found type "+object.getClass());
    }

    /**
     * Expects name-value-definition triples.
     * Definition can be just a type QName.
     *
     * E.g.
     * create(var1name, var1value, var1type, var2name, var2value, var2type, ...)
     *
     * Mostly for testing. Use at your own risk.
     */
    public static ExpressionVariables create(PrismContext prismContext, Object... parameters) {
        ExpressionVariables vars = new ExpressionVariables();
        vars.fillIn(prismContext, parameters);
        return vars;
    }

    @Override
    public String toString() {
        return "variables(" + super.toString() + ")";
    }

    public boolean haveDeltas() {
        for (Map.Entry<String, TypedValue> entry: entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            Object value = entry.getValue().getValue();
            if (value instanceof ObjectDeltaObject<?>) {
                if (((ObjectDeltaObject<?>)value).getObjectDelta() != null && !((ObjectDeltaObject<?>)value).getObjectDelta().isEmpty()) {
                    return true;
                }
            } else if (value instanceof ItemDeltaItem<?,?>) {
                if (((ItemDeltaItem<?,?>)value).getDelta() != null && !((ItemDeltaItem<?,?>)value).getDelta().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
