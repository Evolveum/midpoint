/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.interaction;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Experimental
public interface DashboardService {

    DashboardWidget createWidgetData(DashboardWidgetType widget, boolean useDisplaySource, Task task, OperationResult result)
            throws CommonException;

    ObjectCollectionType getObjectCollectionType(DashboardWidgetType widget, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    CollectionRefSpecificationType getCollectionRefSpecificationType(DashboardWidgetType widget, Task task, OperationResult result);

    DashboardWidget createEmptyWidgetData(DashboardWidgetType widget);
    Integer countAuditEvents(CollectionRefSpecificationType collectionRef, ObjectCollectionType collection, Task task, OperationResult result)
            throws CommonException;

}
