/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.util.DashboardUtils;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * Created by honchar.
 */
public class AuditEventRecordProvider extends BaseSortableDataProvider<AuditEventRecordType> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Trace LOGGER = TraceManager.getTrace(BaseSortableDataProvider.class);

    public static final String PARAMETER_VALUE_REF_TARGET_NAMES = "valueRefTargetNames";
    public static final String PARAMETER_CHANGED_ITEM = "changedItem";
    public static final String PARAMETER_FROM = "from";
    public static final String PARAMETER_TO = "to";
    public static final String PARAMETER_EVENT_TYPE = "eventType";
    public static final String PARAMETER_EVENT_STAGE = "eventStage";
    public static final String PARAMETER_OUTCOME = "outcome";
    public static final String PARAMETER_INITIATOR_NAME = "initiatorName";
    public static final String PARAMETER_CHANNEL = "channel";
    public static final String PARAMETER_HOST_IDENTIFIER = "hostIdentifier";
    public static final String PARAMETER_REQUEST_IDENTIFIER = "requestIdentifier";
    public static final String PARAMETER_TARGET_OWNER_NAME = "targetOwnerName";
    public static final String PARAMETER_TARGET_NAMES = "targetNames";
    public static final String PARAMETER_TASK_IDENTIFIER = "taskIdentifier";

    @Nullable private final IModel<ObjectCollectionType> objectCollectionModel;
    @NotNull private final SerializableSupplier<Map<String, Object>> parametersSupplier;

    private static final String AUDIT_RECORDS_QUERY_SELECT = "select * ";
    private static final String AUDIT_RECORDS_QUERY_CORE = " from m_audit_event as aer";
    private static final String AUDIT_RECORDS_QUERY_ITEMS_CHANGED = " right join m_audit_item as item on item.record_id=aer.id ";
    private static final String AUDIT_RECORDS_QUERY_REF_VALUES = " left outer join m_audit_ref_value as rv on rv.record_id=aer.id ";
    private static final String AUDIT_RECORDS_QUERY_COUNT = "select count(*) ";
    private static final String AUDIT_RECORDS_ORDER_BY = " order by aer.";
    private static final String SET_FIRST_RESULT_PARAMETER = "setFirstResult";
    private static final String SET_MAX_RESULTS_PARAMETER = "setMaxResults";

    public static final String TIMESTAMP_VALUE_PARAMETER = "timestampValue";
    public static final String INITIATOR_OID_PARAMETER = "initiatorOid";
    public static final String EVENT_STAGE_PARAMETER = "eventStage";
    public static final String EVENT_TYPE_PARAMETER = "eventType";
    public static final String TARGET_OID_PARAMETER = "targetOid";
    public static final String TARGET_OWNER_OID_PARAMETER = "targetOwnerOid";
    public static final String CHANNEL_PARAMETER = "channel";
    public static final String OUTCOME_PARAMETER = "outcome";
    public static final SortOrder DEFAULT_SORT_ORDER= SortOrder.DESCENDING;

    private static final String DOT_CLASS = AuditEventRecordProvider.class.getName() + ".";
    private static final String OPERATION_COUNT_OBJECTS = DOT_CLASS + "countObjects";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";

