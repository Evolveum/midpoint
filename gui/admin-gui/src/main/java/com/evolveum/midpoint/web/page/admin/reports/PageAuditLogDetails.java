/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.cases.api.AuditingConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_MatchingRuleEntry;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.audit.AuditChangesPanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordItemValueDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/auditLogDetails")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_AUDIT_LOG_DETAILS_URL,
                        label = "PageAuditLogViewer.auth.auditLogViewer.label",
                        description = "PageAuditLogViewer.auth.auditLogViewer.description")})
public class PageAuditLogDetails extends PageBase {
    @Serial private static final long serialVersionUID = 1L;

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
    private static final String ID_PARAMETERS_EVENT_EFFECTIVE_PRINCIPAL = "effectivePrincipalRef";
    private static final String ID_PARAMETERS_EVENT_EFFECTIVE_PRIVILEGES_MODIFICATION = "effectivePrivilegesModification";
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
    private static final String ID_BUTTON_BACK = "back";

    private static final String DOT_CLASS = PageAuditLogDetails.class.getSimpleName() + ".";
    private static final String OPERATION_RESOLVE_REFERENCE_NAME = DOT_CLASS + "resolveReferenceName()";
    private static final String OPERATION_LOAD_AUDIT_RECORD = DOT_CLASS + "loadAuditRecord";
    private static final String OPERATION_LOAD_TASK = DOT_CLASS + "loadTask";
    private IModel<AuditEventRecordType> recordModel;

