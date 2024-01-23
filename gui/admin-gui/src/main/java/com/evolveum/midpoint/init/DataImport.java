/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import java.io.File;
import java.util.Arrays;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringNormalizerConfigurationType;

/**
 * @author lazyman
 */
public abstract class DataImport {

    private static final Trace LOGGER = TraceManager.getTrace(DataImport.class);

    protected static final String DOT_CLASS = DataImport.class.getName() + ".";
    protected static final String OPERATION_INITIAL_OBJECTS_IMPORT = DOT_CLASS + "initialObjectsImport";
    protected static final String OPERATION_IMPORT_OBJECT = DOT_CLASS + "importObject";

    protected static final String OPERATION_INITIALIZE_ADMINISTRATOR_INITIAL_PASSWORD = DOT_CLASS + "initAdministratorInitialPassword";

    protected static final int MIN_PASSWORD_LENGTH = 10;

    @Autowired protected PrismContext prismContext;
    protected ModelService model;
    @Autowired  protected ModelInteractionService modelInteractionService;
    protected TaskManager taskManager;
    @Autowired protected MidpointConfiguration configuration;

    public void setModel(ModelService model) {
        Validate.notNull(model, "Model service must not be null.");
        this.model = model;
    }

    public void setPrismContext(PrismContext prismContext) {
        Validate.notNull(prismContext, "Prism context must not be null.");
        this.prismContext = prismContext;
    }

    public void setTaskManager(TaskManager taskManager) {
        Validate.notNull(taskManager, "Task manager must not be null.");
        this.taskManager = taskManager;
    }

    public void setConfiguration(MidpointConfiguration configuration) {
        Validate.notNull(configuration, "Midpoint configuration must not be null.");
        this.configuration = configuration;
    }

    public void setModelInteractionService(ModelInteractionService modelInteractionService) {
        this.modelInteractionService = modelInteractionService;
    }

    public abstract void init() throws SchemaException;

    protected SecurityContext provideFakeSecurityContext() {
        // We need to provide a fake Spring security context here.
        // We have to fake it because we do not have anything in the repository yet. And to get
        // something to the repository we need a context. Chicken and egg. So we fake the egg.
        SecurityContext securityContext = SecurityContextHolder.getContext();
        MidPointPrincipal principal = MidPointPrincipal.privileged(
                new UserType()
                        .oid(SystemObjectsType.USER_ADMINISTRATOR.value())
                        .name("initAdmin"));
        Authentication authentication = new PreAuthenticatedAuthenticationToken(principal, null);
        securityContext.setAuthentication(authentication);
        return securityContext;
    }

    protected <O extends ObjectType> void preImportUpdate(PrismObject<O> object, Task task, OperationResult mainResult) {
        if (object.canRepresent(SystemConfigurationType.class)) {
            SystemConfigurationType systemConfigType = (SystemConfigurationType) object.asObjectable();
            InternalsConfigurationType internals = systemConfigType.getInternals();
            if (internals != null) {
                PolyStringNormalizerConfigurationType normalizerConfig = internals.getPolyStringNormalizer();
                if (normalizerConfig != null) {
                    try {
                        prismContext.configurePolyStringNormalizer(normalizerConfig);
                        LOGGER.debug("Applied PolyString normalizer configuration {}", DebugUtil.shortDumpLazily(normalizerConfig));
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        LOGGER.error("Error applying polystring normalizer configuration: " + e.getMessage(), e);
                        throw new SystemException("Error applying polystring normalizer configuration: " + e.getMessage(), e);
                    }
                    // PolyString normalizer configuration applied. But we need to re-normalize the imported object
                    // otherwise it would be normalized in a different way than other objects.
                    object.recomputeAllValues();
                }
            }
        }
        if (SystemObjectsType.USER_ADMINISTRATOR.value().equals(object.getOid())) {
            if (object.asObjectable()  instanceof UserType administrator) {
                initAdministratorInitialPassword(administrator, task, mainResult);
            }
        }

    }

    private <O extends ObjectType> void initAdministratorInitialPassword(UserType object, Task task, OperationResult mainResult) {
        var result = mainResult.createSubresult(OPERATION_INITIALIZE_ADMINISTRATOR_INITIAL_PASSWORD);
        try {
            var initialPassword = configuration.getConfiguration().getString(MidpointConfiguration.ADMINISTRATOR_INITIAL_PASSWORD);
            if (initialPassword == null) {
                // Generate password and log it
                var policy = model.getObject(ValuePolicyType.class, SystemObjectsType.PASSWORD_POLICY_DEFAULT.value(), null, task, result).asObjectable();
                initialPassword = modelInteractionService.generateValue(policy, MIN_PASSWORD_LENGTH, false,
                        object.asPrismObject(), "initial password", task, result);
                LOGGER.warn("Administrator initial password (except double quotes): \"{}\"", initialPassword);
            }

            if (initialPassword != null) {
                object.setCredentials(new CredentialsType()
                        .password(new PasswordType().value(new ProtectedStringType().clearValue(initialPassword))));
                LOGGER.warn("Please change administrator password  after first login.");
            } else {
                LOGGER.warn("Administrator account was created without password. See https://docs.evolveum.com/midpoint/reference/security/authentication/administrator-initial-password");
                // Log that administrator was generated without initial password, pointer how to change administrator password
            }
        } catch (Exception e) {
            result.recordFatalError("Can not set initial password", e);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    protected void sortFiles(File[] files) {
        Arrays.sort(files, (o1, o2) -> {
            int n1 = getNumberFromName(o1);
            int n2 = getNumberFromName(o2);

            return n1 - n2;
        });
    }

    private int getNumberFromName(File file) {
        String name = file.getName();
        String number = StringUtils.left(name, 3);
        if (number.matches("[\\d]+")) {
            return Integer.parseInt(number);
        }
        return 0;
    }
}
