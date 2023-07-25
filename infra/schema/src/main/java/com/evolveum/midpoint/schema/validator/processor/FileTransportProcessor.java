/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@SuppressWarnings("unused")
public class FileTransportProcessor implements UpgradeObjectProcessor<SystemConfigurationType> {

    @Override
    public UpgradePhase getPhase() {
        return UpgradePhase.BEFORE;
    }

    @Override
    public UpgradePriority getPriority() {
        return UpgradePriority.OPTIONAL;
    }

    @Override
    public UpgradeType getType() {
        return UpgradeType.SEAMLESS;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        return matchParentTypeAndItemName(object, path, NotificationConfigurationType.class, NotificationConfigurationType.F_FILE);
    }

    @Override
    public boolean process(PrismObject<SystemConfigurationType> object, ItemPath path) throws Exception {
        SystemConfigurationType config = object.asObjectable();
        NotificationConfigurationType notificationConfig = config.getNotificationConfiguration();

        List<FileConfigurationType> files = notificationConfig.getFile();
        if (files == null || files.isEmpty()) {
            return false;
        }

        MessageTransportConfigurationType messageTransport = config.getMessageTransportConfiguration();
        if (messageTransport == null) {
            messageTransport = new MessageTransportConfigurationType();
            config.setMessageTransportConfiguration(messageTransport);
        }

        for (FileConfigurationType transport : files) {
            FileTransportConfigurationType ft = new FileTransportConfigurationType();
            ft.setFile(transport.getFile());

            copyTransport(transport, ft);

            messageTransport.getFile().add(ft);
        }

        files.clear();

        return true;
    }
}
