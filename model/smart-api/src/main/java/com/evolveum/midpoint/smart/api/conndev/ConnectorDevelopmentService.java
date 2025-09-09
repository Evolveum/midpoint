package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public interface ConnectorDevelopmentService {

    //List<ConnectorDevelopmentStepType> getCompletedSteps(ConnectorDevelopmentType project);

    //List<ConnectorDevelopmentStepType> getAvailableNextSteps(ConnectorDevelopmentType project);


    ConnectorDevelopmentOperation startFromNew(ConnDevApplicationInfoType basicInfo, OperationResult result);

    ConnectorDevelopmentOperation continueFrom(ConnectorDevelopmentType type);


    StatusInfo<ConnDevCreateConnectorResultType> getCreateConnectorStatus(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException;

    StatusInfo<ConnDevDiscoverGlobalInformationResultType> getDiscoverBasicInformationStatus(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException;

    StatusInfo<ConnDevDiscoverDocumentationResultType> getDiscoverDocumentationStatus(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException;

    StatusInfo<ConnDevGenerateArtifactResultType> getGenerateArtifactStatus(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException;

    StatusInfo<ConnDevDiscoverObjectClassInformationResultType> getDiscoverObjectClassInformationStatus(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException;

    StatusInfo<ConnDevDiscoverObjectClassDetailsResultType> getDiscoverObjectClassDetailsStatus(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException;

}
