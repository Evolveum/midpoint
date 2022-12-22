/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringNormalizerConfigurationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author lazyman
 */
public abstract class DataImport {

    private static final Trace LOGGER = TraceManager.getTrace(DataImport.class);

    protected static final String DOT_CLASS = DataImport.class.getName() + ".";
    protected static final String OPERATION_INITIAL_OBJECTS_IMPORT = DOT_CLASS + "initialObjectsImport";
    protected static final String OPERATION_IMPORT_OBJECT = DOT_CLASS + "importObject";

    @Autowired protected PrismContext prismContext;
    protected ModelService model;
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

    public abstract void init() throws SchemaException;

    protected SecurityContext provideFakeSecurityContext() throws SchemaException {
        // We need to provide a fake Spring security context here.
        // We have to fake it because we do not have anything in the repository yet. And to get
        // something to the repository we need a context. Chicken and egg. So we fake the egg.
        SecurityContext securityContext = SecurityContextHolder.getContext();
        UserType userAdministrator = new UserType();
        prismContext.adopt(userAdministrator);
        userAdministrator.setName(new PolyStringType(new PolyString("initAdmin", "initAdmin")));
        MidPointPrincipal principal = new MidPointPrincipal(userAdministrator);
        AuthorizationType superAutzType = new AuthorizationType();
        prismContext.adopt(superAutzType, RoleType.class, RoleType.F_AUTHORIZATION);
        superAutzType.getAction().add(AuthorizationConstants.AUTZ_ALL_URL);
        Authorization superAutz = new Authorization(superAutzType);
        Collection<Authorization> authorities = principal.getAuthorities();
        authorities.add(superAutz);
        Authentication authentication = new PreAuthenticatedAuthenticationToken(principal, null);
        securityContext.setAuthentication(authentication);
        return securityContext;
    }

    protected <O extends ObjectType> void preImportUpdate(PrismObject<O> object) {
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
