/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.MultiButtonPanel;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.reports.component.AuditLogViewerPanel;
import com.evolveum.midpoint.web.session.SessionStorage;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.gui.api.component.delta.ObjectDeltaOperationPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordItemValueDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordProvider;
import com.evolveum.midpoint.web.session.AuditLogStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.wf.api.WorkflowConstants;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@PageDescriptor(url = "/admin/auditLogDetails", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_AUDIT_LOG_VIEWER_URL,
                label = "PageAuditLogViewer.auth.auditLogViewer.label",
                description = "PageAuditLogViewer.auth.auditLogViewer.description") })
public class PageAuditLogDetails extends PageBase {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageAuditLogDetails.class);

    private static final String ID_EVENT_PANEL = "eventPanel";

    private static final String ID_DELTA_LIST_PANEL = "deltaListPanel";
    private static final String ID_DELTA_PANEL = "delta";
    private static final String ID_EVENT_DETAILS_PANEL = "eventDetailsPanel";
    private static final String ID_PARAMETERS_TIMESTAMP = "timestamp";
    private static final String ID_PARAMETERS_EVENT_IDENTIFIER = "eventIdentifier";
    private static final String ID_PARAMETERS_SESSION_IDENTIFIER = "sessionIdentifier";
    private static final String ID_PARAMETERS_TASK_IDENTIFIER = "taskIdentifier";
    private static final String ID_PARAMETERS_REQUEST_IDENTIFIER = "requestIdentifier";
    private static final String ID_PARAMETERS_TASK_OID_LABEL = "taskOIDLabel";
    private static final String ID_PARAMETERS_TASK_OID_LINK = "taskOIDLink";
    private static final String ID_PARAMETERS_HOST_IDENTIFIER = "hostIdentifier";
    private static final String ID_PARAMETERS_NODE_IDENTIFIER = "nodeIdentifier";
    private static final String ID_PARAMETERS_REMOTE_HOST_ADDRESS = "remoteHostAddress";
    private static final String ID_PARAMETERS_EVENT_INITIATOR = "initiatorRef";
    private static final String ID_PARAMETERS_EVENT_ATTORNEY = "attorneyRef";
    private static final String ID_PARAMETERS_EVENT_TARGET = "targetRef";
    private static final String ID_PARAMETERS_EVENT_TARGET_OWNER = "targetOwnerRef";
    private static final String ID_PARAMETERS_EVENT_TYPE = "eventType";
    private static final String ID_PARAMETERS_EVENT_STAGE = "eventStage";
    private static final String ID_PARAMETERS_CHANNEL = "channel";
    private static final String ID_PARAMETERS_EVENT_OUTCOME = "outcome";
    private static final String ID_PARAMETERS_EVENT_RESULT = "result";
    private static final String ID_PARAMETERS_PARAMETER = "parameter";
    private static final String ID_PARAMETERS_MESSAGE = "message";
    private static final String ID_ADDITIONAL_ITEMS = "additionalItems";
    private static final String ID_ADDITIONAL_ITEM_LINE = "additionalItemLine";
    private static final String ID_ITEM_NAME = "itemName";
    private static final String ID_ITEM_VALUE = "itemValue";
