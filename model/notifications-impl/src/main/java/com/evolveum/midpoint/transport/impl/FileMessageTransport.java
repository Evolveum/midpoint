/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.transport.impl;

import static com.evolveum.midpoint.transport.impl.TransportUtil.formatToFileNew;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.SendingContext;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.api.transports.TransportSupport;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Message "transport" writing the messages into file(s).
 */
public class FileMessageTransport implements Transport<FileTransportConfigurationType> {

    private static final Trace LOGGER = TraceManager.getTrace(FileMessageTransport.class);

    private static final String DOT_CLASS = FileMessageTransport.class.getName() + ".";
    private static final String DEFAULT_FILE_NAME = "notifications.txt";

    private String name;
    private FileTransportConfigurationType configuration;

    @Override
    public void configure(
            @NotNull FileTransportConfigurationType configuration,
            @NotNull TransportSupport transportSupport) {
        this.configuration = Objects.requireNonNull(configuration);
        name = Objects.requireNonNull(configuration.getName());
        // transportSupport not needed here
    }

    @Override
    public void send(Message message, String transportName, SendingContext ctx, OperationResult parentResult) {
        OperationResult result = parentResult.createMinorSubresult(DOT_CLASS + "send");
        String fileName = configuration.getFile();
        if (fileName == null) {
            LOGGER.info("File transport configuration '{}' has no file name configured: using default of '{}'",
                    name, DEFAULT_FILE_NAME);
            fileName = DEFAULT_FILE_NAME;
        }
        TransportUtil.appendToFile(fileName, formatToFileNew(message, transportName), LOGGER, result);
    }

    @Override
    public String getDefaultRecipientAddress(FocusType recipient) {
        return PolyString.getOrig(recipient.getName()) + " <" + recipient.getEmailAddress() + ">";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public FileTransportConfigurationType getConfiguration() {
        return configuration;
    }
}
