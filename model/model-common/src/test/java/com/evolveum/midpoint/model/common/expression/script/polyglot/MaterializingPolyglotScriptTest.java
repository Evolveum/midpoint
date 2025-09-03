package com.evolveum.midpoint.model.common.expression.script.polyglot;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MaterializingPolyglotScriptTest {

    @Test
    void scriptConcatTwoParameters_evaluateScriptWithTwoParameters_concatenatedParametersShouldBeReturned() {
        final PolyglotScript script;
        try (final Context ctx = Context.create()) {

            script = new MaterializingPolyglotScript(ctx.parse("js", "param1 + param2"));
            final Object result = script.evaluate(Map.of("param1", "Hello", "param2", " World"));

            Assert.assertEquals(result, "Hello World");
        }
    }

    @Test
    void scriptSumsTwoParameters_evaluateScriptWithTwoParameters_sumOfParametersShouldBeReturned() {
        final PolyglotScript script;
        try (final Context ctx = Context.create()) {

            script = new MaterializingPolyglotScript(ctx.parse("js", "param1 + param2"));
            final Object result = script.evaluate(Map.of("param1", 1, "param2", 2));

            Assert.assertEquals(result, 3);
        }
    }

    @Test
    void scriptSplitsWordToChars_evaluateScriptWithOneParam_arrayWithCharactersShouldBeReturned() {
        final PolyglotScript script;
        final Object result;
        try (final Context ctx = Context.create()) {

            script = new MaterializingPolyglotScript(ctx.parse("js", "Array.from(param1)"));
            result = script.evaluate(Map.of("param1", "Hello"));
        }
        Assert.assertTrue(result instanceof List);
        Assert.assertEquals(result, List.of("H", "e", "l", "l", "o"));
    }

    @Test
    void scriptMapsParamsToMap_evaluateScriptWithTwoParams_mapWithParamsShouldBeReturned() {
        final PolyglotScript script;
        final Object result;
        final Map<String, Object> paramsMap = Map.of("param1", "Hello", "param2", "World");
        try (final Context ctx = Context.create()) {

            script = new MaterializingPolyglotScript(ctx.parse("js", "new Map([['param1', param1], ['param2', param2]])"));
            result = script.evaluate(paramsMap);
        }
        Assert.assertTrue(result instanceof Map);
        Assert.assertEquals(result, paramsMap);
    }

    @Test
    void scriptReturnsJavaType_evaluateScript_javaObjectShouldBeReturned() {
        final PolyglotScript script;
        final Object result;
        try (final Context ctx = Context.newBuilder()
                .allowHostClassLookup("java.util.HashMap"::equals)
                .allowHostAccess(HostAccess.ALL)
                .build()) {

            script = new MaterializingPolyglotScript(ctx.parse("js", """
                    JavaMap = Java.type('java.util.HashMap')
                    mapInstance = new JavaMap();
                    mapInstance.put('key', 'value')
                    mapInstance
                    """));
            result = script.evaluate(Collections.emptyMap());
        }
        Assert.assertTrue(result instanceof Map);
        Assert.assertEquals(result, Map.of("key", "value"));
    }

    @Test
    void scriptReturnsNestedMap_evaluateScript_nestedMapShouldBeReturned() {
        final PolyglotScript script;
        final Object result;
        try (final Context ctx = Context.create()) {
            script = new MaterializingPolyglotScript(ctx.parse("js", """
                    outerMap = new Map()
                    outerMap.set('innerMap', new Map([['key', 'value']]))
                    outerMap
                    """));
            result = script.evaluate(Collections.emptyMap());
        }
        Assert.assertTrue(result instanceof Map);
        Assert.assertEquals(result, Map.of("innerMap", Map.of("key", "value")));
    }

    @Test
    void scriptReturnsDate_evaluateScript_dateIsRepresentedAsInstant() {
        final PolyglotScript script;
        final Object result;
        try (final Context ctx = Context.create()) {
            script = new MaterializingPolyglotScript(ctx.parse("js", """
                    new Date(Date.UTC(2222, 0, 1, 1, 11, 11)) // months are zero based so 0 means January
                    """));
            result = script.evaluate(Collections.emptyMap());
        }
        Assert.assertTrue(result instanceof Instant);
        Assert.assertEquals(result, Instant.parse("2222-01-01T01:11:11Z"));
    }
}