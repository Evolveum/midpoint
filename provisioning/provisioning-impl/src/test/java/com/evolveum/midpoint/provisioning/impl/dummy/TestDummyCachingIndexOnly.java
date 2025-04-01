/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.dummy;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.testng.SkipException;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDummyCachingIndexOnly extends TestDummyCaching {

    public static final File RESOURCE_DUMMY_FILE = new File(TestDummyCaching.TEST_DIR, "resource-dummy-index-only.xml");

    @Autowired(required = false)
    @Qualifier("repositoryService") // we want repo implementation, not cache
    private RepositoryService repositoryService;

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected boolean isWeaponIndexOnly() {
        return !isNativeRepository(); // in native repo, all attributes are technically index-only, and all are fetched by default
    }

    @Override
    protected @NotNull Collection<? extends QName> getCachedAccountAttributes() throws SchemaException, ConfigurationException {
        var all = new ArrayList<>(getCachedAccountAttributesWithIndexOnly());
        if (isWeaponIndexOnly()) {
            all.remove(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME);
        }
        return all;
    }

    @Override
    protected @NotNull Collection<? extends QName> getCachedAccountAttributesWithIndexOnly() throws SchemaException, ConfigurationException {
        return getAccountDefaultDefinition().getAttributeNames();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        if (repositoryService instanceof SqlRepositoryServiceImpl sqlRepositoryService) {
            // These are experimental features, so they need to be explicitly enabled.
            // This will be eliminated later, when we make them enabled by default.
            sqlRepositoryService.sqlConfiguration().setEnableIndexOnlyItems(true);
            sqlRepositoryService.sqlConfiguration().setEnableNoFetchExtensionValuesInsertion(true);
            sqlRepositoryService.sqlConfiguration().setEnableNoFetchExtensionValuesDeletion(true);
        } else {
            // It's Sqale repo and it has no explicit switch for index only.
        }
    }

    @Override
    void assertPolyStringIndexOnly(OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (!isNativeRepository()) {
            var repoShadow = getShadowRepoRetrieveAllAttributes(ACCOUNT_WILL_OID, result);
            var values = repoShadow.getPrismObject()
                    .findProperty(ShadowType.F_ATTRIBUTES.append(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME))
                    .getRealValues(PolyString.class);
            displayValue("weapon values", values);
            var origValues = values.stream().map(v -> v.getOrig()).toList();
            var normValues = values.stream().map(v -> v.getNorm()).toList();
            assertThat(origValues).as("orig values").containsExactlyInAnyOrder("Sword", "LOVE");
            assertThat(normValues).as("norm values").containsExactlyInAnyOrder("sword", "love");
        }
    }

    public void test228RetrievingAccountsWithoutAssociations() throws Exception {
        skipTestIf(!isNativeRepository(), "This test currently fails on generic repo"); // MID-10622
        super.test228RetrievingAccountsWithoutAssociations();
    }
}
