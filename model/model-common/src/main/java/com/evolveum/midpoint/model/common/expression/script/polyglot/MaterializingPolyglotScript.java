package com.evolveum.midpoint.model.common.expression.script.polyglot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Value;

/**
 * Implementation of the {@link PolyglotScript} interface that provides script execution and handles materialization
 * of results into Java objects.
 *
 * This class wraps a given polyglot {@link Value} object, which represent a script and allows executing it with
 * variable bindings, translating the polyglot execution results into readable Java structures such as primitives,
 * instants, lists, and maps.
 */
public class MaterializingPolyglotScript implements PolyglotScript {

    private final Value script;

    public MaterializingPolyglotScript(Value script) {
        this.script = script;
    }

    @Override
    public Object evaluate(Map<String, Object> variables) {
        final Value bindings = this.script.getContext().getBindings("js");
        variables.forEach(bindings::putMember);
        final Value result = this.script.execute();
        return materialize(result);
    }

    private static Object materialize(Value v) {
        if (v.isNull()) {
            return null;
        }
        if (v.isHostObject()) {
            return v.asHostObject();
        }
        if (v.isBoolean() || v.isNumber() || v.isString()){
            return v.as(Object.class);
        }
        if (v.isInstant()) {
            return v.asInstant();
        }
        if (v.hasArrayElements()) {
            final int size = (int) v.getArraySize();
            final List<Object> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(materialize(v.getArrayElement(i)));
            }
            return list;
        }
        if (v.hasHashEntries()) {
            final Map<Object, Object> map = new HashMap<>();
            final Value keysIterator = v.getHashKeysIterator();
            while (keysIterator.hasIteratorNextElement()) {
                final Value polyglotKey = keysIterator.getIteratorNextElement();
                final Object key = materialize(polyglotKey);
                map.put(key, materialize(v.getHashValue(polyglotKey)));
            }
            return map;
        }

        throw new UnsupportedOperationException("Unsupported type " + v);
    }
}
