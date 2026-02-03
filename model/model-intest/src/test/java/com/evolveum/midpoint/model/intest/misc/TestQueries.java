/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.misc;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.schema.query.TypedQuery;
import com.evolveum.midpoint.test.util.ParallelTestThread;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestQueries extends AbstractEmptyModelIntegrationTest {

    /**
     * MID-11058 Using midPoint.queryFor() in multi-threaded iterativeScripting returns incorrect results
     */
    @Test
    public void testQueryForConcurrency() throws Exception {
        List<List<String>> refOids = List.of(
                List.of("32031206-5d6b-4755-835b-8bbb1e671694", "b38ca87f-4677-4e59-9981-6c61382dd9a2"),
                List.of("8aaeed8f-3ac1-4fb8-bddb-d8522c241fcd", "77742c0e-364c-47de-9c57-2b38c0085d1f"),
                List.of("5f7d3f6e-2f4e-4d3a-9f7a-2c4b8e1f0a1b", "d4e5f6a7-b8c9-0d1e-2f3a-4b5c6d7e8f90"),
                List.of("a1b2c3d4-e5f6-7a8b-9c0d-e1f2a3b4c5d6", "f1e2d3c4-b5a6-7980-1a2b-3c4d5e6f7g8h"),
                List.of("12345678-90ab-cdef-1234-567890abcdef", "abcdef12-3456-7890-abcd-ef1234567890"),
                List.of("0fedcba9-8765-4321-0fed-cba98765432a", "23456789-0abc-def1-2345-67890abcdef1")
        );

        ParallelTestThread[] threads = TestUtil.multithread((i) -> {

            try {
                List<String> oids = refOids.get(i);

                for (int j = 0; j < 10000; j++) {
                    logger.info("OIDs: {}", oids);

                    String queryString = ". inOid ('" + StringUtils.join(oids, "','") + "') and " +
                            "resourceRef matches ( oid = '92cc7ce8-e983-11ef-9197-63aa3b960f5e') and objectClass = 'ri:AccountObjectClass'";

                    TypedQuery<ShadowType> query = TypedQuery.parse(ShadowType.class, queryString);
                    Assertions.assertThat(query).isNotNull();

                    Assertions.assertThat(query.toObjectQuery().toString())
                            .isEqualTo("Q{AND(IN OID: "
                                    + StringUtils.join(oids, "")
                                    + "; ; REF: resourceRef, PRV(oid=92cc7ce8-e983-11ef-9197-63aa3b960f5e, targetType=null), "
                                    + "targetFilter=null; EQUAL: objectClass, PPV(QName:{...resource/instance-3}AccountObjectClass)), null paging}");
                }
            } catch (SchemaException ex) {
                throw new RuntimeException(ex);
            }
        }, refOids.size(), 10);

        then();
        TestUtil.waitForThreads(threads, DEFAULT_SHORT_TASK_WAIT_TIMEOUT);
    }
}

