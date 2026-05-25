/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.List;

import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.web.AbstractGuiUnitTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests autocomplete matching for focus object item paths used in mapping source/target selectors.
 */
public class FocusDefinitionsMappingProviderTest extends AbstractGuiUnitTest {

    /**
     * Verifies that filtering matches item local-name substrings regardless of character case.
     */
    @Test
    public void testCollectAvailableDefinitionsMatchesSubstringIgnoreCase() {
        FocusDefinitionsMappingProvider provider = new FocusDefinitionsMappingProvider(null);

        List<String> lowerCaseMatches = provider.collectAvailableDefinitions("name", getUserObjectDefinition());
        assertNameMatches(lowerCaseMatches);

        List<String> mixedCaseMatches = provider.collectAvailableDefinitions("Name", getUserObjectDefinition());
        assertNameMatches(mixedCaseMatches);
    }

    /**
     * Verifies that unrelated input does not include name-related user properties.
     */
    @Test
    public void testCollectAvailableDefinitionsDoesNotReturnNameFieldsForUnrelatedInput() {
        FocusDefinitionsMappingProvider provider = new FocusDefinitionsMappingProvider(null);

        List<String> matches = provider.collectAvailableDefinitions("unrelatedInput", getUserObjectDefinition());

        assertFalse(matches.contains(UserType.F_NAME.getLocalPart()));
        assertFalse(matches.contains(UserType.F_FAMILY_NAME.getLocalPart()));
        assertFalse(matches.contains(UserType.F_GIVEN_NAME.getLocalPart()));
    }

    private void assertNameMatches(List<String> matches) {
        assertTrue(matches.contains(UserType.F_NAME.getLocalPart()));
        assertTrue(matches.contains(UserType.F_FAMILY_NAME.getLocalPart()));
        assertTrue(matches.contains(UserType.F_GIVEN_NAME.getLocalPart()));
    }

    private PrismContainerDefinition<UserType> getUserObjectDefinition() {
        return getPrismContext()
                .getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class);
    }

}