//    private static final String ID_HISTORY_PANEL = "historyPanel";

    private static final String ID_BUTTON_BACK = "back";
    private static final int TASK_EVENTS_TABLE_SIZE = 10;

    private static final String OPERATION_RESOLVE_REFERENCE_NAME = PageAuditLogDetails.class.getSimpleName()
            + ".resolveReferenceName()";
    private static final String OPERATION_LOAD_AUDIT_RECORD = PageAuditLogDetails.class.getSimpleName() + ".loadAuditRecord";
    private IModel<AuditEventRecordType> recordModel;

    // items that are not listed here are sorted according to their display name
    private static final List<String> EXTENSION_ITEMS_ORDER =
            Arrays.asList(
                    WorkflowConstants.AUDIT_OBJECT,
                    WorkflowConstants.AUDIT_TARGET,
                    WorkflowConstants.AUDIT_ORIGINAL_ASSIGNEE,
                    WorkflowConstants.AUDIT_CURRENT_ASSIGNEE,
                    WorkflowConstants.AUDIT_STAGE_NUMBER,
                    WorkflowConstants.AUDIT_STAGE_COUNT,
                    WorkflowConstants.AUDIT_STAGE_NAME,
                    WorkflowConstants.AUDIT_STAGE_DISPLAY_NAME,
                    WorkflowConstants.AUDIT_ESCALATION_LEVEL_NUMBER,
                    WorkflowConstants.AUDIT_ESCALATION_LEVEL_NAME,
                    WorkflowConstants.AUDIT_ESCALATION_LEVEL_DISPLAY_NAME,
                    WorkflowConstants.AUDIT_REQUESTER_COMMENT,
                    WorkflowConstants.AUDIT_COMMENT,
                    WorkflowConstants.AUDIT_WORK_ITEM_ID,
                    WorkflowConstants.AUDIT_PROCESS_INSTANCE_ID);

    public PageAuditLogDetails(PageParameters params) {
        if (params != null) {
            getPageParameters().overwriteWith(params);
        }
        initAuditModel();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();

    }

    private void initAuditModel() {
        recordModel = new LoadableModel<AuditEventRecordType>(false) {

            @Override
            protected AuditEventRecordType load() {

                StringValue param = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
                if (param == null) {
                    return null;
                }
                String eventIdentifier = param.toString();
                ObjectQuery query = getPrismContext().queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_EVENT_IDENTIFIER)
                        .eq(eventIdentifier)
                        .build();
                OperationResult result = new OperationResult(OPERATION_LOAD_AUDIT_RECORD);
                SearchResultList<AuditEventRecordType> records = null;
                try {
                    records = getAuditService().searchObjects(query, null, result);
                    result.computeStatusIfUnknown();
                } catch (SchemaException e) {
                    LOGGER.error("Cannot get audit record, reason: {}", e.getMessage(), e);
                    result.recordFatalError("Cannot get audit record, reason: " + e.getMessage(), e);
                }

                showResult(result, false);
                if (records == null) {
                    return null;
                }

                if (records.size() > 1) {
                    return null;
                }

                return records.iterator().next();
            }
        };
    }

    private void initLayout() {
        WebMarkupContainer eventPanel = new WebMarkupContainer(ID_EVENT_PANEL);
        eventPanel.setOutputMarkupId(true);
        add(eventPanel);
//        initAuditLogHistoryPanel(eventPanel);
        initEventPanel(eventPanel);
        initDeltasPanel(eventPanel);
        initLayoutBackButton();
    }

