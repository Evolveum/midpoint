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
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.expression.ExpressionTestUtil;
import com.evolveum.midpoint.model.common.expression.script.velocity.SafeVelocityScriptExecutor;
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
                prismContext, protector, localizationService, ExpressionTestUtil.testingExpressionsConfiguration(restrictedMode));
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
    public void testUnsafePrismContextVariable() throws Exception {
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
    public void testUnsafeGetPropertyClass() throws Exception {
        assertUnsafeScriptNeutralized("$user.class", unsafeVariables());
    }

    /**
     * The class loader of a midPoint object must not be accessible from the script.
     */
    @Test
    public void testUnsafeClassloaderOfUserObject() throws Exception {
        assertUnsafeScriptNeutralized("$user.getClass().getClassLoader()", unsafeVariables());
    }

    /**
     * The class loader of a PolyString (a midPoint class) must not be accessible from the script.
     */
    @Test
    public void testUnsafeClassloaderOfPolyString() throws Exception {
        assertUnsafeScriptNeutralized("$ps.getClass().getClassLoader()", unsafeVariables());
    }

    /**
     * The class loader must not be accessible even through the (allowed) function library.
     */
    @Test
    public void testUnsafeClassloaderOfFunctionsLibrary() throws Exception {
        assertUnsafeScriptNeutralized("$basic.getClass().getClassLoader()", createVariables());
    }

    /**
     * Loading midPoint classes through the class loader must not be possible.
     */
    @Test
    public void testUnsafeClassloaderLoadClass() throws Exception {
        assertUnsafeScriptNeutralized(
                "$user.getClass().getClassLoader().loadClass(\"com.evolveum.midpoint.prism.PrismContext\")",
                unsafeVariables());
    }

    /**
     * Loading arbitrary (JDK) classes through {@code Class.forName} must not be possible.
     */
    @Test
    public void testUnsafeClassForName() throws Exception {
        assertUnsafeScriptNeutralized(
                "$user.getClass().forName(\"java.lang.Runtime\")",
                unsafeVariables());
    }

    /**
     * Even a plain String variable must not provide a way to load arbitrary classes.
     */
    @Test
    public void testUnsafeForNameFromPlainString() throws Exception {
        assertUnsafeScriptNeutralized("$foo.getClass().forName(\"java.lang.Runtime\")", unsafeVariables());
    }

    /**
     * Getting the protection domain (and thus the code source) of a midPoint class must not leak anything.
     */
    @Test
    public void testUnsafeGetProtectionDomain() throws Exception {
        assertUnsafeScriptNeutralized("$user.getClass().getProtectionDomain()", unsafeVariables());
    }

    /**
     * Introspecting the public API of midPoint classes through reflection must not be possible.
     */
    @Test
    public void testUnsafeReflectionGetMethods() throws Exception {
        assertUnsafeScriptNeutralized("$user.getClass().getMethods()", unsafeVariables());
    }

    /**
     * Introspecting the fields of midPoint classes through reflection must not be possible.
     */
    @Test
    public void testUnsafeReflectionGetDeclaredFields() throws Exception {
        assertUnsafeScriptNeutralized("$user.getClass().getDeclaredFields()", unsafeVariables());
    }

    /**
     * Getting an instance of {@code java.lang.Runtime} through reflection must not be possible.
     * (Once the Runtime instance is reachable, arbitrary command execution is possible as well.)
     */
    @Test
    public void testUnsafeGetRuntimeInstance() throws Exception {
        assertUnsafeScriptNeutralized(
                "$user.getClass().forName(\"java.lang.Runtime\").getMethod(\"getRuntime\").invoke(\"\")",
                unsafeVariables());
    }

    /**
     * Instantiating arbitrary objects through reflection must not be possible.
     * For example, a {@code ProcessBuilder} instance would allow arbitrary command execution via {@code command("...").start()}.
     */
    @Test
    public void testUnsafeArbitraryObjectInstantiation() throws Exception {
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
    public void testUnsafeEvaluateDirective() throws Exception {
        assertUnsafeScriptBlocked(
                "#evaluate(\"$user.getClass().getClassLoader()\")",
                unsafeVariables(),
                "AppClassLoader", "ClassLoader@");
    }

    @Test
    public void testUnsafeGettingForbidden() throws Exception {
        assertUnsafeScriptNeutralized(
                "$user.asPrismObject()",
                unsafeVariables());
    }

    @Test
    public void testUnsafeSettingForbidden() throws Exception {
        assertUnsafeScriptBlocked(
                """
                        #set($user.costCenter = "123456")
                        ${user.costCenter}7890
                        """,
                unsafeVariables(),
                "1234567890");
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

                "list", List.of("a", "b", "c"), List.class);
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
    private void assertUnsafeScriptBlocked(String code, VariablesMap variables, String... leakMarkers)
            throws CommonException {
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
}
