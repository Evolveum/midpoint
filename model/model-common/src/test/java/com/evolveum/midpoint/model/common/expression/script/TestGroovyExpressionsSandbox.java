/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import static org.testng.AssertJUnit.assertTrue;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.expression.ExpressionPermissionProfile;
import com.evolveum.midpoint.schema.expression.ScriptExpressionProfile;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;

/**
 * @author Radovan Semancik
 */
public class TestGroovyExpressionsSandbox extends TestGroovyExpressions {

    @Override
    protected ScriptExpressionProfile createScriptExpressionProfile(@NotNull String language) {

        ExpressionPermissionProfile permissionProfile =
                ExpressionPermissionProfile.open(
                        this.getClass().getSimpleName(), AccessDecision.ALLOW);

        permissionProfile.addClassAccessRule(Poison.class, AccessDecision.ALLOW);
        permissionProfile.addClassAccessRule(Poison.class, "smell", AccessDecision.DENY);
        permissionProfile.addClassAccessRule(Poison.class, "drink", AccessDecision.DENY);

        permissionProfile.addClassAccessRule(String.class, AccessDecision.ALLOW);
        permissionProfile.addClassAccessRule(String.class, "execute", AccessDecision.DENY);

        permissionProfile.addClassAccessRule(List.class, AccessDecision.ALLOW);
        permissionProfile.addClassAccessRule(List.class, "execute", AccessDecision.DENY);

        return new ScriptExpressionProfile(language, AccessDecision.DEFAULT, true, permissionProfile);
    }

    /**
     * This should NOT pass here. There restrictions should not allow to invoke
     * poison.smell()
     */
    @Test
    @Override
    public void testSmellPoison() throws Exception {
        Poison poison = new Poison();

        // WHEN
        evaluateAndAssertStringScalarExpressionRestricted(
                "expression-poison-smell.xml",
                "testSmellPoison",
                createPoisonVariables(poison));

        // THEN
        poison.assertNotSmelled();
    }

    /**
     * Smelling poison should be forbidden even if the script
     * tries to obfuscate that a bit.
     */
    @Test
    @Override
    public void testSmellPoisonTricky() throws Exception {
        Poison poison = new Poison();

        // WHEN
        evaluateAndAssertStringScalarExpressionRestricted(
                "expression-poison-smell-tricky.xml",
                "testDrinkPoisonTricky",
                createPoisonVariables(poison));

        poison.assertNotSmelled();
    }

    /**
     * Attempt to smell poison by using a dynamic invocation.
     */
    @Test
    @Override
    public void testSmellPoisonDynamic() throws Exception {
        Poison poison = new Poison();

        // WHEN
        evaluateAndAssertStringScalarExpressionRestricted(
                "expression-poison-smell-dynamic.xml",
                "testSmellPoisonDynamic",
                createPoisonVariables(poison));

        poison.assertNotSmelled();

    }

    /**
     * Attempt to smell poison by using a very dynamic invocation.
     *
     * We are in type checking mode, therefore this just won't compile.
     */
    @Test(enabled=false)
    @Override
    public void testSmellPoisonVeryDynamic() throws Exception {
        Poison poison = new Poison();

        // WHEN
        try {
            evaluateAndAssertStringScalarExpression(
                    "expression-poison-smell-very-dynamic.xml",
                    "testSmellPoisonVeryDynamic",
                    createPoisonVariables(poison),
                    RESULT_POISON_OK);

        } catch (ExpressionEvaluationException e) {
            // THEN
            assertTrue("Unexpected exception message" + e.getMessage(), e.getMessage().contains("cannot resolve dynamic method name at compile time"));
        }

        poison.assertNotSmelled();
    }

    /**
     * Attempt to smell poison by using reflection
     */
    @Test
    @Override
    public void testSmellPoisonReflection() throws Exception {
        Poison poison = new Poison();

        // WHEN
        evaluateAndAssertStringScalarExpressionRestricted(
                "expression-poison-smell-reflection.xml",
                "testSmellPoisonDynamic",
                createPoisonVariables(poison));

        poison.assertNotSmelled();

    }

    /**
     * Drinking poison should be forbidden here.
     */
    @Test
    @Override
    public void testDrinkPoison() throws Exception {
        Poison poison = new Poison();

        // WHEN
        evaluateAndAssertStringScalarExpressionRestricted(
                "expression-poison-drink.xml",
                "testDrinkPoison",
                createPoisonVariables(poison));

    }

    /**
     * Deny execute from string.
     */
    @Test
    @Override
    public void testStringExec() throws Exception {

        // WHEN
        evaluateAndAssertStringScalarExpressionRestricted(
                "expression-string-exec.xml",
                "testStringExec",
                null);

        // THEN

    }

    /**
     * Deny execute from list.
     */
    @Test
    @Override
    public void testListExec() throws Exception {

        // WHEN
        evaluateAndAssertStringScalarExpressionRestricted(
                "expression-list-exec.xml",
                "testListExec",
                null);

        // THEN

    }

}
