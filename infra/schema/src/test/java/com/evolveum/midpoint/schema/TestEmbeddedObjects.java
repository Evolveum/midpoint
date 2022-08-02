/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests working with "embedded objects" - like $focus/identities/identity/data.
 *
 * FIXME not finished, failing
 *
 * Here should be also:
 *
 * . serialization and parsing
 * . delta application
 * . equivalence checking
 *
 * etc
 */
public class TestEmbeddedObjects extends AbstractSchemaTest {

    @Test
    public void testCreateEmbeddedObject() {
        given("main and to-be-embedded user objects");
        UserType main = new UserType()
                .name("main")
                .identities(new FocusIdentitiesType()
                        .identity(new FocusIdentityType()));

        UserType identityData = new UserType()
                .name("identity-data");

        when("object is embedded");
        main.getIdentities().getIdentity().get(0).setData(
                identityData);

        then("object is there");
        FocusType identityDataRetrieved = main.getIdentities().getIdentity().get(0).getData();
        Assertions.assertThat(identityDataRetrieved)
                .as("retrieved embedded object")
                .isEqualTo(identityData);
    }
}
