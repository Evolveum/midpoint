/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.CheckedConsumer;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public interface ConnectorInstallationService {

    DownloadedConnector downloadConnector(String uri, String targetName, OperationResult result);

    DownloadedConnector writeConnector(String targetName, CheckedConsumer<FileOutputStream> writter) throws CommonException;

    EditableConnector editableConnectorFor(@NotNull ConnectorType objectable);

    EditableConnector editableConnectorFor(String directory);

}
