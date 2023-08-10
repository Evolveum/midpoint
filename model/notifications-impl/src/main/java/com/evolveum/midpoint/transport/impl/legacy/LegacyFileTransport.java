/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.transport.impl.legacy;

import static com.evolveum.midpoint.transport.impl.TransportUtil.formatToFileNew;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.SendingContext;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.api.transports.TransportSupport;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.transport.impl.TransportUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/** Legacy transport that should be removed after 4.5; type parameter is irrelevant. */
@Deprecated
@Component
public class LegacyFileTransport implements Transport<GeneralTransportConfigurationType> {

    private static final Trace LOGGER = TraceManager.getTrace(LegacyFileTransport.class);

    private static final String NAME = "file";

    private static final String DOT_CLASS = LegacyFileTransport.class.getName() + ".";
    private static final String DEFAULT_FILE_NAME = "notifications.txt";

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    @Override
    public void send(Message message, String transportName, SendingContext ctx, OperationResult parentResult) {
        OperationResult result = parentResult.createMinorSubresult(DOT_CLASS + "send");
        FileConfigurationType fileConfig = getTransportConfiguration(
                transportName, cacheRepositoryService, result);
        String fileName;
        if (fileConfig != null && fileConfig.getFile() != null) {
            fileName = fileConfig.getFile();
        } else {
            LOGGER.info("Notification transport configuration for '{}' was not found or has no file name configured: using default of '{}'",
                    transportName, DEFAULT_FILE_NAME);
            fileName = DEFAULT_FILE_NAME;
        }
        TransportUtil.appendToFile(fileName, formatToFileNew(message, transportName), LOGGER, result);
    }

    private static FileConfigurationType getTransportConfiguration(
            String transportName, RepositoryService cacheRepositoryService, OperationResult result) {

        SystemConfigurationType systemConfiguration =
                TransportUtil.getSystemConfiguration(cacheRepositoryService, result);
        if (systemConfiguration == null || systemConfiguration.getNotificationConfiguration() == null) {
            return null;
        }

        String transportConfigName = StringUtils.substringAfter(transportName, NAME + ":");
        for (FileConfigurationType namedConfiguration : systemConfiguration.getNotificationConfiguration().getFile()) {
            if ((StringUtils.isEmpty(transportConfigName) && namedConfiguration.getName() == null)
                    || (transportConfigName != null && transportConfigName.equals(namedConfiguration.getName()))) {
                return namedConfiguration;
            }
        }
        return null;
    }

    @Override
    public String getDefaultRecipientAddress(FocusType recipient) {
        return PolyString.getOrig(recipient.getName()) + " <" + recipient.getEmailAddress() + ">";
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void configure(@NotNull GeneralTransportConfigurationType configuration, @NotNull TransportSupport transportSupport) {
        // not called for legacy transport component
    }

    @Override
    public GeneralTransportConfigurationType getConfiguration() {
        return null;
    }
}
