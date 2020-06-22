/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;

// TODO MID-6319 - finish and add to test suite
@UnusedTestElement
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuditSearchTest extends BaseSQLRepoTest {

    @Override
    public void initSystem() throws Exception {
        // TODO add some sample audits here
    }

    @Test
    public void test100SearchAllAuditEvents() throws SchemaException {
        when("Searching audit with query without any conditions");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class).build();
        SearchResultList<AuditEventRecord> prismObjects =
                auditService.searchObjects(query, null, null);

        then("All audit events are returned");
        System.out.println("prismObjects = " + prismObjects);
        // TODO
    }

    @Test
    public void test110SearchAllAuditEventsOfSomeType() throws SchemaException {
        when("searching audit filtered by event type");
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_EVENT_TYPE).eq(AuditEventTypeType.ADD_OBJECT)
                .build();
        SearchResultList<AuditEventRecord> prismObjects =
                auditService.searchObjects(query, null, null);

        then("only audit events of a particular type are returned");
        System.out.println("prismObjects = " + prismObjects);
        // TODO
    }
}

