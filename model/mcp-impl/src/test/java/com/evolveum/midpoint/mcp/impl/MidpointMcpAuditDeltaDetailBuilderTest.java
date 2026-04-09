/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

/**
 * Unit tests for {@link MidpointMcpAuditDeltaDetailBuilder}.
 */
public class MidpointMcpAuditDeltaDetailBuilderTest extends AbstractUnitTest {

    private static final ItemPath CREDENTIALS_PASSWORD_VALUE_PATH =
            ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);

    @BeforeClass
    public void initPrismContext() throws SchemaException, IOException, SAXException {
        if (com.evolveum.midpoint.prism.PrismContext.get() == null) {
            PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        }
    }

    @Test
    public void replacePropertyProducesOldAndNew() throws SchemaException {
        var objectDelta = PrismTestUtil.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, "oid-1", UserType.F_COST_CENTER, "cc-new");

        ObjectDeltaType bean = DeltaConvertor.toObjectDeltaType(objectDelta);
        MidpointMcpAuditDeltaDetailBuilder.AttributeChangesResult r = MidpointMcpAuditDeltaDetailBuilder.buildAttributeChanges(bean);

        assertFalse(r.truncated());
        List<Map<String, Object>> rows = r.rows();
        assertEquals(rows.size(), 1);
        Map<String, Object> row = rows.get(0);
        assertTrue(row.get("path").toString().toLowerCase().contains("costcenter"));
        assertEquals(row.get("modificationType"), "REPLACE");
        assertNull(row.get("oldValue"));
        assertEquals(row.get("newValue"), "cc-new");
    }

    @Test
    public void passwordReplaceIsRedacted() throws SchemaException {
        ProtectedStringType protectedString = new ProtectedStringType();
        protectedString.setClearValue("secret-value");

        var objectDelta = PrismTestUtil.getPrismContext().deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, "oid-2", CREDENTIALS_PASSWORD_VALUE_PATH, protectedString);

        ObjectDeltaType bean = DeltaConvertor.toObjectDeltaType(objectDelta);
        MidpointMcpAuditDeltaDetailBuilder.AttributeChangesResult r = MidpointMcpAuditDeltaDetailBuilder.buildAttributeChanges(bean);

        assertEquals(r.rows().size(), 1);
        Map<String, Object> row = r.rows().get(0);
        assertTrue(row.get("path").toString().toLowerCase().contains("password"));
        assertEquals(row.get("oldValue"), "***");
        assertEquals(row.get("newValue"), "***");
    }

    @Test
    public void iterationAndIterationTokenAreOmittedFromDeltaRows() throws SchemaException {
        var objectDelta = PrismTestUtil.getPrismContext().deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, "oid-3");
        objectDelta.addModificationReplaceProperty(UserType.F_COST_CENTER, "cc");
        objectDelta.addModificationReplaceProperty(AssignmentHolderType.F_ITERATION, 0);
        objectDelta.addModificationReplaceProperty(AssignmentHolderType.F_ITERATION_TOKEN, "token-value");

        ObjectDeltaType bean = DeltaConvertor.toObjectDeltaType(objectDelta);
        MidpointMcpAuditDeltaDetailBuilder.AttributeChangesResult r = MidpointMcpAuditDeltaDetailBuilder.buildAttributeChanges(bean);

        assertFalse(r.truncated());
        assertEquals(r.rows().size(), 1);
        assertTrue(r.rows().get(0).get("path").toString().toLowerCase().contains("costcenter"));
    }
}
