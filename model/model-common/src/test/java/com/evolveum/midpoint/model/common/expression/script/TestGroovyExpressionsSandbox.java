/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.common.expression.script;

import static org.testng.AssertJUnit.assertTrue;

import java.util.List;

import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.expression.ExpressionPermissionProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.ScriptExpressionProfile;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;

/**
 * @author Radovan Semancik
 */
public class TestGroovyExpressionsSandbox extends TestGroovyExpressions {


	@Override
	protected ScriptExpressionProfile getScriptExpressionProfile(String language) {
		ScriptExpressionProfile profile = new ScriptExpressionProfile(language);
		
		profile.setTypeChecking(true);
		
		ExpressionPermissionProfile permissionProfile = new ExpressionPermissionProfile(this.getClass().getSimpleName());
		profile.setPermissionProfile(permissionProfile);
		
		permissionProfile.addClassAccessRule(Poison.class, AccessDecision.ALLOW);
		permissionProfile.addClassAccessRule(Poison.class, "smell", AccessDecision.DENY);
		permissionProfile.addClassAccessRule(Poison.class, "drink", AccessDecision.DENY);
		
		permissionProfile.addClassAccessRule(String.class, AccessDecision.ALLOW);
		permissionProfile.addClassAccessRule(String.class, "execute", AccessDecision.DENY);
		
		permissionProfile.addClassAccessRule(List.class, AccessDecision.ALLOW);
		permissionProfile.addClassAccessRule(List.class, "execute", AccessDecision.DENY);
		
		permissionProfile.setDecision(AccessDecision.ALLOW);
		
		return profile;
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
		evaluateAndAssertStringScalarExpresssionRestricted(
				"expression-poinson-smell.xml",
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
		evaluateAndAssertStringScalarExpresssionRestricted(
				"expression-poinson-smell-tricky.xml",
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
		evaluateAndAssertStringScalarExpresssionRestricted(
				"expression-poinson-smell-dynamic.xml",
				"testSmellPoisonDynamic",
				createPoisonVariables(poison));
		
		poison.assertNotSmelled();
		
    }
	
	/**
	 * Attempt to smell poison by using a very dynamic invocation.
	 * 
	 * We are in type checking mode, therefore this just won't compile.
	 */
	@Test
	@Override
    public void testSmellPoisonVeryDynamic() throws Exception {
		Poison poison = new Poison();
		
		// WHEN
		try {
			evaluateAndAssertStringScalarExpresssion(
					"expression-poinson-smell-very-dynamic.xml",
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
		evaluateAndAssertStringScalarExpresssionRestricted(
				"expression-poinson-smell-reflection.xml",
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
		evaluateAndAssertStringScalarExpresssionRestricted(
				"expression-poinson-drink.xml",
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
		evaluateAndAssertStringScalarExpresssionRestricted(
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
		evaluateAndAssertStringScalarExpresssionRestricted(
				"expression-list-exec.xml",
				"testListExec",
				null);
		
		// THEN
		
    }

}
