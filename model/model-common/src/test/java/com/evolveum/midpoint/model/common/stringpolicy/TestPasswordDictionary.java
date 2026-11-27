/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/** Tests the built-in dictionary of common passwords and the service that combines dictionaries. */
public class TestPasswordDictionary {

    private static final LimitationsType ENABLED = new LimitationsType().checkAgainstDictionary(true);
    private static final LimitationsType DISABLED = new LimitationsType().checkAgainstDictionary(false);
    private static final LimitationsType UNSET = new LimitationsType();

    private final PasswordDictionaryService service =
            new PasswordDictionaryService(List.of(new BundledPasswordDictionary()));

    @BeforeClass
    public void initPrismContext() throws Exception {
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testCommonPasswordIsNotValid() {
        assertFalse(service.isValid("password", ENABLED));
        assertFalse(service.isValid("12345678", ENABLED));
    }

    @Test
    public void testCaseIsIgnored() {
        assertFalse(service.isValid("Password1", ENABLED));
        assertFalse(service.isValid("PASSWORD1", ENABLED));
    }

    @Test
    public void testOtherPasswordIsValid() {
        assertTrue(service.isValid("Zk7mQx2pLw", ENABLED));
        assertTrue(service.isValid("password1-extended", ENABLED));
    }

    @Test
    public void testDictionaryIsUsedOnlyWhenEnabled() throws ConfigurationException {
        assertFalse(policy(ENABLED).isValidAgainstDictionaries("Password1"));
        assertTrue(policy(DISABLED).isValidAgainstDictionaries("Password1"));
        assertTrue(policy(UNSET).isValidAgainstDictionaries("Password1"));
    }

    private StringPolicy policy(LimitationsType limitations) throws ConfigurationException {
        ValuePolicyType valuePolicy = new ValuePolicyType().stringPolicy(new StringPolicyType().limitations(limitations));
        return StringPolicy.compile(ConfigurationItem.embedded(valuePolicy.getStringPolicy()), service);
    }

    @Test
    public void testServiceRejectsValueRejectedByAnyDictionary() {
        PasswordDictionary company = new FixedPasswordDictionary("companypassword");
        var combined = new PasswordDictionaryService(List.of(new BundledPasswordDictionary(), company));

        assertFalse(combined.isValid("Password1", ENABLED));
        assertFalse(combined.isValid("companypassword", ENABLED));
        assertTrue(combined.isValid("Zk7mQx2pLw", ENABLED));
    }

    @Test
    public void testServiceWithoutDictionariesAcceptsEverything() {
        var empty = new PasswordDictionaryService(List.of());

        assertTrue(empty.isValid("Password1", ENABLED));
    }
}
