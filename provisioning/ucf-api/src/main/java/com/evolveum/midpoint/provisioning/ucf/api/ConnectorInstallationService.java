/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public interface ConnectorInstallationService {

    DownloadedConnector downloadConnector(String uri, String targetName, OperationResult result);
    EditableConnector editableConnectorFor(@NotNull ConnectorType objectable);
}
