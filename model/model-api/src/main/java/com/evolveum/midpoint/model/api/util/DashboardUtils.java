/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.util;

import java.util.Date;
import java.util.Map;

import javax.xml.datatype.Duration;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.common.Clock;
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

    public static String createQuery(ObjectCollectionType collectionForQuery, Map<String, Object> parameters,
            boolean forDomain, Clock clock) {
        if(collectionForQuery == null) {
            return null;
        }
        AuditSearchType auditSearch = collectionForQuery.getAuditSearch();
        if(auditSearch != null && StringUtils.isNotBlank(auditSearch.getRecordQuery())) {
            Duration interval = auditSearch.getInterval();
            if(interval == null) {
                return auditSearch.getRecordQuery();
            }
            String origQuery = auditSearch.getRecordQuery();
            if(forDomain) {
                origQuery = auditSearch.getDomainQuery();
                if(origQuery == null) {
                    return null;
                }
            }
            String [] partsOfQuery = origQuery.split("where");
            if(interval.getSign() == 1) {
                interval = interval.negate();
            }
            Date date = new Date(clock.currentTimeMillis());
            interval.addTo(date);
            String query = partsOfQuery[0] + "where " + TIMESTAMP_VALUE_NAME + " >= " + ":from" + " ";
            parameters.put(PARAMETER_FROM, date);
            if(partsOfQuery.length > 1) {
                query+= "and" +partsOfQuery[1];
            }
            return query;
        }
        return null;
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
                if (collection != null && collection.asObjectable().getAuditSearch() != null
                        && collection.asObjectable().getAuditSearch().getRecordQuery() != null) {
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
