/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.channel;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

public class IdentityRecoveryAuthenticationChannel extends AuthenticationChannelImpl {

    private final TaskManager taskManager;
    private final RepositoryService repositoryService;

    private PrismObject<ServiceType> identityRecoveryService = null;

    private static final Trace LOGGER = TraceManager.getTrace(IdentityRecoveryAuthenticationChannel.class);
    private static final String DOT_CLASS = IdentityRecoveryAuthenticationChannel.class.getName() + ".";
    private static final String IDENTITY_RECOVERY_SERVICE_OID = "00000000-0000-0000-0000-000000000610";
    private static final String LOAD_IDENTITY_RECOVERY_SERVICE = DOT_CLASS + "loadIdentityRecoveryService";


    public IdentityRecoveryAuthenticationChannel(AuthenticationSequenceChannelType channel, TaskManager taskManager,
            RepositoryService repositoryService) {
        super(channel);
        this.taskManager = taskManager;
        this.repositoryService = repositoryService;
        loadIdentityRecoveryService();
    }

    private void loadIdentityRecoveryService() {
        var result = taskManager.createTaskInstance(LOAD_IDENTITY_RECOVERY_SERVICE).getResult();
        try {
            identityRecoveryService = repositoryService.getObject(ServiceType.class, IDENTITY_RECOVERY_SERVICE_OID,
                    null, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            LOGGER.debug("Unable to load identity recovery service. ", e);
        }
    }

    public ServiceType getIdentityRecoveryService() {
        return identityRecoveryService != null ? identityRecoveryService.asObjectable() : null;
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_IDENTITY_RECOVERY_URI;
    }

    public String getPathAfterSuccessfulAuthentication() {
        return "/identityRecovery";
    }

//    @Override
//    protected Collection<String> getAdditionalAuthoritiesList() {
//        return Collections.singletonList(AuthorizationConstants.AUTZ_UI_IDENTITY_RECOVERY_URL);
//    }

}