//    private void initAuditLogHistoryPanel(WebMarkupContainer eventPanel) {
//
//        AuditLogViewerPanel panel = new AuditLogViewerPanel(ID_HISTORY_PANEL) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            protected ObjectQuery getCustomizeContentQuery(){
//                return getPageBase().getPrismContext().queryFor(AuditEventRecordType.class)
//                        .item(AuditEventRecordType.F_TASK_IDENTIFIER)
//                        .eq(recordModel.getObject().getTaskIdentifier())
//                        .build();
//            }
//
//            @Override
//            protected String getAuditStorageKey(String collectionNameValue) {
//                if (StringUtils.isNotEmpty(collectionNameValue)) {
//                    return SessionStorage.KEY_EVENT_DETAIL_AUDIT_LOG + "." + collectionNameValue;
//                }
//                return SessionStorage.KEY_EVENT_DETAIL_AUDIT_LOG;
//            }
//        };
//        panel.setOutputMarkupId(true);
//        panel.add(new VisibleBehaviour(() -> recordModel.getObject() != null && recordModel.getObject().getTaskIdentifier() != null));
//        eventPanel.addOrReplace(panel);
//    }

    private void initEventPanel(WebMarkupContainer eventPanel) {

        WebMarkupContainer eventDetailsPanel = new WebMarkupContainer(ID_EVENT_DETAILS_PANEL);
        eventDetailsPanel.setOutputMarkupId(true);
        eventPanel.addOrReplace(eventDetailsPanel);

        final Label identifier = new Label(ID_PARAMETERS_EVENT_IDENTIFIER, new PropertyModel(recordModel, ID_PARAMETERS_EVENT_IDENTIFIER));
        identifier.setOutputMarkupId(true);
        eventDetailsPanel.add(identifier);

        final Label timestamp = new Label(ID_PARAMETERS_TIMESTAMP, new PropertyModel(recordModel, ID_PARAMETERS_TIMESTAMP));
        timestamp.setOutputMarkupId(true);
        eventDetailsPanel.add(timestamp);

        final Label sessionIdentifier = new Label(ID_PARAMETERS_SESSION_IDENTIFIER, new PropertyModel(recordModel, ID_PARAMETERS_SESSION_IDENTIFIER));
        sessionIdentifier.setOutputMarkupId(true);
        eventDetailsPanel.add(sessionIdentifier);

        final Label taskIdentifier = new Label(ID_PARAMETERS_TASK_IDENTIFIER, new PropertyModel(recordModel, ID_PARAMETERS_TASK_IDENTIFIER));
        taskIdentifier.setOutputMarkupId(true);
        eventDetailsPanel.add(taskIdentifier);

        IModel<TaskType> taskModel = new LoadableModel<TaskType>(false) {

            @Override
            protected TaskType load() {
                PrismObject<TaskType> task;
                if (recordModel != null && recordModel.getObject() != null && StringUtils.isNotEmpty(recordModel.getObject().getTaskOID())) {
                    List<PrismObject<TaskType>> tasks = WebModelServiceUtils.searchObjects(TaskType.class,
                            getPrismContext().queryFor(TaskType.class).id(recordModel.getObject().getTaskOID())
                                    .build(),
                            new OperationResult("search task by oid"), PageAuditLogDetails.this);
                    if (tasks != null && !tasks.isEmpty()) {
                        task = tasks.get(0);
                        if (task != null) {
                            return task.asObjectable();
                        }
                    }
                }
                return null;
            }
        };

        Label taskOidLabel = new Label(ID_PARAMETERS_TASK_OID_LABEL, new PropertyModel(recordModel, "taskOID"));

        AjaxLinkPanel taskOidLink = new AjaxLinkPanel(ID_PARAMETERS_TASK_OID_LINK, createTaskNameModel(taskModel)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                TaskType finalTask = taskModel.getObject();
                if (finalTask != null) {
                    WebComponentUtil.dispatchToObjectDetailsPage(ObjectTypeUtil.createObjectRef(finalTask, getPrismContext()), this, false);
                }
            }
        };

        taskOidLabel.setOutputMarkupId(true);
        eventDetailsPanel.add(taskOidLabel);
        taskOidLink.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return taskModel != null && taskModel.getObject() != null;
            }
        });
        taskOidLink.setOutputMarkupId(true);
        eventDetailsPanel.add(taskOidLink);

        final Label requestIdentifier = new Label(ID_PARAMETERS_REQUEST_IDENTIFIER, new PropertyModel(recordModel, ID_PARAMETERS_REQUEST_IDENTIFIER));
        requestIdentifier.setOutputMarkupId(true);
        eventDetailsPanel.add(requestIdentifier);

        final Label hostIdentifier = new Label(ID_PARAMETERS_HOST_IDENTIFIER, new PropertyModel(recordModel, ID_PARAMETERS_HOST_IDENTIFIER));
        hostIdentifier.setOutputMarkupId(true);
        eventDetailsPanel.add(hostIdentifier);

        final Label nodeIdentifier = new Label(ID_PARAMETERS_NODE_IDENTIFIER, new PropertyModel(recordModel, ID_PARAMETERS_NODE_IDENTIFIER));
        nodeIdentifier.setOutputMarkupId(true);
        eventDetailsPanel.add(nodeIdentifier);

        final Label remoteHostAddress = new Label(ID_PARAMETERS_REMOTE_HOST_ADDRESS, new PropertyModel(recordModel, ID_PARAMETERS_REMOTE_HOST_ADDRESS));
        remoteHostAddress.setOutputMarkupId(true);
        eventDetailsPanel.add(remoteHostAddress);

        final Label initiatorRef = new Label(ID_PARAMETERS_EVENT_INITIATOR, createInitiatorRefModel());
        initiatorRef.setOutputMarkupId(true);
        eventDetailsPanel.add(initiatorRef);

        final Label attorneyRef = new Label(ID_PARAMETERS_EVENT_ATTORNEY, createAttorneyRefModel());
        attorneyRef.setOutputMarkupId(true);
        eventDetailsPanel.add(attorneyRef);

        final Label targetRef = new Label(ID_PARAMETERS_EVENT_TARGET, createTargetRefModel());
        targetRef.setOutputMarkupId(true);
        eventDetailsPanel.add(targetRef);

        IModel<String> targetOwnerRefModel = new IModel<String>() {
            @Override
            public String getObject() {
                return WebModelServiceUtils.resolveReferenceName(recordModel.getObject().getTargetOwnerRef(),
                        PageAuditLogDetails.this,
                        createSimpleTask(OPERATION_RESOLVE_REFERENCE_NAME),
                        new OperationResult(OPERATION_RESOLVE_REFERENCE_NAME));
            }

            @Override
            public void setObject(String s) {

            }

            @Override
            public void detach() {

            }
        };
        final Label targetOwnerRef = new Label(ID_PARAMETERS_EVENT_TARGET_OWNER, targetOwnerRefModel);
        targetOwnerRef.setOutputMarkupId(true);
        eventDetailsPanel.add(targetOwnerRef);

        final Label eventType = new Label(ID_PARAMETERS_EVENT_TYPE, new PropertyModel(recordModel, ID_PARAMETERS_EVENT_TYPE));
        eventType.setOutputMarkupId(true);
        eventDetailsPanel.add(eventType);

        final Label eventStage = new Label(ID_PARAMETERS_EVENT_STAGE, new PropertyModel(recordModel, ID_PARAMETERS_EVENT_STAGE));
        eventStage.setOutputMarkupId(true);
        eventDetailsPanel.add(eventStage);

        final Label channel = new Label(ID_PARAMETERS_CHANNEL, new PropertyModel(recordModel, ID_PARAMETERS_CHANNEL));
        channel.setOutputMarkupId(true);
        eventDetailsPanel.add(channel);

        final Label eventOutcome = new Label(ID_PARAMETERS_EVENT_OUTCOME, new PropertyModel(recordModel, ID_PARAMETERS_EVENT_OUTCOME));
        eventOutcome.setOutputMarkupId(true);
        eventDetailsPanel.add(eventOutcome);

        final Label eventResult = new Label(ID_PARAMETERS_EVENT_RESULT, new PropertyModel(recordModel, ID_PARAMETERS_EVENT_RESULT));
        eventResult.setOutputMarkupId(true);
        eventDetailsPanel.add(eventResult);

        final Label parameter = new Label(ID_PARAMETERS_PARAMETER, new PropertyModel(recordModel, ID_PARAMETERS_PARAMETER));
        parameter.setOutputMarkupId(true);
        eventDetailsPanel.add(parameter);

        final Label message = new Label(ID_PARAMETERS_MESSAGE, new PropertyModel(recordModel, ID_PARAMETERS_MESSAGE));
        message.setOutputMarkupId(true);
        eventDetailsPanel.add(message);

        ListView<AuditEventRecordItemValueDto> additionalItemsList = new ListView<AuditEventRecordItemValueDto>(
                ID_ADDITIONAL_ITEM_LINE, createAdditionalItemsListModel()) {

            @Override
            protected void populateItem(ListItem<AuditEventRecordItemValueDto> item) {
                item.add(new Label(ID_ITEM_NAME, new PropertyModel<String>(item.getModel(), AuditEventRecordItemValueDto.F_NAME)));
                item.add(new Label(ID_ITEM_VALUE, new PropertyModel<String>(item.getModel(), AuditEventRecordItemValueDto.F_VALUE)));
            }
        };
        WebMarkupContainer additionalItemsContainer = new WebMarkupContainer(ID_ADDITIONAL_ITEMS);
        additionalItemsContainer.add(additionalItemsList);
        additionalItemsContainer.add(new VisibleBehaviour(() -> !additionalItemsList.getModelObject().isEmpty()));
        eventDetailsPanel.add(additionalItemsContainer);
    }

    private IModel<String> createTaskNameModel(IModel<TaskType> taskModel) {
        return new ReadOnlyModel<>(() -> {
            TaskType task = taskModel.getObject();
            if (task == null) {
                return "";
            }
            return " " + WebComponentUtil.getName(task);
        });
    }

    private IModel<String> createInitiatorRefModel() {
        return new ReadOnlyModel<>(() -> WebModelServiceUtils.resolveReferenceName(recordModel.getObject().getInitiatorRef(), PageAuditLogDetails.this));
    }

    private IModel<String> createAttorneyRefModel() {
        return new ReadOnlyModel<>(() -> WebModelServiceUtils.resolveReferenceName(recordModel.getObject().getAttorneyRef(), PageAuditLogDetails.this,
                createSimpleTask(ID_PARAMETERS_EVENT_ATTORNEY),
                new OperationResult(ID_PARAMETERS_EVENT_ATTORNEY)));
    }

    private IModel<String> createTargetRefModel() {
        return new ReadOnlyModel<>(() -> WebModelServiceUtils.resolveReferenceName(
                recordModel.getObject().getTargetRef(),
                this,
                createSimpleTask(ID_PARAMETERS_EVENT_TARGET),
                new OperationResult(ID_PARAMETERS_EVENT_TARGET)));
    }

    private IModel<List<AuditEventRecordItemValueDto>> createAdditionalItemsListModel() {
        return new IModel<List<AuditEventRecordItemValueDto>>() {
            @Override
            public List<AuditEventRecordItemValueDto> getObject() {
                List<AuditEventRecordItemValueDto> rv = new ArrayList<>();
                for (AuditEventRecordItemType item : getSortedItems()) {
                    String currentName = getDisplayName(item.getName());
                    if (item instanceof AuditEventRecordPropertyType) {
                        for (String value : ((AuditEventRecordPropertyType) item).getValue()) {
                            rv.add(new AuditEventRecordItemValueDto(currentName, value));
                            currentName = null;
                        }
                    } else if (item instanceof AuditEventRecordReferenceType) {
                        for (AuditEventRecordReferenceValueType value : ((AuditEventRecordReferenceType) item).getValue()) {
                            rv.add(new AuditEventRecordItemValueDto(currentName, value.getTargetName() != null ?
                                    value.getTargetName().getOrig() : value.getOid()));
                            currentName = null;
                        }
                    } else {
                        // should not occur
                    }
                }
                return rv;
            }

            // TODO take locale into account when sorting
            private List<AuditEventRecordItemType> getSortedItems() {
                AuditEventRecordType record = recordModel.getObject();
                List<AuditEventRecordItemType> rv = new ArrayList<>();
                rv.addAll(record.getProperty());
                rv.addAll(record.getReference());
                rv.sort((a, b) -> {
                    // explicitly enumerated are shown first; others are sorted by display name
                    int indexA = EXTENSION_ITEMS_ORDER.indexOf(a.getName());
                    int indexB = EXTENSION_ITEMS_ORDER.indexOf(b.getName());
                    if (indexA != -1 && indexB != -1) {
                        return Integer.compare(indexA, indexB);
                    } else if (indexA != -1) {
                        return -1;
                    } else if (indexB != -1) {
                        return 1;
                    }
                    String nameA = getDisplayName(a.getName());
                    String nameB = getDisplayName(b.getName());
                    return String.CASE_INSENSITIVE_ORDER.compare(nameA, nameB);
                });
                return rv;
            }

            private String getDisplayName(String nameKey) {
                // null should not occur so we don't try to be nice when displaying it
                return nameKey != null ? createStringResource(nameKey).getString() : "(null)";
            }
        };
    }

    private void initDeltasPanel(WebMarkupContainer eventPanel) {
        ListView<ObjectDeltaOperationType> deltaScene = new ListView<ObjectDeltaOperationType>(ID_DELTA_LIST_PANEL, createObjectDeltasModel()) {

            @Override
            protected void populateItem(ListItem<ObjectDeltaOperationType> item) {
                ObjectDeltaOperationPanel deltaPanel = new ObjectDeltaOperationPanel(ID_DELTA_PANEL, item.getModel(), PageAuditLogDetails.this) {
                    @Override
                    public boolean getIncludeOriginalObject() {
                        return false;
                    }
                };
                deltaPanel.setOutputMarkupId(true);
                item.add(deltaPanel);
            }
        };
        eventPanel.add(deltaScene);
    }

    private IModel<List<ObjectDeltaOperationType>> createObjectDeltasModel() {
        return new LoadableModel<List<ObjectDeltaOperationType>>(false) {

            @Override
            protected List<ObjectDeltaOperationType> load() {
                List<ObjectDeltaOperationType> deltas = recordModel.getObject().getDelta();
                List<ObjectDeltaOperationType> connectedDeltas = connectDeltas(deltas);
                return connectedDeltas;
            }
        };
    }

    private List<ObjectDeltaOperationType> connectDeltas(List<ObjectDeltaOperationType> deltas) {
        Map<PolyStringType, ObjectDeltaOperationType> focusDeltas = new HashMap<>();
        List<ObjectDeltaOperationType> otherDeltas = new ArrayList<>();
        for (ObjectDeltaOperationType delta : deltas) {
            if (delta != null && delta.getObjectDelta() != null && FocusType.class.isAssignableFrom(WebComponentUtil.qnameToClass(getPrismContext(), delta.getObjectDelta().getObjectType()))) {
                if (focusDeltas.containsKey(delta.getObjectName())) {
                    focusDeltas.get(delta.getObjectName()).setResourceName(null);
                    focusDeltas.get(delta.getObjectName()).setResourceOid(null);
                    if (delta.getObjectDelta() != null) {
                        if (focusDeltas.get(delta.getObjectName()).getObjectDelta() == null) {
                            focusDeltas.get(delta.getObjectName()).setObjectDelta(delta.getObjectDelta());
                        } else {
                            focusDeltas.get(delta.getObjectName()).getObjectDelta().getItemDelta().addAll(delta.getObjectDelta().getItemDelta());
                        }
                        for (ItemDeltaType itemDelta : delta.getObjectDelta().getItemDelta()) {
                            if (itemDelta == null) {
                                continue;
                            }
                        }
                    }
                } else {
                    focusDeltas.put(delta.getObjectName(), delta);
                }
            } else {
                otherDeltas.add(delta);
            }
        }
        List<ObjectDeltaOperationType> retDeltas = new ArrayList<>();
        retDeltas.addAll(focusDeltas.values());
        retDeltas.addAll(otherDeltas);
        return retDeltas;
    }

    protected void initLayoutBackButton() {
        AjaxButton back = new AjaxButton(ID_BUTTON_BACK, createStringResource("PageBase.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                redirectBack();
            }

        };
        add(back);
    }
}
