package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.IOException;
import java.io.InputStream;

public interface ConnectorDevelopmentService {

    ConnectorDevelopmentOperation startFromNew(ConnDevApplicationInfoType basicInfo, OperationResult result);

    ConnectorDevelopmentOperation continueFrom(ConnectorDevelopmentType type);


    StatusInfo<ConnDevCreateConnectorResultType> getCreateConnectorStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevDiscoverGlobalInformationResultType> getDiscoverBasicInformationStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevDiscoverDocumentationResultType> getDiscoverDocumentationStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevProcessDocumentationResultType> getProcessDocumentationStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevGenerateArtifactResultType> getGenerateArtifactStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevFixObjectClassResultType> getFixObjectClassStatus(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException;

    StatusInfo<ConnDevDiscoverObjectClassInformationResultType> getDiscoverObjectClassInformationStatus(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException;

    StatusInfo<ConnDevDiscoverObjectClassAttributesResultType> getDiscoverObjectClassAttributesStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevDiscoverObjectClassEndpointsResultType> getDiscoverObjectClassEndpointsStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevRefreshSchemaResultType> getRefreshSchemaStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevDiscoverConnectivityEndpointResultType> getDiscoverConnectivityEndpointStatus(String token, Task task, OperationResult result) throws CommonException;

    StatusInfo<ConnDevExportConnectorResultType> getExportConnectorStatus(String token, Task task, OperationResult result) throws CommonException;

    /**
     * Cluster-aware download of an exported connector bundle jar, previously stored under
     * {@code fileName} in the midPoint home "export" directory of the node identified by
     * {@code nodeOid}. Falls back to a local file read if the file exists on this node.
     */
    InputStream getExportedConnectorFileStream(String fileName, String nodeOid, Task task, OperationResult result)
            throws CommonException, IOException;

}
