/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.match;

import static org.testng.AssertJUnit.*;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.AbstractPrismTest;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.impl.match.MatchingRuleRegistryFactory;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 */
public class TestMatchingRule extends AbstractPrismTest {

    private static MatchingRuleRegistry matchingRuleRegistry =
            MatchingRuleRegistryFactory.createRegistry();

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
        MatchingRule<String> rule = matchingRuleRegistry.getMatchingRule(PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME,
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
        MatchingRule<PolyString> rule = matchingRuleRegistry.getMatchingRule(PrismConstants.POLY_STRING_STRICT_MATCHING_RULE_NAME,
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
        MatchingRule<PolyString> rule = matchingRuleRegistry.getMatchingRule(PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME,
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
        MatchingRule<PolyString> rule = matchingRuleRegistry.getMatchingRule(PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME,
                PolyStringType.COMPLEX_TYPE);
        // WHEN, THEN
        assertMatch(rule, new PolyString("Bar", "bar"), new PolyString("Bar", "bar"));
        assertMatch(rule, new PolyString("BAR", "bar"), new PolyString("Foo", "bar"));
        assertMatch(rule, new PolyString("Bar", "bar"), new PolyString("bAR", "bar"));
        assertNoMatch(rule, new PolyString("Bar", "bar"), new PolyString("Bar", "barbar"));
    }

    @Test
    public void testXml() throws Exception {
        // GIVEN
        MatchingRule<String> rule = matchingRuleRegistry.getMatchingRule(PrismConstants.XML_MATCHING_RULE_NAME,
                DOMUtil.XSD_STRING);
        // WHEN, THEN
        assertMatch(rule, "<foo>BAR</foo>", "<foo>BAR</foo>");
        assertNoMatch(rule, "<foo>BAR</foo>", "<foo>BARbar</foo>");
        assertMatch(rule, "<foo>BAR</foo>", "  <foo>BAR</foo>  ");
        assertMatch(rule, "<foo>\n  BAR\n</foo>", "  <foo>BAR</foo>  ");

        assertMatch(rule, "<foo>FOO<bar>BAR</bar></foo>", "<foo>FOO<bar>BAR</bar></foo>");
        assertNoMatch(rule, "<foo>FOO<bar>BAR</bar></foo>", "<foo>FOO<baZ>BAR</baZ></foo>");
        assertNoMatch(rule, "<foo>FOO<bar>BAR</bar></foo>", "<foo><bar>BAR</bar></foo>");
        assertMatch(rule, "<foo>FOO<bar>BAR</bar></foo>", "<foo>\n  FOO\n   <bar>BAR</bar>\n</foo>\n");
        assertMatch(rule, "<foo>FOO<bar></bar></foo>", " <foo>  FOO  <bar/> </foo> ");
        assertMatch(rule, "\n\n <foo>  \n   FOO  <bar>  </bar> \n  </foo>", " <foo>  FOO <bar/></foo> ");
        assertNoMatch(rule, "\n\n <foo>  \n   FOO  <bar>   X </bar> \n  </foo>", " <foo>  FOO <bar/></foo> ");
        assertMatch(rule, "<foo>FOO<bar>BAR</bar></foo>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<foo>\n  FOO\n   <bar>BAR</bar>\n</foo>\n");
        assertMatch(rule, "<foo>  \n <!-- dada -->   FOO  <bar>  </bar> \n  </foo>", "<!-- bubu --> <foo>  FOO  <!-- he -->   <bar/><!-- hihi --></foo> ");
        assertMatch(rule, "<foo>FOO  <bar/> \n  </foo>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!-- blahblah ... AS IS ... blah -->\n<foo>  FOO  <!-- he -->   <bar/><!-- hihi --></foo> ");

        // Invalid XML
        assertMatch(rule, "<foo>FOO<bar>BAR</foo>", "<foo>FOO<bar>BAR</foo>");
        assertNoMatch(rule, "<foo>FOO<bar>BAR</foo>", "<foo>FOO<bar>BAR</bar></foo>");

        // normalization
        assertNormalized(rule, "<foo>BAR</foo>", "<foo>BAR</foo>");
        assertNormalized(rule, "<foo>BAR</foo>", " <foo>   BAR   </foo>  ");
        assertNormalized(rule, "<foo>BAR</foo>", "<foo>\n  BAR\n</foo>");
        assertNormalized(rule, "<foo>FOO<bar/></foo>", "\n\n <foo>  \n   FOO  <bar>  </bar> \n  </foo>");
        assertNormalized(rule, "<foo>FOOfoo<bar/></foo>", " <foo>  FOOfoo <bar/></foo> ");
        assertNormalized(rule, "<foo>FOO<bar/></foo>", "\n\n <foo>  \n   FOO  <bar>  </bar> \n  </foo>");
        assertNormalized(rule, "<foo>FOO<bar/></foo>", " <foo>  FOO <bar/></foo> ");
        assertNormalized(rule, "<foo>FOO<bar/></foo>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <foo>  FOO <bar/></foo> ");
        assertNormalized(rule, "<foo>FOO<bar/></foo>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <!-- bubu --> "
                + "<foo>  FOO <!-- hehe --> <bar/> <!-- hah! --> </foo> ");

        // Invalid XML
        assertNormalized(rule, "<foo>FOO<bar> BAR </foo>", "<foo>FOO<bar> BAR </foo>   ");
    }

    private <T> void assertMatch(MatchingRule<T> rule, T a, T b) throws SchemaException {
        assertTrue("Values '" + a + "' and '" + b + "' does not match; rule: " + rule, rule.match(a, b));
    }

    private <T> void assertNoMatch(MatchingRule<T> rule, T a, T b) throws SchemaException {
        assertFalse("Values '" + a + "' and '" + b + "' DOES match but they should not; rule: " + rule, rule.match(a, b));
    }

    private void assertNormalized(MatchingRule<String> rule, String expected, String original) throws SchemaException {
        assertEquals("Normalized value does not match", expected, rule.normalize(original));
    }
}
