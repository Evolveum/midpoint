/*
 * Copyright (c) 2016-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.rest.authentication;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.common.rest.MidpointJsonProvider;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.testing.rest.AbstractRestServiceInitializer;

import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.AfterMethod;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;

public abstract class TestAbstractAuthentication extends AbstractRestServiceInitializer {

    protected static final File BASE_AUTHENTICATION_DIR = new File("src/test/resources/authentication/");
    protected static final File BASE_REPO_DIR = new File(BASE_AUTHENTICATION_DIR,"repo/");

    public static final File SECURITY_POLICY_DEFAULT = new File(BASE_REPO_DIR, "security-policy-default.xml");

    @Override
    public void initSystem(Task initTask, OperationResult result) throws Exception {
        super.initSystem(initTask, result);
        logger.trace("initSystem");

        InternalsConfig.encryptionChecks = false;

        addObject(parseObject(ROLE_SUPERUSER_FILE), executeOptions().overwrite(), initTask, result);
        addObject(parseObject(USER_ADMINISTRATOR_FILE), executeOptions().overwrite().raw(), initTask, result);
        addObject(parseObject(VALUE_POLICY_GENERAL), executeOptions().overwrite(), initTask, result);
        addObject(parseObject(SECURITY_POLICY_DEFAULT), executeOptions().overwrite(), initTask, result);
        addObject(parseObject(SYSTEM_CONFIGURATION_FILE), executeOptions().overwrite(), initTask, result);

        InternalMonitor.reset();

        provisioningService.postInit(result);
        getModelService().postInit(result);

        result.computeStatus();
    }

    @Autowired
    protected MidpointJsonProvider jsonProvider;

    @Autowired
    private SystemObjectCache systemObjectCache;

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

    @AfterMethod
    public void clearCache(){
        systemObjectCache.invalidateCaches();
    }

    protected void replaceSecurityPolicy(File securityPolicy) throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        PrismObject<SecurityPolicyType> secPolicy = parseObject(securityPolicy);
        addObject(secPolicy, executeOptions().overwrite(), task, result);
        getDummyAuditService().clear();
    }
}
