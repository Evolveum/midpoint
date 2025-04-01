/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.notifications.impl.formatters.ValueFormatter;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-notifications-test.xml" })
public class TestTextFormatter extends AbstractSpringTest {

    private static final String OBJECTS_DIR_NAME = "src/test/resources/objects";
    private static final String USER_JACK_FILE = OBJECTS_DIR_NAME + "/user-jack.xml";
    private static final String ACCOUNT_JACK_FILE = OBJECTS_DIR_NAME + "/account-jack.xml";

    private static final List<ItemPath> AUXILIARY_PATHS = Arrays.asList(
            UserType.F_FAMILY_NAME, // for testing purposes
            ShadowType.F_METADATA,
            InfraItemName.METADATA,
            ItemPath.create(ShadowType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),
            ItemPath.create(ShadowType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
            ItemPath.create(ShadowType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
            ItemPath.create(ShadowType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
            ItemPath.create(ShadowType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
            ItemPath.create(ShadowType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
            ShadowType.F_ITERATION,
            ShadowType.F_ITERATION_TOKEN,
            UserType.F_LINK_REF,
            ShadowType.F_TRIGGER
    );

    @Autowired private TextFormatter textFormatter;
    @Autowired private ValueFormatter valueFormatter;
    @Autowired private PrismContext prismContext;

    static {
        // We set the locale to US to avoid translation of item names.
        // It is crucial that this method is called before TextFormatter class is loaded.
        // Currently this solution suffices but it is quite fragile. If something would change
        // in this respect and breaks it, a different mechanism to set correct locale would need to be used.
        Locale.setDefault(Locale.US);
    }

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        SchemaDebugUtil.initializePrettyPrinter();
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @SuppressWarnings({ "SimplifiedTestNGAssertion", "CheckStyle" })
    @Test
    public void test030FormatAccount() throws Exception {

        given();

        PrismObject<ShadowType> jack = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_FILE));
        System.out.println(jack.debugDump());

        when();

        String jackFormattedHideNone = valueFormatter.formatAccountAttributes(jack.asObjectable(), null, true);
        System.out.println("no hidden paths + show operational attributes: " + jackFormattedHideNone);

        String jackFormattedHideAux = valueFormatter.formatAccountAttributes(jack.asObjectable(), AUXILIARY_PATHS, true);
        System.out.println("hide auxiliary paths + show operational attributes: " + jackFormattedHideAux);

        then();

        final String NAME = "Name: jack";
        final String PASSWORD = "(protected string)";
        final String ADMINISTRATIVE_STATUS = "Administrative status: ENABLED";
        final String EFFECTIVE_STATUS = "Effective status: ENABLED";

        assertTrue("account name is not shown", jackFormattedHideNone.contains(NAME));
        assertTrue("account password is not shown", jackFormattedHideNone.contains(PASSWORD));
        assertTrue("administrative status is not shown", jackFormattedHideNone.contains(ADMINISTRATIVE_STATUS));
        assertTrue("effective status is not shown", jackFormattedHideNone.contains(EFFECTIVE_STATUS));

        assertTrue("account name is not shown", jackFormattedHideAux.contains(NAME));
        assertTrue("account password is not shown", jackFormattedHideAux.contains(PASSWORD));
        assertTrue("administrative status is not shown", jackFormattedHideAux.contains(ADMINISTRATIVE_STATUS));
        assertTrue("effective status is shown although it should be hidden", !jackFormattedHideAux.contains(EFFECTIVE_STATUS));
    }


    /**
     * Delta formatter cannot correctly deal with a situation when we are replacing empty container value with one
     * that contains only hidden items.
     *
     * An example:
     * - BEFORE: assignment[1]/activation = (empty)
     * - DELTA: REPLACE assignment[1]/activation with (effectiveStatus: ENABLED) -- i.e. with seemingly empty PCV
     *
     * We should hide such modification. But we do not do this now. (See MID-5350.)
     *
     * We fixed that issue by changing the delta that is created.
     * But this behavior of delta formatter should be eventually fixed. See MID-6111.
     */
    @Test(enabled = false)
    public void test060FormatDeltaWithSingleOperationalItemContainer() throws Exception {

        given();

        PrismObject<UserType> jack = PrismTestUtil.parseObject(new File(USER_JACK_FILE));
        displayValue("jack", jack.debugDump());

        // @formatter:off
        ObjectDelta<Objectable> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS)
                    .replace(ActivationStatusType.ENABLED)
                // see MID-5350
                .item(UserType.F_ASSIGNMENT, 1, UserType.F_ACTIVATION)
                    .replace(new ActivationType().effectiveStatus(ActivationStatusType.ENABLED))
                .asObjectDelta("some-user-oid");
        // @formatter:on

        displayValue("delta", delta.debugDump());

        when();

        boolean hasVisible = textFormatter.containsVisibleModifiedItems(delta.getModifications(), false, false);

        then();

        assertFalse("There should be no visible modified items", hasVisible);
    }

}
