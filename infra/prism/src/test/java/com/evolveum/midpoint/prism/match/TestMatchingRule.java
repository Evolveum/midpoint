/**
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.prism.match;

import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * @author semancik
 *
 */
public class TestMatchingRule {

	private static MatchingRuleRegistry matchingRuleRegistry;
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
		
		matchingRuleRegistry = MatchingRuleRegistryFactory.createRegistry();
	}
	
	@Test
	public void testStringDefault() throws Exception {
		// GIVEN
		MatchingRule<String> rule = matchingRuleRegistry.getMatchingRule(null, DOMUtil.XSD_STRING);
		// WHEN, THEN		
		assertMatch(rule, "foo", "foo");
		assertNoMatch(rule, "foo", "bar");
		assertNoMatch(rule, "foo", "Foo");
		assertNoMatch(rule, "FOO", "Foo");
		assertNormalized(rule, "Foo", "Foo");
		assertNormalized(rule, "baR", "baR");
	}
	
	@Test
	public void testStringCaseInsensitive() throws Exception {
		// GIVEN
		MatchingRule<String> rule = matchingRuleRegistry.getMatchingRule(StringIgnoreCaseMatchingRule.NAME, 
				DOMUtil.XSD_STRING);
		// WHEN, THEN		
		assertMatch(rule, "foo", "foo");
		assertNoMatch(rule, "foo", "bar");
		assertMatch(rule, "foo", "Foo");
		assertMatch(rule, "FOO", "Foo");
		assertNormalized(rule, "bar", "baR");
		assertNormalized(rule, "foo", "FoO");
		assertNormalized(rule, "foobar", "foobar");
	}
	
	@Test
	public void testPolyStringStrict() throws Exception {
		// GIVEN
		MatchingRule<PolyString> rule = matchingRuleRegistry.getMatchingRule(PolyStringStrictMatchingRule.NAME, 
				PolyStringType.COMPLEX_TYPE);
		// WHEN, THEN		
		assertMatch(rule, new PolyString("Bar", "bar"), new PolyString("Bar", "bar"));
		assertNoMatch(rule, new PolyString("BAR", "bar"), new PolyString("Foo", "bar"));
		assertNoMatch(rule, new PolyString("Bar", "bar"), new PolyString("bAR", "bar"));
		assertNoMatch(rule, new PolyString("Bar", "bar"), new PolyString("Bar", "barbar"));
	}
	
	@Test
	public void testPolyStringOrig() throws Exception {
		// GIVEN
		MatchingRule<PolyString> rule = matchingRuleRegistry.getMatchingRule(PolyStringOrigMatchingRule.NAME, 
				PolyStringType.COMPLEX_TYPE);
		// WHEN, THEN		
		assertMatch(rule, new PolyString("Bar", "bar"), new PolyString("Bar", "bar"));
		assertNoMatch(rule, new PolyString("BAR", "bar"), new PolyString("Foo", "bar"));
		assertNoMatch(rule, new PolyString("Bar", "bar"), new PolyString("bAR", "bar"));
		assertMatch(rule, new PolyString("Bar", "bar"), new PolyString("Bar", "barbar"));
	}
	
	@Test
	public void testPolyStringNorm() throws Exception {
		// GIVEN
		MatchingRule<PolyString> rule = matchingRuleRegistry.getMatchingRule(PolyStringNormMatchingRule.NAME, 
				PolyStringType.COMPLEX_TYPE);
		// WHEN, THEN		
		assertMatch(rule, new PolyString("Bar", "bar"), new PolyString("Bar", "bar"));
		assertMatch(rule, new PolyString("BAR", "bar"), new PolyString("Foo", "bar"));
		assertMatch(rule, new PolyString("Bar", "bar"), new PolyString("bAR", "bar"));
		assertNoMatch(rule, new PolyString("Bar", "bar"), new PolyString("Bar", "barbar"));
	}

	private <T> void assertMatch(MatchingRule<T> rule, T a, T b) {
		assertTrue("Values '"+a+"' and '"+b+"' does not match; rule: "+rule, rule.match(a, b));
	}

	private <T> void assertNoMatch(MatchingRule<T> rule, T a, T b) {
		assertFalse("Values '"+a+"' and '"+b+"' DOES match but they should not; rule: "+rule, rule.match(a, b));
	}

	private void assertNormalized(MatchingRule<String> rule, String expected, String original) {
		assertEquals("Normalized value does not match", expected, rule.normalize(original));
	}
}