    // items that are not listed here are sorted according to their display name
    private static final List<String> EXTENSION_ITEMS_ORDER =
            Arrays.asList(
                    AuditingConstants.AUDIT_OBJECT,
                    AuditingConstants.AUDIT_TARGET,
                    AuditingConstants.AUDIT_ORIGINAL_ASSIGNEE,
                    AuditingConstants.AUDIT_CURRENT_ASSIGNEE,
                    AuditingConstants.AUDIT_STAGE_NUMBER,
                    AuditingConstants.AUDIT_STAGE_COUNT,
                    AuditingConstants.AUDIT_STAGE_NAME,
                    AuditingConstants.AUDIT_STAGE_DISPLAY_NAME,
                    AuditingConstants.AUDIT_ESCALATION_LEVEL_NUMBER,
                    AuditingConstants.AUDIT_ESCALATION_LEVEL_NAME,
                    AuditingConstants.AUDIT_ESCALATION_LEVEL_DISPLAY_NAME,
                    AuditingConstants.AUDIT_REQUESTER_COMMENT,
                    AuditingConstants.AUDIT_COMMENT,
                    AuditingConstants.AUDIT_WORK_ITEM_ID,
                    AuditingConstants.AUDIT_CASE_OID,
                    AuditingConstants.AUDIT_PROCESS_INSTANCE_ID);

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
        recordModel = new LoadableModel<>(false) {

            @Override
            protected AuditEventRecordType load() {
                Long repoId = getRepoIdParameter();
                S_MatchingRuleEntry filter = getPrismContext().queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_REPO_ID)
                        .eq(repoId);
                ObjectQuery query = filter.build();
                Task task = createSimpleTask(OPERATION_LOAD_AUDIT_RECORD);
                OperationResult result = task.getResult();
                SearchResultList<AuditEventRecordType> records = null;
                try {
                    records = getModelAuditService().searchObjects(query, null, task, result);
                    result.computeStatusIfUnknown();
                } catch (CommonException e) {
                    LOGGER.error("Cannot get audit record, reason: {}", e.getMessage(), e);
                    result.recordFatalError("Cannot get audit record, reason: " + e.getMessage(), e);
                }

                showResult(result, false);
                if (records == null || records.size() > 1) {
                    getSession().error("Cannot load audit event record, "
                            + (records == null ? "no record found " : "more than one record found ")
                            + ", identifier: " + repoId);
                    throw restartResponseExceptionToReload();
                }

                return records.iterator().next();
            }
        };
    }

    private Long getRepoIdParameter() {
        StringValue param = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        if (param == null) {
            return null;
        }
        return Long.valueOf(param.toString());
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

    private void initEventPanel(WebMarkupContainer eventPanel) {

        WebMarkupContainer eventDetailsPanel = new WebMarkupContainer(ID_EVENT_DETAILS_PANEL);
        eventDetailsPanel.setOutputMarkupId(true);
        eventPanel.addOrReplace(eventDetailsPanel);

        eventDetailsPanel.add(createLabel(ID_PARAMETERS_EVENT_IDENTIFIER, new PropertyModel<>(recordModel, ID_PARAMETERS_EVENT_IDENTIFIER)));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_TIMESTAMP, new PropertyModel<>(recordModel, ID_PARAMETERS_TIMESTAMP)));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_SESSION_IDENTIFIER, new PropertyModel<>(recordModel, ID_PARAMETERS_SESSION_IDENTIFIER)));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_TASK_IDENTIFIER, new PropertyModel<>(recordModel, ID_PARAMETERS_TASK_IDENTIFIER)));
        eventDetailsPanel.add(createTaskLink());
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_TASK_OID_LABEL, new PropertyModel<>(recordModel, "taskOID")));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_REQUEST_IDENTIFIER, new PropertyModel<>(recordModel, ID_PARAMETERS_REQUEST_IDENTIFIER)));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_HOST_IDENTIFIER, new PropertyModel<>(recordModel, ID_PARAMETERS_HOST_IDENTIFIER)));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_NODE_IDENTIFIER, new PropertyModel<>(recordModel, ID_PARAMETERS_NODE_IDENTIFIER)));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_REMOTE_HOST_ADDRESS, new PropertyModel<>(recordModel, ID_PARAMETERS_REMOTE_HOST_ADDRESS)));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_EVENT_INITIATOR, createInitiatorRefModel()));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_EVENT_ATTORNEY, createAttorneyRefModel()));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_EVENT_EFFECTIVE_PRINCIPAL, createEffectivePrincipalRefModel()));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_EVENT_EFFECTIVE_PRIVILEGES_MODIFICATION, new PropertyModel<>(recordModel, AuditEventRecordType.F_EFFECTIVE_PRIVILEGES_MODIFICATION.getLocalPart())));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_EVENT_TARGET, createTargetRefModel()));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_EVENT_TARGET_OWNER, createTargetOwnerRefModel()));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_EVENT_TYPE, new PropertyModel<>(recordModel, ID_PARAMETERS_EVENT_TYPE)));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_EVENT_STAGE, new PropertyModel<>(recordModel, ID_PARAMETERS_EVENT_STAGE)));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_CHANNEL, new PropertyModel<>(recordModel, ID_PARAMETERS_CHANNEL)));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_EVENT_OUTCOME, new PropertyModel<>(recordModel, ID_PARAMETERS_EVENT_OUTCOME)));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_EVENT_RESULT, new PropertyModel<>(recordModel, ID_PARAMETERS_EVENT_RESULT)));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_PARAMETER, new PropertyModel<>(recordModel, ID_PARAMETERS_PARAMETER)));
        eventDetailsPanel.add(createLabel(ID_PARAMETERS_MESSAGE, new PropertyModel<>(recordModel, ID_PARAMETERS_MESSAGE)));

        ListView<AuditEventRecordItemValueDto> additionalItemsList = new ListView<>(ID_ADDITIONAL_ITEM_LINE, createAdditionalItemsListModel()) {

            @Override
            protected void populateItem(ListItem<AuditEventRecordItemValueDto> item) {
                item.add(new Label(ID_ITEM_NAME, () -> item.getModelObject().getName()));
                item.add(new Label(ID_ITEM_VALUE, () -> item.getModelObject().getValue()));

                item.add(new VisibleBehaviour(() -> item.getModelObject().getValue() != null));
            }
        };
        WebMarkupContainer additionalItemsContainer = new WebMarkupContainer(ID_ADDITIONAL_ITEMS);
        additionalItemsContainer.add(additionalItemsList);
        additionalItemsContainer.add(new VisibleBehaviour(() -> !additionalItemsList.getModelObject().isEmpty()));
        eventDetailsPanel.add(additionalItemsContainer);
    }

    private AjaxLinkPanel createTaskLink() {
        IModel<TaskType> taskModel = createTaskModel();

        AjaxLinkPanel taskOidLink = new AjaxLinkPanel(ID_PARAMETERS_TASK_OID_LINK, createTaskNameModel(taskModel)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                TaskType finalTask = taskModel.getObject();
                if (finalTask != null) {
                    DetailsPageUtil.dispatchToObjectDetailsPage(ObjectTypeUtil.createObjectRef(finalTask), this, false);
                }
            }
        };
        taskOidLink.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return taskModel.getObject() != null;
            }
        });
        taskOidLink.setOutputMarkupId(true);
        return taskOidLink;
    }

    private IModel<String> createTargetOwnerRefModel() {
        return () -> {
            AuditEventRecordType record = recordModel.getObject();
            if (record == null) {
                return null;
            }
            return WebModelServiceUtils.resolveReferenceName(record.getTargetOwnerRef(),
                    PageAuditLogDetails.this,
                    createSimpleTask(OPERATION_RESOLVE_REFERENCE_NAME),
                    new OperationResult(OPERATION_RESOLVE_REFERENCE_NAME));
        };
    }

    private Label createLabel(String id, IModel<String> model) {
        Label label = new Label(id, model);
        label.setOutputMarkupId(true);
        return label;
    }

    private LoadableModel<TaskType> createTaskModel() {
        return new LoadableModel<>(false) {

            @Override
            protected TaskType load() {
                String taskOid = getTaskOid(recordModel);
                if (taskOid == null) {
                    return null;
                }
                Task task = createSimpleTask(OPERATION_LOAD_TASK);
                OperationResult result = new OperationResult(OPERATION_LOAD_TASK);
                PrismObject<TaskType> taskPrism = WebModelServiceUtils.loadObject(TaskType.class, taskOid, PageAuditLogDetails.this, task, result);
                if (taskPrism == null) {
                    return null;
                }
                return taskPrism.asObjectable();

            }
        };
    }

    private String getTaskOid(IModel<AuditEventRecordType> recordModel) {
        if (recordModel == null || recordModel.getObject() == null) {
            return null;
        }

        return recordModel.getObject().getTaskOID();
    }

    private IModel<String> createTaskNameModel(IModel<TaskType> taskModel) {
        return () -> {
            TaskType task = taskModel.getObject();
            if (task == null) {
                return "";
            }
            return " " + WebComponentUtil.getName(task);
        };
    }

    private IModel<String> createInitiatorRefModel() {
        AuditEventRecordType record = recordModel.getObject();
        if (record == null) {
            return null;
        }
        return () -> WebModelServiceUtils.resolveReferenceName(record.getInitiatorRef(), PageAuditLogDetails.this);
    }

    private IModel<String> createAttorneyRefModel() {
        return () -> WebModelServiceUtils.resolveReferenceName(
                recordModel.getObject().getAttorneyRef(), PageAuditLogDetails.this,
                createSimpleTask(ID_PARAMETERS_EVENT_ATTORNEY),
                new OperationResult(ID_PARAMETERS_EVENT_ATTORNEY));
    }

    private IModel<String> createEffectivePrincipalRefModel() {
        return () -> WebModelServiceUtils.resolveReferenceName(
                recordModel.getObject().getEffectivePrincipalRef(), PageAuditLogDetails.this,
                createSimpleTask(ID_PARAMETERS_EVENT_EFFECTIVE_PRINCIPAL),
                new OperationResult(ID_PARAMETERS_EVENT_EFFECTIVE_PRINCIPAL));
    }

    private IModel<String> createTargetRefModel() {
        return () -> WebModelServiceUtils.resolveReferenceName(
                recordModel.getObject().getTargetRef(),
                this,
                createSimpleTask(ID_PARAMETERS_EVENT_TARGET),
                new OperationResult(ID_PARAMETERS_EVENT_TARGET));
    }

    private IModel<List<AuditEventRecordItemValueDto>> createAdditionalItemsListModel() {
        return new IModel<>() {
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
                    }
                }
                return rv;
            }

            // TODO take locale into account when sorting
            private List<AuditEventRecordItemType> getSortedItems() {
                AuditEventRecordType record = recordModel.getObject();
                List<AuditEventRecordItemType> rv = new ArrayList<>();
                if (record == null) {
                    return rv;
                }
                rv.addAll(record.getProperty());
                rv.addAll(record.getReference());
                rv.sort((a, b) -> sortItems(a, b));
                return rv;
            }

        };
    }

    private int sortItems(AuditEventRecordItemType a, AuditEventRecordItemType b) {
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
    }

    private String getDisplayName(String nameKey) {
        // null should not occur so we don't try to be nice when displaying it
        return nameKey != null ? createStringResource(nameKey).getString() : "(null)";
    }

    private void initDeltasPanel(WebMarkupContainer eventPanel) {
        ListView<ObjectDeltaOperationType> deltaListPanel = new ListView<>(ID_DELTA_LIST_PANEL, createObjectDeltasModel()) {

            @Override
            protected void populateItem(ListItem<ObjectDeltaOperationType> item) {
                item.add(new AuditChangesPanel(ID_DELTA_PANEL, item.getModel(), PageAuditLogDetails.this));
            }
        };
        eventPanel.add(deltaListPanel);
    }

    private IModel<List<ObjectDeltaOperationType>> createObjectDeltasModel() {
        return new LoadableModel<>(false) {

            @Override
            protected List<ObjectDeltaOperationType> load() {
                AuditEventRecordType record = recordModel.getObject();
                if (record == null) {
                    return new ArrayList<>();
                }
                return record.getDelta();
            }
        };
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
