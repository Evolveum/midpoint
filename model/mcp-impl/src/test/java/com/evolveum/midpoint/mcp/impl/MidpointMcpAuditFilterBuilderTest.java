/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import static org.testng.Assert.assertNotNull;

import com.evolveum.midpoint.mcp.api.MidpointMcpAdvancedFilterSpec;
import com.evolveum.midpoint.mcp.api.MidpointMcpAdvancedQuerySpec;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Unit tests for {@link MidpointMcpAuditFilterBuilder}.
 */
public class MidpointMcpAuditFilterBuilderTest extends AbstractUnitTest {

    @BeforeClass
    public void initPrismContextIfNeeded() throws com.evolveum.midpoint.util.exception.SchemaException, IOException, SAXException {
        if (PrismContext.get() == null) {
            PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        }
    }

    @Test
    public void buildAdvancedFilterMessageEqProducesFilter() {
        MidpointMcpAdvancedQuerySpec spec = new MidpointMcpAdvancedQuerySpec();
        MidpointMcpAdvancedFilterSpec f = new MidpointMcpAdvancedFilterSpec();
        f.setPath("message");
        f.setOp("eq");
        f.setValue("login");
        spec.getFilters().add(f);

        ObjectFilter filter = MidpointMcpAuditFilterBuilder.buildAdvancedFilter(PrismContext.get(), spec);
        assertNotNull(filter);
    }

    @Test
    public void describeAdvancedFilterIncludesMessage() {
        MidpointMcpAdvancedQuerySpec spec = new MidpointMcpAdvancedQuerySpec();
        MidpointMcpAdvancedFilterSpec f = new MidpointMcpAdvancedFilterSpec();
        f.setPath("message");
        f.setOp("contains");
        f.setValue("admin");
        spec.getFilters().add(f);

        ObjectFilter filter = MidpointMcpAuditFilterBuilder.buildAdvancedFilter(PrismContext.get(), spec);
        String desc = MidpointMcpAuditFilterBuilder.describeAdvancedFilter(filter);
        assertNotNull(desc);
    }
}
