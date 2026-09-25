/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.expression.ExpressionTestUtil;
import com.evolveum.midpoint.model.common.expression.script.velocity.SafeVelocityScriptExecutor;
import com.evolveum.midpoint.model.common.expression.script.velocity.VelocityScriptExecutor;
import com.evolveum.midpoint.prism.Safe;
import com.evolveum.midpoint.prism.crypto.Protector;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class TestSafeVelocityExpressions extends AbstractVelocityExpressionsTest {

    private static final String SAFE_LANGUAGE = MidPointConstants.EXPRESSION_LANGUAGE_SAFE_VELOCITY_URL;

    @Override
    protected ScriptExecutor createExecutor(PrismContext prismContext, Protector protector, Clock clock, boolean restrictedMode) {
        return new SafeVelocityScriptExecutor(
                prismContext,
                protector,
                localizationService,
                ExpressionTestUtil.testingExpressionsConfiguration(restrictedMode),
                ExpressionTestUtil.testingExpressionsConfigurationView());
    }

    @Override
    protected void processScriptBeanAfterParsing(ScriptExpressionEvaluatorType bean) {
        super.processScriptBeanAfterParsing(bean);
        bean.setLanguage(SAFE_LANGUAGE);
    }

    // ========================================================================================
    // Unsafe ("sandbox-escaping") expressions.
    //
    // The safe Velocity executor must prevent untrusted scripts from accessing midPoint
    // internals (e.g. prism context) or manipulating the JVM (class loaders, reflection,
    // arbitrary class loading, runtime execution, ...). The tests below try various escape
    // techniques. Each of them expects the attempt to be neutralized: either the evaluation
    // is rejected with an exception, or the output contains no sensitive data (e.g., in the default
    // non-strict Velocity mode an unresolved reference is just rendered as literal text,
    // which is harmless).
    // ========================================================================================

    /**
     * A midPoint-internal object (prism context) passed as a regular script variable
     * must not be available in the safe Velocity context.
     */
    @Test
    public void testUnsafePrismContextVariable() {
        assertUnsafeScriptNeutralized("$ctx", createVariables(
                "ctx", prismContext, PrismContext.class));
    }

    /**
     * The built-in service variables (prism context, localization service) must not be
     * available to safe Velocity scripts.
     */
    @Test
    public void testUnsafeServiceVariables() {
        assertUnsafeScriptNeutralized("$prismContext", createVariables());
        assertUnsafeScriptNeutralized("$localizationService", createVariables());
    }

    /**
     * Getting the Class of a midPoint object must not be possible.
     */
    @Test
    public void testUnsafeGetClass() {
        assertUnsafeScriptNeutralized("$user.getClass()", unsafeVariables());
    }

    /**
     * The "class" property is another way to get the Class of a midPoint object.
     */
    @Test
    public void testUnsafeGetPropertyClass() {
        assertUnsafeScriptNeutralized("$user.class", unsafeVariables());
    }

    /**
     * The class loader of a midPoint object must not be accessible from the script.
     */
    @Test
    public void testUnsafeClassloaderOfUserObject() {
        assertUnsafeScriptNeutralized("$user.getClass().getClassLoader()", unsafeVariables());
    }

    /**
     * The class loader of a PolyString (a midPoint class) must not be accessible from the script.
     */
    @Test
    public void testUnsafeClassloaderOfPolyString() {
        assertUnsafeScriptNeutralized("$ps.getClass().getClassLoader()", unsafeVariables());
    }

    /**
     * The class loader must not be accessible even through the (allowed) function library.
     */
    @Test
    public void testUnsafeClassloaderOfFunctionsLibrary() {
        assertUnsafeScriptNeutralized("$basic.getClass().getClassLoader()", createVariables());
    }

    /**
     * Loading midPoint classes through the class loader must not be possible.
     */
    @Test
    public void testUnsafeClassloaderLoadClass() {
        assertUnsafeScriptNeutralized(
                "$user.getClass().getClassLoader().loadClass(\"com.evolveum.midpoint.prism.PrismContext\")",
                unsafeVariables());
    }

    /**
     * Loading arbitrary (JDK) classes through {@code Class.forName} must not be possible.
     */
    @Test
    public void testUnsafeClassForName() {
        assertUnsafeScriptNeutralized(
                "$user.getClass().forName(\"java.lang.Runtime\")",
                unsafeVariables());
    }

    /**
     * Even a plain String variable must not provide a way to load arbitrary classes.
     */
    @Test
    public void testUnsafeForNameFromPlainString() {
        assertUnsafeScriptNeutralized("$foo.getClass().forName(\"java.lang.Runtime\")", unsafeVariables());
    }

    /**
     * Getting the protection domain (and thus the code source) of a midPoint class must not leak anything.
     */
    @Test
    public void testUnsafeGetProtectionDomain() {
        assertUnsafeScriptNeutralized("$user.getClass().getProtectionDomain()", unsafeVariables());
    }

    /**
     * Introspecting the public API of midPoint classes through reflection must not be possible.
     */
    @Test
    public void testUnsafeReflectionGetMethods() {
        assertUnsafeScriptNeutralized("$user.getClass().getMethods()", unsafeVariables());
    }

    /**
     * Introspecting the fields of midPoint classes through reflection must not be possible.
     */
    @Test
    public void testUnsafeReflectionGetDeclaredFields() {
        assertUnsafeScriptNeutralized("$user.getClass().getDeclaredFields()", unsafeVariables());
    }

    /**
     * Getting an instance of {@code java.lang.Runtime} through reflection must not be possible.
     * (Once the Runtime instance is reachable, arbitrary command execution is possible as well.)
     */
    @Test
    public void testUnsafeGetRuntimeInstance() {
        assertUnsafeScriptNeutralized(
                "$user.getClass().forName(\"java.lang.Runtime\").getMethod(\"getRuntime\").invoke(\"\")",
                unsafeVariables());
    }

    /**
     * Instantiating arbitrary objects through reflection must not be possible.
     * For example, a {@code ProcessBuilder} instance would allow arbitrary command execution via {@code command("...").start()}.
     */
    @Test
    public void testUnsafeArbitraryObjectInstantiation() {
        assertUnsafeScriptNeutralized(
                "$foo.getClass().forName(\"java.util.ArrayList\").newInstance()",
                unsafeVariables());
    }

    /**
     * Parsing (including) external templates must not be possible from a safe script.
     */
    @Test
    public void testUnsafeParseDirective() throws Exception {
        File file = new File("other.vm");
        try (var fw = new FileWriter(file)) {
            String marker = "test";
            fw.write(marker);
            fw.close();
            assertUnsafeScriptBlocked("#parse(\"other.vm\")", createVariables(), marker);
            assertUnsafeScriptBlocked("#include(\"other.vm\")", createVariables(), marker);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    /**
     * The #evaluate directive: the same sandbox restrictions apply to the evaluated text as to the original script.
     */
    @Test
    public void testUnsafeEvaluateDirective() {
        assertUnsafeScriptBlocked(
                "#evaluate(\"$user.getClass().getClassLoader()\")",
                unsafeVariables(),
                "AppClassLoader", "ClassLoader@");
    }

    @Test
    public void testUnsafeGettingForbidden() {
        assertUnsafeScriptNeutralized(
                "$user.asPrismObject()",
                unsafeVariables());
    }

    @Test
    public void testUnsafeSettingForbidden() {
        assertUnsafeScriptBlocked(
                """
                        #set($user.costCenter = "123456")
                        ${user.costCenter}7890
                        """,
                unsafeVariables(),
                "1234567890");
    }

    @Test
    public void testMapModificationsBlocked() {
        assertUnsafeScriptBlocked(
                """
                        #set($b = "BOOM")
                        before: $aMap.key
                        ${aMap.put("key", $b)}
                        after1: $aMap.key
                        #set($aMap.key = $b)
                        after2: $aMap.key
                        #set($aMap["key"] = $b)
                        after3: $aMap.key
                        """,
                unsafeVariables(),
                "BOOM");
    }

    @Test
    public void testGetFieldBlocked() {
        // This shouldn't work even in the full mode, but let's check anyway
        assertUnsafeScriptNeutralized("$fieldOnly.foo", unsafeVariables());
    }

    @Test
    public void testSetFieldBlocked() {
        // This shouldn't work even in the full mode, but let's check anyway
        assertUnsafeScriptBlocked(
                "#set($fieldOnly.foo = 'BOOM')#if($fieldOnly.foo == 'BOOM')BOOM#{else}SAFE#{end}",
                unsafeVariables(),
                "BOOM");
    }

    @Test
    public void testSetRecordFieldBlocked() {
        // This shouldn't work even in the full mode, but let's check anyway
        assertUnsafeScriptBlocked(
                "#set($myRecord.foo = 'BOOM')$myRecord.foo",
                unsafeVariables(),
                "BOOM");
    }

    /** Checks that access via get("property") is still blocked, unless the method is annotated with {@link Safe}. */
    @Test
    public void testGetViaDynamicAccessBlocked() {
        assertUnsafeScriptNeutralized("$dynamicAccess.foo", unsafeVariables());
    }

    /** Checks that access via set("property", value) is still blocked, unless the method is annotated with {@link Safe}. */
    @Test
    public void testSetViaDynamicAccessBlocked() {
        // This shouldn't work even in the full mode, but let's check anyway
        assertUnsafeScriptBlocked(
                "#set($dynamicAccess.foo = 'BOOM')$dynamicAccess.safeGetFoo()",
                unsafeVariables(),
                "BOOM");
    }

    // ========================================================================================
    // Helpers for the unsafe-expression tests above
    // ========================================================================================

    /**
     * Variables with various "allowed" types that a safe Velocity script may legitimately see.
     * They are used as the starting point for the escape attempts.
     */
    private VariablesMap unsafeVariables() {
        return createVariables(
                "user",
                MiscSchemaUtil.createObjectReference(USER_JACK_OID, UserType.COMPLEX_TYPE),
                prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class),

                "foo", "FOO", PrimitiveType.STRING,

                "ps", PolyString.fromOrig("secret"), PolyStringType.COMPLEX_TYPE,

                "list", List.of("a", "b", "c"), List.class,

                "aMap", new HashMap<>(Map.of("key", "value")), Map.class,

                "fieldOnly", new FieldOnly(), FieldOnly.class,

                "myRecord", new MyRecord(), MyRecord.class,

                "dynamicAccess", new DynamicAccess(), DynamicAccess.class
        );
    }

    @SuppressWarnings("WeakerAccess")
    @Safe // to allow it to go into the Velocity context
    public static class FieldOnly {
        /** Intentionally public field. */
        public String foo = "bar";
    }

    @Safe // to allow it to go into the Velocity context
    public record MyRecord(String foo) {

        public MyRecord() {
            this("bar");
        }

        @Override
        @Safe // to be able to call it from Velocity
        public String foo() {
            return foo;
        }
    }

    @SuppressWarnings({ "WeakerAccess", "unused" })
    @Safe // to allow it to go into the Velocity context
    public static final class DynamicAccess {

        private String foo = "bar";

        public String get(String name) {
            if ("foo".equals(name)) {
                return foo;
            } else {
                return null;
            }
        }

        public void put(String name, String value) {
            if ("foo".equals(name)) {
                foo = value;
            }
        }

        @Safe // to check if put is blocked even when get is blocked as well
        public String safeGetFoo() {
            return foo;
        }
    }

    private ScriptExpressionEvaluatorType createUnsafeScriptBean(String code) {
        ScriptExpressionEvaluatorType bean = new ScriptExpressionEvaluatorType();
        bean.setLanguage(SAFE_LANGUAGE);
        bean.setCode(code);
        return bean;
    }

    private String evaluateUnsafeScript(String code, VariablesMap variables) throws CommonException {
        List<PrismPropertyValue<String>> results = executeScript(
                createUnsafeScriptBean(code), DOMUtil.XSD_STRING, true, variables, getTestName(),
                createOperationResult());
        Collection<String> values = getPropertyValues(results);
        if (values.isEmpty()) {
            return null;
        }
        assertEquals("Unexpected number of results from unsafe script evaluation: " + values, 1, values.size());
        return values.iterator().next();
    }

    /**
     * Evaluates the given script as safe Velocity and asserts the safety by checking that:
     *
     * . either an exception was thrown during evaluation (the script was rejected),
     * . or the result is exactly the same as the input script (the reference was rejected to be not resolved, which is OK).
     */
    private void assertUnsafeScriptNeutralized(String template, VariablesMap variables) {
        String expanded;
        try {
            expanded = evaluateUnsafeScript(template, variables);
        } catch (CommonException e) {
            // The script was rejected - good
            assertExpectedException(e);
            return;
        }
        displayValue("Result after template expansion (to be checked if neutralized)", expanded);
        assertThat(expanded)
                .withFailMessage("Unsafe expression was not neutralized; reference was resolved: %s", expanded)
                .isEqualTo(template);
    }

    /**
     * Similar to {@link #assertUnsafeScriptNeutralized(String, VariablesMap)} but assumes that the output may be different
     * from the input template (e.g. if the script was partially evaluated), so we have to rely on "leak markers" to check that
     * the evaluation of the critical code did not take place.
     *
     * If possible, please use the above version. This one is weaker, as the leak markers may be incomplete or outdated.
     */
    private void assertUnsafeScriptBlocked(String code, VariablesMap variables, String... leakMarkers) {
        String result;
        try {
            result = evaluateUnsafeScript(code, variables);
        } catch (CommonException e) {
            // The script was rejected - good
            assertExpectedException(e);
            return;
        }
        displayValue("Result after template expansion (to be checked if neutralized)", result);
        for (String marker : leakMarkers) {
            assertThat(result)
                    .withFailMessage("Unsafe expression leaked '%s' in the result: %s", marker, result)
                    .doesNotContain(marker);
        }
    }

    /**
     * Gross check that the decisions regarding methods are not cached in the Velocity executor.
     * Executes a script with the full executor (which allows access to all methods)
     * and then executes the same script with the safe executor (which should neutralize the unsafe expression).
     */
    @Test
    public void test990SwitchingExecutors() throws CommonException {

        String unsafeTemplate = "$user.getClass()";

        given("executed script with the full executor");

        var safeExecutor = scriptExecutor;
        try {
            scriptFactory.replaceExecutor(createFullVelocityExecutor());
            var fullResult = executeScript(
                    new ScriptExpressionEvaluatorType()
                            .language(MidPointConstants.EXPRESSION_LANGUAGE_VELOCITY_URL)
                            .code(unsafeTemplate),
                    DOMUtil.XSD_STRING,
                    true,
                    unsafeVariables(),
                    getTestName(),
                    createOperationResult());

            assertThat(fullResult.get(0).getRealValue()).as("returned value").isEqualTo(UserType.class.toString());

            when("executed the same script with the safe executor -> will be neutralized (no caching in Velocity)");

            scriptFactory.replaceExecutor(safeExecutor);
            assertUnsafeScriptNeutralized(unsafeTemplate, unsafeVariables());

        } finally {
            scriptFactory.replaceExecutor(safeExecutor);
        }
    }

    private ScriptExecutor createFullVelocityExecutor() {
        return new VelocityScriptExecutor(
                prismContext,
                protector,
                localizationService,
                ExpressionTestUtil.testingExpressionsConfiguration(),
                ExpressionTestUtil.testingExpressionsConfigurationView());
    }
}
