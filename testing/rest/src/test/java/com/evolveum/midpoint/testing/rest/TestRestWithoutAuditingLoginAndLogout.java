/*
 * Copyright (c) 2013-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.rest;

import java.io.File;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.cxf.jaxrs.client.WebClient;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static org.testng.AssertJUnit.assertNotNull;

public class TestRestWithoutAuditingLoginAndLogout extends RestServiceInitializer {

    public static final File SYSTEM_CONFIGURATION_FILE = new File(BASE_REPO_DIR, "system-configuration-without-auditing.xml");

    @Override
    public void initSystem(Task initTask, OperationResult result) throws Exception {
        super.initSystem(initTask, result);
        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);
        addObject(systemConfig, executeOptions().overwrite(), initTask, result);
    }

    @Test
    public void test001GetUserAdministrator() {
        WebClient client = prepareClient(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD);
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        when();
        Response response = client.get();

        then();
        assertStatus(response, 200);
        UserType userType = response.readEntity(UserType.class);
        assertNotNull("Returned entity in body must not be null.", userType);
        logger.info("Returned entity: {}", userType.asPrismObject().debugDump());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(0);
    }

    @Override
    protected String getAcceptHeader() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    protected String getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    protected MidpointAbstractProvider getProvider() {
        return jsonProvider;
    }
}
