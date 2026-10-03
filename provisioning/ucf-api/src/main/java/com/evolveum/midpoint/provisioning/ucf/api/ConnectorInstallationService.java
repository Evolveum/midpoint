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
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;

public interface ConnectorInstallationService {

    DownloadedConnector downloadConnector(String uri, String targetName, OperationResult result);

    DownloadedConnector writeConnector(String targetName, CheckedConsumer<FileOutputStream> writter) throws CommonException;

    EditableConnector editableConnectorFor(@NotNull ConnectorType objectable);

    EditableConnector editableConnectorFor(String directory);

    /**
     * Copies the local bundle directory of the given connector to a new bundle directory with the
     * given name (under the connector scan directory) and returns an editable handle to the copy.
     * The staging/atomic-move semantics mirror {@link DownloadedConnector#install(OperationResult)}:
     * the copy is written to {@code <targetName>.tmp} first and then moved to the final location,
     * so a concurrent bundle scan never sees a half-copied directory. Any pre-existing directory
     * (or staging file) with the target name is removed first.
     *
     * @throws com.evolveum.midpoint.util.exception.SystemException if the connector has no local
     *         (file-based) bundle directory or the copy fails
     */
    EditableConnector copyBundle(@NotNull ConnectorType sourceConnector, @NotNull String targetDirectoryName,
            OperationResult result);

    /**
     * The {@code ConnectorBundle-ConnectorClass} manifest attribute of the local bundle of the
     * given connector (the fully-qualified class name of its {@code @ConnectorClass}), or {@code null}
     * when the connector has no local bundle or the attribute is absent.
     */
    String getConnectorClass(@NotNull ConnectorType connector);

    /**
     * Reloads the local connector bundle of the given connector in the UCF framework so that
     * subsequently generated schemas and created instances reflect the current (possibly
     * modified) bundle content.
     *
     * @throws ObjectNotFoundException if the connector bundle is not registered in the UCF framework
     */
    void reloadLocalConnectorBundle(@NotNull ConnectorType connector) throws ObjectNotFoundException;

}
