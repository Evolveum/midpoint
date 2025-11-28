package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import java.util.List;

public interface DownloadedConnector {

    List<ConnectorType> install(OperationResult result);

    DownloadedConnector unpack(String directory, OperationResult result);

    void remove();
    EditableConnector asEditable();
}
