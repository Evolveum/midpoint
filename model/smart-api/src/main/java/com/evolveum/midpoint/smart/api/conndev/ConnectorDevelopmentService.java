package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public interface ConnectorDevelopmentService {

    //List<ConnectorDevelopmentStepType> getCompletedSteps(ConnectorDevelopmentType project);

    //List<ConnectorDevelopmentStepType> getAvailableNextSteps(ConnectorDevelopmentType project);


    ConnectorDevelopmentOperation startFromNew(ConnDevApplicationInfoType basicInfo, OperationResult result);

    ConnectorDevelopmentOperation continueFrom(ConnectorDevelopmentType type);


}
