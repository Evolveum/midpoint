/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.rest;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public abstract class RestServiceInitializer extends AbstractRestServiceInitializer {

    private <O extends ObjectType> void addObjectViaRepository(
            PrismObject<O> object, RepoAddOptions options, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException {

        repositoryService.addObject(object, options, result);
    }

    @Override
    public void initSystem(Task initTask, OperationResult result) throws Exception {
        super.initSystem(initTask, result);
        logger.trace("initSystem");

        InternalsConfig.encryptionChecks = false;

        PrismObject<RoleType> superRole = parseObject(ROLE_SUPERUSER_FILE);
        addObject(superRole, executeOptions().overwrite(), initTask, result);
        PrismObject<RoleType> endRole = parseObject(ROLE_ENDUSER_FILE);
        addObject(endRole, executeOptions().overwrite(), initTask, result);
        addObject(ROLE_REST_FILE, initTask, result);
        addObject(ROLE_REST_LIMITED_FILE, initTask, result);
        addObject(ROLE_READER_FILE, initTask, result);
        PrismObject<UserType> adminUser = parseObject(USER_ADMINISTRATOR_FILE);
        addObject(adminUser, executeOptions().overwrite(), initTask, result);
        addObject(USER_NOBODY_FILE, initTask, result);
        addObject(USER_CYCLOPS_FILE, initTask, result);
        addObject(USER_SOMEBODY_FILE, initTask, result);
        addObject(USER_JACK_FILE, initTask, result);
        addObject(USER_REST_LIMITED_FILE, initTask, result);
        addObject(parseObject(VALUE_POLICY_GENERAL), executeOptions().overwrite(), initTask, result);
        addObject(VALUE_POLICY_NUMERIC, initTask, result);
        addObject(VALUE_POLICY_SIMPLE, initTask, result);
        addObject(VALUE_POLICY_SECURITY_ANSWER, initTask, result);
        addObject(parseObject(SECURITY_POLICY), executeOptions().overwrite(), initTask, result);
        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);
        addObject(systemConfig, executeOptions().overwrite(), initTask, result);

        addObject(ROLE_META_APPROVAL, initTask, result);
        addObject(ROLE_TO_APPROVE, initTask, result);

        InternalMonitor.reset();

        getModelService().postInit(result);

        result.computeStatus();
    }
}
