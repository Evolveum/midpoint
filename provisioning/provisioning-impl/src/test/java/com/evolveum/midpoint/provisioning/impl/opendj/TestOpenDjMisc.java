/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.opendj;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.QNAME_DN;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Any other OpenDJ-based tests that require specific resource configuration, and are not easily integrable into
 * the {@link AbstractOpenDjTest}.
 *
 * Not part of the standard test suite yet. To be run separately.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDjMisc extends AbstractOpenDjTest {

    protected static final File TEST_DIR = new File(AbstractOpenDjTest.TEST_DIR, "misc");

    private static final TestObject<?> SYSTEM_CONFIGURATION = TestObject.file(
            TEST_DIR, "system-configuration.xml", "00000000-0000-0000-0000-000000000001");

    @AfterClass
    public void stopLdap() {
        doStopLdap();
    }

    protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj-many-associated-intents.xml");

    private static final String GROUP_1_DN = "cn=group-1,ou=groups,dc=example,dc=com";

    @Autowired private CacheConfigurationManager cacheConfigurationManager;

    @Override
    protected File getResourceOpenDjFile() {
        return RESOURCE_OPENDJ_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        doStartLdap();

        super.initSystem(initTask, initResult);

        resource = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, initTask, initResult);

        repoAdd(SYSTEM_CONFIGURATION, initResult);

        var result = provisioningService.testResource(RESOURCE_OPENDJ_OID, initTask, initResult);
        assertSuccess(result);

        openDJController.addEntry("""
                dn: cn=group-1,ou=groups,dc=example,dc=com
                cn: group-1
                objectclass: top
                objectclass: groupOfUniqueNames
                """);
    }

    /** Fetching an object with multiple entitlements covering multiple intents (MID-10600). */
    @Test
    public void test200GettingObjectsAssociatedToManyIntents() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var accountDn = "uid=john,ou=people,dc=example,dc=com";

        given("an account in 8 groups");
        openDJController.addEntry("""
                dn: uid=john,ou=people,dc=example,dc=com
                objectClass: inetOrgPerson
                uid: john
                cn: John
                sn: Smith
                """);
        openDJController.addGroupUniqueMember(GROUP_1_DN, accountDn);

        var shadows = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(resource)
                        .queryFor(OBJECT_CLASS_INETORGPERSON_QNAME)
                        .and()
                        .item(ShadowType.F_ATTRIBUTES, QNAME_DN).eq(accountDn)
                        .build(),
                null, task, result);
        var oid = MiscUtil.extractSingletonRequired(shadows).getOid();

        when("the account is fetched");
        CachePerformanceCollector.INSTANCE.clear();
        RepositoryCache.enterLocalCaches(cacheConfigurationManager);
        try {
            provisioningService.getObject(ShadowType.class, oid, null, task, result);
        } finally {
            RepositoryCache.exitLocalCaches();
        }
        displayDumpable("cache performance", CachePerformanceCollector.INSTANCE);

        // TODO some asserts here
    }
}
