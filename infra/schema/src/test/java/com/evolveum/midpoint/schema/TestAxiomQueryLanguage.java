/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.PrismQueryLanguageParser;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TestAxiomQueryLanguage extends AbstractSchemaTest {

    @Test
    public void testSpacesInRefMatches() throws SchemaException {
        PrismQueryLanguageParser parser = PrismContext.get().createQueryParser();

        // MID-7709
        ObjectFilter query = parser.parseFilter(UserType.class,
                "assignment/targetRef matches (oid =\"9b99ada6-b421-472a-9b64-22c38b5af296\")");
        assertTrue(query instanceof RefFilter);

        query = parser.parseFilter(UserType.class,
                "assignment/targetRef matches (oid= \"9b99ada6-b421-472a-9b64-22c38b5af296\")");
        assertTrue(query instanceof RefFilter);
    }
}