//    private static final String TIMESTAMP_VALUE_NAME = "aer.timestamp";

    public AuditEventRecordProvider(Component component, @Nullable IModel<ObjectCollectionType> objectCollectionModel, @NotNull SerializableSupplier<Map<String, Object>> parametersSupplier) {
        super(component);
        this.objectCollectionModel = objectCollectionModel;
        this.parametersSupplier = parametersSupplier;
        initSorting();
    }

    private void initSorting(){
        String sortParameter = "";
        SortOrder sortOrder = null;
        if (getPageStorage() != null && getPageStorage().getPaging() != null && getPageStorage().getPaging().getOrderBy() != null){
            sortParameter = getPageStorage().getPaging().getOrderBy().last() != null ?
                    getPageStorage().getPaging().getOrderBy().last().toString() : "";
        }
        if (getPageStorage() != null && getPageStorage().getPaging() != null && getPageStorage().getPaging().getDirection() != null){
            sortOrder = OrderDirection.ASCENDING.equals(getPageStorage().getPaging().getDirection()) ?
                    SortOrder.ASCENDING : SortOrder.DESCENDING;
        }
        setSort(StringUtils.isNotEmpty(sortParameter) ? sortParameter : TIMESTAMP_VALUE_PARAMETER,
                sortOrder != null ? sortOrder : DEFAULT_SORT_ORDER);
    }

    @Override
    public Iterator<AuditEventRecordType> internalIterator(long first, long count) {
        saveCurrentPage(first, count);
        List<AuditEventRecordType> recordsList = listRecords(true, first, count);
        return recordsList.iterator();
    }

    protected int internalSize() {
        String query;
        String origQuery;
        Map<String, Object> parameters = new HashMap<String, Object>();
        origQuery = DashboardUtils.createQuery(getCollectionForQuery(), parameters, false, getPage().getClock());
        if(StringUtils.isNotBlank(origQuery)) {
            query = generateFullQuery(origQuery, false, true);
        } else {
            parameters = parametersSupplier.get();
            query = generateFullQuery(parameters, false, true);
        }
        int count = 0;
        Task task = getPage().createSimpleTask(OPERATION_COUNT_OBJECTS);
        OperationResult result = task.getResult();
        try {
            count = (int) getAuditService().countObjects(query, parameters, task, result);
        } catch (Exception e) {
            result.recordFatalError(
                    getPage().createStringResource("AuditEventRecordProvider.message.internalSize.fatalError", e.getMessage()).getString(), e);
            LoggingUtils.logException(LOGGER, "Cannot count audit records: " + e.getMessage(), e);
        }

        result.computeStatusIfUnknown();
        getPage().showResult(result, false);
        return count;
     }

    private List<AuditEventRecordType> listRecords(boolean ordered, long first, long count) {
        String query;
        String origQuery;
        Map<String, Object> parameters = new HashMap<String, Object>();
        origQuery = DashboardUtils.createQuery(getCollectionForQuery(), parameters, false, getPage().getClock());
        if(StringUtils.isNotBlank(origQuery)) {
            query = generateFullQuery(origQuery, ordered, false);
        } else {
            parameters = parametersSupplier.get();
            query = generateFullQuery(parameters, ordered, false);
        }

        parameters.put(SET_FIRST_RESULT_PARAMETER, (int) first);
        parameters.put(SET_MAX_RESULTS_PARAMETER, (int) count);

        List<AuditEventRecord> auditRecords = null;
        Task task = getPage().createSimpleTask(OPERATION_SEARCH_OBJECTS);
        OperationResult result = task.getResult();
        try {
            auditRecords = getAuditService().listRecords(query, parameters, task, result);
        } catch (Exception e) {
            result.recordFatalError(
                    getPage().createStringResource("AuditEventRecordProvider.message.listRecords.fatalError", e.getMessage()).getString(), e);
            LoggingUtils.logException(LOGGER, "Cannot search audit records: " + e.getMessage(), e);
        }
        if (auditRecords == null) {
            auditRecords = new ArrayList<>();
        }
        List<AuditEventRecordType> auditRecordList = new ArrayList<>();
        for (AuditEventRecord record : auditRecords){
            auditRecordList.add(record.createAuditEventRecordType());
        }

        result.computeStatusIfUnknown();
        getPage().showResult(result, false);
        return auditRecordList;
    }

    @SuppressWarnings("unused")
    @Nullable
    public ObjectCollectionType getCollectionForQuery() {
        if(objectCollectionModel == null) {
            return null;
        }
        return objectCollectionModel.getObject();
    }

    private String generateFullQuery(Map<String, Object> parameters, boolean ordered, boolean isCount) {
        boolean filteredOnChangedItem = parameters.get(PARAMETER_CHANGED_ITEM) != null;
        boolean filteredOnValueRefTargetNames = filteredOnValueRefTargetNames(parameters);
        List<String> conditions = new ArrayList<>();
        if (parameters.get(PARAMETER_FROM) != null) {
            conditions.add("aer.timestampValue >= :from");
        } else {
            parameters.remove(PARAMETER_FROM);
        }
        if (parameters.get(PARAMETER_TO) != null) {
            conditions.add("aer.timestampValue <= :to");
        } else {
            parameters.remove(PARAMETER_TO);
        }
        if (parameters.get(PARAMETER_EVENT_TYPE) != null) {
            conditions.add("aer.eventType = :eventType");
        } else {
            parameters.remove(PARAMETER_EVENT_TYPE);
        }
        if (parameters.get(PARAMETER_EVENT_STAGE) != null) {
            conditions.add("aer.eventStage = :eventStage");
        } else {
            parameters.remove(PARAMETER_EVENT_STAGE);
        }
        Object outcomeValue = parameters.get(PARAMETER_OUTCOME);
        if (outcomeValue != null) {
            if (outcomeValue != OperationResultStatusType.UNKNOWN) {
                conditions.add("aer.outcome = :outcome");
            } else {
                // this is a bit questionable; but let us do it in this way to ensure compliance with GUI (null is shown as UNKNOWN)
                // see MID-3903
                conditions.add("(aer.outcome = :outcome or aer.outcome is null)");
            }
        } else {
            parameters.remove(PARAMETER_OUTCOME);
        }
        if (parameters.get(PARAMETER_INITIATOR_NAME) != null) {
            conditions.add("aer.initiatorOid = :initiatorName");
        } else {
            parameters.remove(PARAMETER_INITIATOR_NAME);
        }
        if (parameters.get(PARAMETER_CHANNEL) != null) {
            conditions.add("aer.channel = :channel");
        } else {
            parameters.remove(PARAMETER_CHANNEL);
        }
        if (parameters.get(PARAMETER_HOST_IDENTIFIER) != null) {
            conditions.add("aer.hostIdentifier = :hostIdentifier");
        } else {
            parameters.remove(PARAMETER_HOST_IDENTIFIER);
        }
        if (parameters.get(PARAMETER_REQUEST_IDENTIFIER) != null) {
            conditions.add("aer.requestIdentifier = :requestIdentifier");
        } else {
            parameters.remove(PARAMETER_REQUEST_IDENTIFIER);
        }
        if (parameters.get(PARAMETER_TARGET_OWNER_NAME) != null) {
            conditions.add("aer.targetOwnerOid = :targetOwnerName");
        } else {
            parameters.remove(PARAMETER_TARGET_OWNER_NAME);
        }
        if (parameters.get(PARAMETER_TARGET_NAMES) != null) {
            conditions.add("aer.targetOid in ( :targetNames )");
        } else {
            parameters.remove(PARAMETER_TARGET_NAMES);
        }
        if (parameters.get(PARAMETER_TASK_IDENTIFIER) != null) {
            conditions.add("aer.taskIdentifier = :taskIdentifier");
        } else {
            parameters.remove(PARAMETER_TASK_IDENTIFIER);
        }
        if (filteredOnChangedItem) {
            conditions.add("item.changedItemPath = :changedItem");
        } else {
            parameters.remove(PARAMETER_CHANGED_ITEM);
        }
        if (filteredOnValueRefTargetNames) {
            conditions.add("rv.targetName_orig in ( :valueRefTargetNames )");
        } else {
            parameters.remove(PARAMETER_VALUE_REF_TARGET_NAMES);
        }
        ObjectCollectionType collection = getCollectionForQuery();
        String query = "";
        if (collection == null || collection.getAuditSearch() == null
                || collection.getAuditSearch().getRecordQuery() == null) {
            query = AUDIT_RECORDS_QUERY_CORE;
            if (filteredOnChangedItem) {
                query += AUDIT_RECORDS_QUERY_ITEMS_CHANGED;
            }
            if (filteredOnValueRefTargetNames) {
                query += AUDIT_RECORDS_QUERY_REF_VALUES;
            }
            if (!conditions.isEmpty()) {
                query += " where ";
            }
        } else {
            query = collection.getAuditSearch().getRecordQuery();
        }
        if (isCount) {
            query = AUDIT_RECORDS_QUERY_COUNT + query;
        } else {
            query = AUDIT_RECORDS_QUERY_SELECT + query;
        }
        query += conditions.stream().collect(Collectors.joining(" and "));
        if (ordered) {
            query += getQueryOrderByPart();
        }
        return query;
    }

    private String generateFullQuery(String origQuery, boolean ordered, boolean isCount) {
        String query = origQuery;
        if (isCount) {
            int index = query.toLowerCase().indexOf("from");
            query = AUDIT_RECORDS_QUERY_COUNT + query.substring(index);
        }
        if (ordered) {
            query += getQueryOrderByPart();
        }
        return query;
    }

    private boolean filteredOnValueRefTargetNames(Map<String, Object> parameters2) {
        return valueRefTargetIsNotEmpty(parameters2.get(PARAMETER_VALUE_REF_TARGET_NAMES));
    }

    private boolean valueRefTargetIsNotEmpty(Object valueRefTargetNamesParam) {
        if (valueRefTargetNamesParam instanceof String) {
            return StringUtils.isNotBlank((String)valueRefTargetNamesParam);
        } else if (valueRefTargetNamesParam instanceof Collection) {
            return CollectionUtils.isNotEmpty((Collection)valueRefTargetNamesParam);
        } else {
            return valueRefTargetNamesParam != null;
        }
    }

    @Override
    protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
        if (getPageStorage() != null){
            getPageStorage().setPaging(paging);
        }
    }

    @Override
    protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
        if (sortParam != null && sortParam.getProperty() != null) {
            OrderDirection order = sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING;
            return Collections.singletonList(
                    getPrismContext().queryFactory().createOrdering(
                            ItemPath.create(new QName(sortParam.getProperty())), order));
        } else {
            return Collections.emptyList();
        }
    }

    protected void saveCurrentPage(long from, long count) {
    }

    protected PageStorage getPageStorage(){
        return null;
    }

    private String getQueryOrderByPart() {
        SortParam<String> sortParam = getSort();
        if (sortParam != null && sortParam.getProperty() != null) {
            String sortOrder = sortParam.isAscending() ? "asc" : "desc";
            return AUDIT_RECORDS_ORDER_BY + sortParam.getProperty() + " " + sortOrder;
        }
        return AUDIT_RECORDS_ORDER_BY + TIMESTAMP_VALUE_PARAMETER + " desc";
    }
}
