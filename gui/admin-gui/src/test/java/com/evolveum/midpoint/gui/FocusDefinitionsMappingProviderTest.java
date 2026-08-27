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

import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.web.AbstractGuiUnitTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
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
        FocusDefinitionsMappingProvider provider = new TestFocusDefinitionsMappingProvider();

        List<String> lowerCaseMatches = provider.collectAvailableDefinitions("name", getResourceObjectType());
        assertNameMatches(lowerCaseMatches);

        List<String> mixedCaseMatches = provider.collectAvailableDefinitions("Name", getResourceObjectType());
        assertNameMatches(mixedCaseMatches);
    }

    /**
     * Verifies that unrelated input does not include name-related user properties.
     */
    @Test
    public void testCollectAvailableDefinitionsDoesNotReturnNameFieldsForUnrelatedInput() {
        FocusDefinitionsMappingProvider provider = new TestFocusDefinitionsMappingProvider();

        List<String> matches = provider.collectAvailableDefinitions("unrelatedInput", getResourceObjectType());

        assertFalse(matches.contains(UserType.F_NAME.getLocalPart()));
        assertFalse(matches.contains(UserType.F_FAMILY_NAME.getLocalPart()));
        assertFalse(matches.contains(UserType.F_GIVEN_NAME.getLocalPart()));
    }

    private void assertNameMatches(List<String> matches) {
        assertTrue(matches.contains(UserType.F_NAME.getLocalPart()));
        assertTrue(matches.contains(UserType.F_FAMILY_NAME.getLocalPart()));
        assertTrue(matches.contains(UserType.F_GIVEN_NAME.getLocalPart()));
    }

    private ResourceObjectTypeDefinitionType getResourceObjectType() {
        return new ResourceObjectTypeDefinitionType();
    }

    private PrismContainerDefinition<UserType> getUserObjectDefinition() {
        return getPrismContext()
                .getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class);
    }

    private class TestFocusDefinitionsMappingProvider extends FocusDefinitionsMappingProvider {

        private TestFocusDefinitionsMappingProvider() {
            super(null);
        }

        @Override
        protected PrismContainerDefinition<? extends Containerable> getFocusTypeDefinition(
                ResourceObjectTypeDefinitionType resourceObjectType) {
            return getUserObjectDefinition();
        }
    }
}
