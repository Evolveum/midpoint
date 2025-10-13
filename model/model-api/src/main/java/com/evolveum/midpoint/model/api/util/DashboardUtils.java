/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.util;


import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

/**
 * @author skublik
 */
@Experimental
public class DashboardUtils {

    private static final Trace LOGGER = TraceManager.getTrace(DashboardUtils.class);

    private static final String AUDIT_RECORDS_ORDER_BY = " order by aer.timestampValue desc";
    private static final String TIMESTAMP_VALUE_NAME = "aer.timestampValue";
    private static final String PARAMETER_FROM = "from";

    public static DashboardWidgetSourceTypeType getSourceType(DashboardWidgetType widget) {
        if(isSourceTypeOfDataNull(widget)) {
            return null;
        }
        return widget.getData().getSourceType();
    }

    public static DashboardWidgetSourceTypeType getDisplaySourceType(DashboardWidgetType widget) {
        if(isDisplaySourceTypeOfDataNull(widget)) {
            return null;
        }
        return widget.getData().getDisplaySourceType();
    }

    public static boolean isDisplaySourceTypeOfDataNull(DashboardWidgetType widget) {
        if(isDataNull(widget)) {
            return true;
        }
        if(widget.getData().getDisplaySourceType() == null) {
            return true;
        }
        return false;
    }

    public static boolean isSourceTypeOfDataNull(DashboardWidgetType widget) {
        if(isDataNull(widget)) {
            return true;
        }
        if(widget.getData().getSourceType() == null) {
            LOGGER.error("SourceType of data is not found in widget " + widget.getIdentifier());
            return true;
        }
        return false;
    }

    public static boolean isDataNull(DashboardWidgetType widget) {
        if(widget.getData() == null) {
            LOGGER.error("Data is not found in widget " + widget.getIdentifier());
            return true;
        }
        return false;
    }

    public static boolean isCollectionOfDataNull(DashboardWidgetType widget) {
        if(isDataNull(widget)) {
            return true;
        }
        if(widget.getData().getCollection() == null) {
            LOGGER.error("Collection of data is not found in widget " + widget.getIdentifier());
            return true;
        }
        return false;
    }

    public static boolean isCollectionRefSpecOfCollectionNull(DashboardWidgetType widget) {
        if (isDataNull(widget)) {
            return true;
        }
        if (isCollectionOfDataNull(widget)) {
            return true;
        }
        if (widget.getData().getCollection() == null) {
            LOGGER.error("CollectionRefSpecification of Data is not found in widget " + widget.getIdentifier());
            return true;
        }
        return false;
    }

    public static boolean isCollectionRefOfCollectionNull(DashboardWidgetType widget) {
        if (isDataNull(widget)) {
            return true;
        }
        if (isCollectionOfDataNull(widget)) {
            return true;
        }
        ObjectReferenceType ref = widget.getData().getCollection().getCollectionRef();
        if (ref == null) {
            LOGGER.error("CollectionRef of collection is not found in widget " + widget.getIdentifier());
            return true;
        }
        return false;
    }

    public static boolean isDataFieldsOfPresentationNullOrEmpty(DashboardWidgetPresentationType presentation) {
        if(presentation != null) {
            if(presentation.getDataField() != null) {
                if(!presentation.getDataField().isEmpty()) {
                    return false;
                } else {
                    LOGGER.error("DataField of presentation is empty");
                }
            } else {
                LOGGER.error("DataField of presentation is not defined");
            }
        } else {
            LOGGER.error("Presentation of widget is not defined");
        }

        return true;
    }

    public static String getQueryForListRecords(String query) {
        query = query + AUDIT_RECORDS_ORDER_BY;
        LOGGER.debug("Query for select: " + query);
        return query;
    }

    public static boolean isAuditCollection(CollectionRefSpecificationType collectionRef, ModelService modelService, Task task, OperationResult result) {
        if (collectionRef == null) {
            return false;
        }
        if (collectionRef.getCollectionRef() != null && collectionRef.getCollectionRef().getOid() != null) {
            try {
                @NotNull PrismObject<ObjectCollectionType> collection = modelService.getObject(ObjectCollectionType.class,
                        collectionRef.getCollectionRef().getOid(), null, task, result);
                if (collection != null && QNameUtil.match(collection.asObjectable().getType(), AuditEventRecordType.COMPLEX_TYPE)) {
                    return true;
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't get object collection from oid " + collectionRef.getCollectionRef().getOid());
            }
        }
        if (collectionRef.getBaseCollectionRef() != null && collectionRef.getBaseCollectionRef().getCollectionRef() != null
                && collectionRef.getBaseCollectionRef().getCollectionRef().getOid() != null) {
            try {
                @NotNull PrismObject<ObjectCollectionType> collection = modelService.getObject(ObjectCollectionType.class,
                        collectionRef.getBaseCollectionRef().getCollectionRef().getOid(), null, task, result);
                if (collection != null && QNameUtil.match(collection.asObjectable().getType(), AuditEventRecordType.COMPLEX_TYPE)) {
                    return true;
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't get object collection from oid " + collectionRef.getCollectionRef().getOid());
            }
        }
        return false;
    }

}
