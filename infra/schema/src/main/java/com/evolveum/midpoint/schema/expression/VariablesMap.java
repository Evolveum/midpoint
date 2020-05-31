/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.util.*;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class VariablesMap implements Map<String,TypedValue>, DebugDumpable {

    private Map<String, TypedValue> variables;

    /**
     * We register aliases to make variable dumps more organized by eliminating duplicate objects.
     * But currently (for simplicity) we keep values for both real variables and aliases in variables map.
     *
     * Note that manipulating variables directly e.g. by entrySet-returned value could lead to inconsistencies.
     *
     * The aliases map semantics is: alias -> real name.
     *
     * Anyway, aliases should be used SOLELY for presentation purposes. No "real" functionality should depend on them.
     */
    private final Map<String, String> aliases = new HashMap<>();

    public VariablesMap() {
        variables = new HashMap<>();
    }

    private VariablesMap(Map<String, TypedValue> variablesMap) {
        this.variables = variablesMap;
    }

    public int size() {
        return variables.size();
    }

    public boolean isEmpty() {
        return variables.isEmpty();
    }

    public boolean containsKey(Object key) {
        return variables.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return variables.containsValue(value);
    }

    public TypedValue get(Object key) {
        return variables.get(key);
    }

    @SuppressWarnings("ConstantConditions")
    public TypedValue put(String key, TypedValue typedValue) {
        if (typedValue == null) {
            throw new IllegalArgumentException("Attempt to set variable '"+key+"' with null typed value: "+typedValue);
        }
        if (!typedValue.canDetermineType()) {
            throw new IllegalArgumentException("Attempt to set variable '"+key+"' without determinable type: "+typedValue);
        }
        return variables.put(key, typedValue);
    }

    // mainVariable of "null" means the default source
    public void registerAlias(String alias, @Nullable String realName) {
        if (isAlias(realName)) {
            throw new IllegalArgumentException("Trying to put alias definition: " + alias + "->" + realName + ", but " + realName + " is itself an alias");
        }
        aliases.put(alias, realName);
    }

    /**
     * Use only if you previously register all variables from this map!
     */
    public void registerAliasesFrom(VariablesMap map) {
        aliases.putAll(map.aliases);
    }

    public void unregisterAlias(String alias) {
        aliases.remove(alias);
    }

    @SuppressWarnings("rawtypes")
    public <D extends ItemDefinition> TypedValue put(String key, Object value, D definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Attempt to set variable '"+key+"' without definition: " + value);
        }
        return variables.put(key, new TypedValue<>(value, definition));
    }

    /**
     * Note: Type of the value should really be Object and not T. The value may be quite complicated,
     * e.g. it may be ItemDeltaItem of the actual real value. However, the class defines the real type
     * of the value precisely.
     */
    public <T> TypedValue put(String key, Object value, Class<T> typeClass) {
        if (typeClass == null) {
            throw new IllegalArgumentException("Attempt to set variable '"+key+"' without class specification: " + value);
        }
        return variables.put(key, new TypedValue<>(value, typeClass));
    }

    /**
     * Convenience method to put objects with definition.
     * Maybe later improve by looking up full definition.
     */
    @SuppressWarnings("unchecked")
    public <O extends ObjectType> TypedValue<O> putObject(String key, O objectType, Class<O> expectedClass) {
        if (objectType == null) {
            return put(key, null, expectedClass);
        } else {
            return put(key, objectType, objectType.asPrismObject().getDefinition());
        }
    }

    /**
     * Convenience method to put objects with definition.
     * Maybe later improve by looking up full definition.
     */
    @SuppressWarnings("unchecked")
    public <O extends ObjectType> TypedValue<O> putObject(String key, PrismObject<O> object, Class<O> expectedClass) {
        if (object == null) {
            return put(key, null, expectedClass);
        } else {
            return put(key, object, object.getDefinition());
        }
    }

    /**
     * Convenience method to put multivalue variables (lists).
     * This is very simple now. But later on we may need to declare generics.
     * Therefore dedicated method would be easier to find all usages and fix them.
     */
    @SuppressWarnings("unchecked")
    public <T> TypedValue<List<T>> putList(String key, List<T> list) {
        return put(key, list, List.class);
    }

    public TypedValue remove(Object key) {
        aliases.remove(key);
        return variables.remove(key);
    }

    public void putAll(Map<? extends String, ? extends TypedValue> m) {
        variables.putAll(m);
    }

    public void putAll(VariablesMap m) {
        variables.putAll(m);
        aliases.putAll(m.aliases);
    }

    public void clear() {
        aliases.clear();
        variables.clear();
    }

    @NotNull
    public Set<String> keySet() {
        return variables.keySet();
    }

    @NotNull
    public Collection<TypedValue> values() {
        return variables.values();
    }

    @NotNull
    public Set<Entry<String, TypedValue>> entrySet() {
        return variables.entrySet();
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
    public static VariablesMap create(PrismContext prismContext, Object... parameters) {
        VariablesMap vars = new VariablesMap();
        vars.fillIn(prismContext, parameters);
        return vars;
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
    protected void fillIn(PrismContext prismContext, Object... parameters) {
        for (int i = 0; i < parameters.length; i += 3) {
            Object nameObj = parameters[i];
            String name = null;
            if (nameObj instanceof String) {
                name = (String)nameObj;
            } else if (nameObj instanceof QName) {
                name = ((QName)nameObj).getLocalPart();
            }
            Object value = parameters[i+1];
            Object defObj = parameters[i+2];
            ItemDefinition def = null;
            if (defObj instanceof QName) {
                def = prismContext.definitionFactory().createPropertyDefinition(
                        new QName(SchemaConstants.NS_C, name), (QName)defObj, null, null);
                put(name, value, def);
            } else if (defObj instanceof PrimitiveType) {
                def = prismContext.definitionFactory().createPropertyDefinition(
                        new QName(SchemaConstants.NS_C, name), ((PrimitiveType)defObj).getQname(), null, null);
                put(name, value, def);
            } else if (defObj instanceof ItemDefinition) {
                def = (ItemDefinition)defObj;
                put(name, value, def);
            } else if (defObj instanceof Class) {
                put(name, value, (Class)defObj);
            } else {
                throw new IllegalArgumentException("Unexpected def "+defObj);
            }

        }
    }

    public static VariablesMap emptyMap() {
        return new VariablesMap(Collections.emptyMap());
    }

    public boolean equals(Object o) {
        return variables.equals(o);
    }

    public int hashCode() {
        return variables.hashCode();
    }

    @Override
    public String toString() {
        return variables.toString();
    }

    public String formatVariables() {
        StringBuilder sb = new StringBuilder();
        Iterator<Entry<String, TypedValue>> i = entrySet().iterator();
        while (i.hasNext()) {
            Entry<String, TypedValue> entry = i.next();
            if (!isAlias(entry.getKey())) {
                SchemaDebugUtil.indentDebugDump(sb, 1);
                sb.append(entry.getKey());
                sb.append(getAliasesListFormatted(entry.getKey()));
                sb.append(": ");
                TypedValue valueDef = entry.getValue();
                Object value = valueDef.getValue();
                // TODO: dump definitions?
                if (value instanceof DebugDumpable) {
                    sb.append("\n");
                    sb.append(((DebugDumpable) value).debugDump(2));
                } else if (value instanceof Element) {
                    sb.append("\n");
                    sb.append(DOMUtil.serializeDOMToString(((Element) value)));
                } else {
                    sb.append(SchemaDebugUtil.prettyPrint(value));
                }
                if (i.hasNext()) {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    private String getAliasesListFormatted(String key) {
        Collection<String> aliases = getAliases(key);
        if (aliases.isEmpty()) {
            return "";
        } else {
            return aliases.stream().collect(Collectors.joining(", ", " (", ")"));
        }
    }

    @NotNull
    public Collection<String> getAliases(String key) {
        return aliases.entrySet().stream()
                .filter(e -> Objects.equals(key, e.getValue()))
                .map(Entry::getKey)
                .collect(Collectors.toList());
    }

    public boolean isAlias(String key) {
        return aliases.containsKey(key);
    }

    public String getAliasResolution(String key) {
        return aliases.get(key);
    }

    public String dumpSingleLine() {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, TypedValue> entry: variables.entrySet()) {
            if (!isAlias(entry.getKey())) {
                sb.append(entry.getKey());
                sb.append(getAliasesListFormatted(entry.getKey()));
                sb.append("=");
                sb.append(PrettyPrinter.prettyPrint(entry.getValue().getValue()));
                sb.append("; ");
            }
        }
        return sb.toString();
    }

    // TODO: dump definitions?
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpMapMultiLine(sb, getAliasReducedMap(), 1);
        return sb.toString();
    }

    private Map<String, TypedValue> getAliasReducedMap() {
        Map<String, TypedValue> rv = new HashMap<>();
        for (Entry<String, TypedValue> entry : variables.entrySet()) {
            String key = entry.getKey();
            if (!isAlias(key)) {
                rv.put(key + getAliasesListFormatted(key), entry.getValue());
            }
        }
        return rv;
    }
}
