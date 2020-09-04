/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.interaction;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

/**
 * @author skublik
 */
@Experimental
public interface DashboardService {

    DashboardWidget createWidgetData(DashboardWidgetType widget, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException;

    List<PrismObject<ObjectType>> searchObjectFromCollection(CollectionRefSpecificationType collection, QName typeForFilter,
            Collection<SelectorOptions<GetOperationOptions>> options, ExpressionType condition, Task task, OperationResult result) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    List<AuditEventRecordType> searchObjectFromCollection(CollectionRefSpecificationType collectionConfig, ExpressionType condition, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException;

    ObjectCollectionType getObjectCollectionType(DashboardWidgetType widget, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    CollectionRefSpecificationType getCollectionRefSpecificationType(DashboardWidgetType widget, Task task, OperationResult result);

    Integer countAuditEvents(CollectionRefSpecificationType collectionRef, ObjectCollectionType collection, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException;

}
