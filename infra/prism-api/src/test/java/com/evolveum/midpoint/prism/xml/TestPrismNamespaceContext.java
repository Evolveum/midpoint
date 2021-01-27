package com.evolveum.midpoint.prism.xml;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismNamespaceContext;
import com.evolveum.midpoint.prism.PrismNamespaceContext.PrefixPreference;
import com.google.common.collect.ImmutableMap;

import static com.evolveum.midpoint.prism.PrismConstants.PREFIX_NS_TYPES;
import static com.evolveum.midpoint.prism.PrismConstants.PREFIX_NS_QUERY;
import static com.evolveum.midpoint.prism.PrismConstants.NS_QUERY;
import static com.evolveum.midpoint.prism.PrismConstants.NS_TYPES;

public class TestPrismNamespaceContext {

    private static final String NS_EXAMPLE = "https://example.com";

    private static final Map<String, String> GLOBAL_PREFIXES = ImmutableMap.<String, String>builder()
            .put(PREFIX_NS_QUERY, NS_QUERY)
            .put(PREFIX_NS_TYPES, NS_TYPES)
            .build();

    private static final PrismNamespaceContext GLOBAL = PrismNamespaceContext.from(GLOBAL_PREFIXES);

    private static final Map<String, String> NS_EXAMPLE_DEFAULT = ImmutableMap.of("", NS_EXAMPLE);
    private static final Map<String, String> NS_QUERY_DEFAULT = ImmutableMap.of("", NS_QUERY);

    @Test
    public void testLookupByPrefix() {
        assertRootPrefixesToNamespace(GLOBAL);

        assertTrue(GLOBAL.get("nonExistant").isLocalEmpty());

        PrismNamespaceContext derivedNs = GLOBAL.childContext(ImmutableMap.of("", NS_EXAMPLE));
        assertRootPrefixesToNamespace(derivedNs);
        assertEquals(derivedNs.get("").get(), NS_EXAMPLE);
        assertEquals(derivedNs.get(null).get(), NS_EXAMPLE);

        PrismNamespaceContext overridenNs = derivedNs.childContext(ImmutableMap.of("", NS_QUERY));

        assertRootPrefixesToNamespace(overridenNs);
        assertEquals(overridenNs.get("").get(), NS_QUERY);

    }

    @Test
    public void testLookupByNamespace() {
        assertRootNamespaceToPrefix(GLOBAL, PrefixPreference.GLOBAL_FIRST);
        assertRootNamespaceToPrefix(GLOBAL, PrefixPreference.LOCAL_FIRST);
        PrismNamespaceContext exampleDefault = GLOBAL.childContext(NS_EXAMPLE_DEFAULT);
        assertEquals(exampleDefault.prefixFor(NS_EXAMPLE).get(), "");
        assertRootNamespaceToPrefix(exampleDefault, PrefixPreference.GLOBAL_FIRST);
        assertRootNamespaceToPrefix(exampleDefault, PrefixPreference.LOCAL_FIRST);


        PrismNamespaceContext queryDefault = exampleDefault.childContext(NS_QUERY_DEFAULT);
        assertTrue(queryDefault.prefixFor(NS_EXAMPLE, PrefixPreference.LOCAL_FIRST).isEmpty(), "No prefix is should be avaialble to NS_EXAMPLE");
        assertTrue(queryDefault.prefixFor(NS_EXAMPLE, PrefixPreference.GLOBAL_FIRST).isEmpty(), "No prefix should be available to NS_EXAMPLE");

        assertEquals(queryDefault.prefixFor(NS_QUERY, PrefixPreference.LOCAL_FIRST).get(), "");
        assertEquals(queryDefault.prefixFor(NS_QUERY, PrefixPreference.GLOBAL_FIRST).get(), PREFIX_NS_QUERY);
        assertRootNamespaceToPrefix(queryDefault, PrefixPreference.GLOBAL_FIRST);

    }


    @Test
    public void testMemoryOptimizations() {
        PrismNamespaceContext emptyInherited = GLOBAL.childContext(Collections.emptyMap());
        assertRootPrefixesToNamespace(emptyInherited);

        assertSame(GLOBAL.childContext(Collections.emptyMap()), emptyInherited, "Multiple calls to childContext(empty) should return same instance.");
        assertSame(emptyInherited.inherited(), emptyInherited, "Inheritance of inherited should return itself");
        assertSame(emptyInherited.childContext(Collections.emptyMap()), emptyInherited, "Inheritance of inherited should return itself");
    }

    private void assertRootPrefixesToNamespace(PrismNamespaceContext globalNs) {
        assertEquals(globalNs.get(PREFIX_NS_QUERY).get(), PrismConstants.NS_QUERY);
        assertEquals(globalNs.get(PREFIX_NS_TYPES).get(), PrismConstants.NS_TYPES);
    }


    private void assertRootNamespaceToPrefix(PrismNamespaceContext ctx, PrefixPreference pref) {
        assertEquals(ctx.prefixFor(NS_QUERY, pref).get(),PREFIX_NS_QUERY);
        assertEquals(ctx.prefixFor(NS_TYPES, pref).get(),PREFIX_NS_TYPES);
    }
}
