package com.evolveum.midpoint.web.page.admin.reports;

import java.util.*;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordItemValueDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordProvider;
import com.evolveum.midpoint.web.session.AuditLogStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.*;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.delta.ObjectDeltaOperationPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;

import javax.xml.datatype.XMLGregorianCalendar;


@PageDescriptor(url = "/admin/auditLogDetails", action = {
        @AuthorizationAction(actionUri = PageAdminReports.AUTH_REPORTS_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_AUDIT_LOG_VIEWER_URL,
                label = "PageAuditLogViewer.auth.auditLogViewer.label",
                description = "PageAuditLogViewer.auth.auditLogViewer.description")})
public class PageAuditLogDetails extends PageBase{

    private static final Trace LOGGER = TraceManager.getTrace(PageAuditLogDetails.class);

    private static final long serialVersionUID = 1L;
    private static final String ID_EVENT_PANEL = "eventPanel";

    private static final String ID_DELTA_LIST_PANEL = "deltaListPanel";
    private static final String ID_OBJECT_DELTA_OP_PANEL ="objectDeltaOpPanel";
    private static final String ID_EVENT_DETAILS_PANEL = "eventDetailsPanel";
    private static final String ID_PARAMETERS_TIMESTAMP = "timestamp";
    private static final String ID_PARAMETERS_EVENT_IDENTIFIER = "eventIdentifier";
    private static final String ID_PARAMETERS_SESSION_IDENTIFIER = "sessionIdentifier";
    private static final String ID_PARAMETERS_TASK_IDENTIFIER = "taskIdentifier";
    private static final String ID_PARAMETERS_TASK_OID = "taskOID";
    private static final String ID_PARAMETERS_HOST_IDENTIFIER = "hostIdentifier";
    private static final String ID_PARAMETERS_EVENT_INITIATOR = "initiatorRef";
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
    private static final String ID_HISTORY_PANEL = "historyPanel";

    private static final String ID_BUTTON_BACK = "back";
    private static final String TASK_IDENTIFIER_PARAMETER = "taskIdentifier";
    private static final int TASK_EVENTS_TABLE_SIZE = 10;

    private static final String OPERATION_RESOLVE_REFENRENCE_NAME = PageAuditLogDetails.class.getSimpleName()
            + ".resolveReferenceName()";
    private IModel<AuditEventRecordType> recordModel;

    public PageAuditLogDetails() {
        AuditLogStorage storage = getSessionStorage().getAuditLog();
        initModel(storage.getAuditRecord());
        initLayout();
    }

    public PageAuditLogDetails(AuditEventRecordType recordType) {
        initModel(recordType);
        initLayout();
    }

    private void initModel(AuditEventRecordType recordType){
        AuditLogStorage storage = getSessionStorage().getAuditLog();
        storage.setAuditRecord(recordType);
        recordModel = new LoadableModel<AuditEventRecordType>(false) {

            @Override
            protected AuditEventRecordType load() {
                return recordType;
            }
        };
    }
    private void initLayout(){
        WebMarkupContainer eventPanel = new WebMarkupContainer(ID_EVENT_PANEL);
        eventPanel.setOutputMarkupId(true);
        add(eventPanel);
        initAuditLogHistoryPanel(eventPanel);
        initEventPanel(eventPanel);
        initDeltasPanel(eventPanel);
        initLayoutBackButton();
    }

    private void initAuditLogHistoryPanel(WebMarkupContainer eventPanel){
        AuditEventRecordProvider provider = new AuditEventRecordProvider(PageAuditLogDetails.this){
            private static final long serialVersionUID = 1L;

            public Map<String, Object> getParameters() {
                Map<String, Object> parameters = new HashMap<String, Object>();
                parameters.put(TASK_IDENTIFIER_PARAMETER, recordModel.getObject().getTaskIdentifier());
                return parameters;
            }
        };


        BoxedTablePanel<AuditEventRecordType> table = new BoxedTablePanel<AuditEventRecordType>(
                ID_HISTORY_PANEL, provider, initColumns(), UserProfileStorage.TableId.TASK_EVENTS_TABLE, TASK_EVENTS_TABLE_SIZE) {

            @Override
            protected Item<AuditEventRecordType> customizeNewRowItem(final Item<AuditEventRecordType> item,
                                                                     final IModel<AuditEventRecordType> rowModel) {

                if (rowModel.getObject().getTimestamp().equals(recordModel.getObject().getTimestamp())){
                    item.add(new AttributeAppender("style", "background-color: #eee; border-color: #d6d6d6; color: #000"));
                }

                item.add(new AjaxEventBehavior("click") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        PageAuditLogDetails.this.rowItemClickPerformed(target, item, rowModel);
                    }
                });
                return item;
            }
        };
        table.getFooterMenu().setVisible(false);
        table.getFooterCountLabel().setVisible(false);
        //TODO hidden temporarily
        table.setVisible(false);
        table.setOutputMarkupId(true);
        table.setAdditionalBoxCssClasses("without-box-header-top-border");
        eventPanel.addOrReplace(table);

    }

    protected void rowItemClickPerformed(AjaxRequestTarget target,
                                         Item<AuditEventRecordType> item, final IModel<AuditEventRecordType> rowModel){
        recordModel.setObject(rowModel.getObject());
        AuditLogStorage storage = getSessionStorage().getAuditLog();
        storage.setAuditRecord(rowModel.getObject());
        WebMarkupContainer eventPanel = (WebMarkupContainer)PageAuditLogDetails.this.get(ID_EVENT_PANEL);
        initAuditLogHistoryPanel(eventPanel);
        initEventPanel(eventPanel);
        initDeltasPanel(eventPanel);
        target.add(eventPanel);

    }

    private List<IColumn<AuditEventRecordType, String>> initColumns() {
        List<IColumn<AuditEventRecordType, String>> columns = new ArrayList<>();
        PropertyColumn<AuditEventRecordType, String> timeColumn = new PropertyColumn<AuditEventRecordType, String>
                (createStringResource("AuditEventRecordType.timestamp"),
                        AuditEventRecordType.F_TIMESTAMP.getLocalPart()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                     IModel<AuditEventRecordType> rowModel) {
                XMLGregorianCalendar time = rowModel.getObject().getTimestamp();
                item.add(new Label(componentId, WebComponentUtil.getLocalizedDate(time, DateLabelComponent.SHORT_SHORT_STYLE)));
            }
        };
        columns.add(timeColumn);
        PropertyColumn<AuditEventRecordType, String> stageColumn = new PropertyColumn<AuditEventRecordType, String>
                (createStringResource("PageAuditLogViewer.eventStageShortLabel"),
                        AuditEventRecordType.F_EVENT_STAGE.getLocalPart()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                     IModel<AuditEventRecordType> rowModel) {
                AuditEventStageType stage = rowModel.getObject().getEventStage();
                String shortStage  = "";
                if (AuditEventStageType.EXECUTION.equals(stage)){
                    shortStage = AuditEventStageType.EXECUTION.value().substring(0, 4);
                } else if (AuditEventStageType.REQUEST.equals(stage)){
                    shortStage = AuditEventStageType.REQUEST.value().substring(0, 3);
                }
                item.add(new Label(componentId, shortStage));
            }
        };
        columns.add(stageColumn);
        PropertyColumn<AuditEventRecordType, String> typeColumn = new PropertyColumn<AuditEventRecordType, String>
                (createStringResource("PageAuditLogViewer.eventTypeShortLabel"),
                        AuditEventRecordType.F_EVENT_TYPE.getLocalPart()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AuditEventRecordType>> item, String componentId,
                                     IModel<AuditEventRecordType> rowModel) {
                //TODO create some proper short values
                AuditEventTypeType type = rowModel.getObject().getEventType();
                String typeVal = type.value().substring(0, 4);
                item.add(new Label(componentId, typeVal));
            }
        };
        columns.add(typeColumn);

        return columns;
    }

    private void initEventPanel(WebMarkupContainer eventPanel){

        WebMarkupContainer eventDetailsPanel = new WebMarkupContainer(ID_EVENT_DETAILS_PANEL);
        eventDetailsPanel.setOutputMarkupId(true);
        eventPanel.addOrReplace(eventDetailsPanel);

        final Label identifier = new Label(ID_PARAMETERS_EVENT_IDENTIFIER , new PropertyModel(recordModel,ID_PARAMETERS_EVENT_IDENTIFIER));
        identifier.setOutputMarkupId(true);
        eventDetailsPanel.add(identifier);

        final Label timestamp = new Label(ID_PARAMETERS_TIMESTAMP , new PropertyModel(recordModel,ID_PARAMETERS_TIMESTAMP));
        timestamp.setOutputMarkupId(true);
        eventDetailsPanel.add(timestamp);

        final Label sessionIdentifier = new Label(ID_PARAMETERS_SESSION_IDENTIFIER , new PropertyModel(recordModel,ID_PARAMETERS_SESSION_IDENTIFIER));
        sessionIdentifier.setOutputMarkupId(true);
        eventDetailsPanel.add(sessionIdentifier);

        final Label taskIdentifier = new Label(ID_PARAMETERS_TASK_IDENTIFIER , new PropertyModel(recordModel,ID_PARAMETERS_TASK_IDENTIFIER));
        taskIdentifier.setOutputMarkupId(true);
        eventDetailsPanel.add(taskIdentifier);

        final Label taskOID = new Label(ID_PARAMETERS_TASK_OID , new PropertyModel(recordModel,ID_PARAMETERS_TASK_OID));
        taskOID.setOutputMarkupId(true);
        eventDetailsPanel.add(taskOID);

        final Label hostIdentifier = new Label(ID_PARAMETERS_HOST_IDENTIFIER , new PropertyModel(recordModel,ID_PARAMETERS_HOST_IDENTIFIER));
        hostIdentifier.setOutputMarkupId(true);
        eventDetailsPanel.add(hostIdentifier);

        final Label initiatorRef = new Label(ID_PARAMETERS_EVENT_INITIATOR,
                new Model(WebModelServiceUtils.resolveReferenceName(recordModel.getObject().getInitiatorRef(), this,
                        createSimpleTask(ID_PARAMETERS_EVENT_INITIATOR),
                        new OperationResult(ID_PARAMETERS_EVENT_INITIATOR))));
        initiatorRef.setOutputMarkupId(true);
        eventDetailsPanel.add(initiatorRef);

        final Label targetRef = new Label(ID_PARAMETERS_EVENT_TARGET,
                new Model(WebModelServiceUtils.resolveReferenceName(recordModel.getObject().getTargetRef(), this,
                        createSimpleTask(ID_PARAMETERS_EVENT_TARGET),
                        new OperationResult(ID_PARAMETERS_EVENT_TARGET))));
        targetRef.setOutputMarkupId(true);
        eventDetailsPanel.add(targetRef);

        IModel<String> targetOwnerRefModel = new IModel<String>() {
            @Override
            public String getObject() {
                return WebModelServiceUtils.resolveReferenceName(recordModel.getObject().getTargetOwnerRef(),
                        PageAuditLogDetails.this,
                        createSimpleTask(OPERATION_RESOLVE_REFENRENCE_NAME),
                        new OperationResult(OPERATION_RESOLVE_REFENRENCE_NAME));
            }

            @Override
            public void setObject(String s) {

            }

            @Override
            public void detach() {

            }
        };
        final Label targetOwnerRef = new Label(ID_PARAMETERS_EVENT_TARGET_OWNER , targetOwnerRefModel);
        targetOwnerRef.setOutputMarkupId(true);
        eventDetailsPanel.add(targetOwnerRef);

        final Label eventType = new Label(ID_PARAMETERS_EVENT_TYPE , new PropertyModel(recordModel,ID_PARAMETERS_EVENT_TYPE));
        eventType.setOutputMarkupId(true);
        eventDetailsPanel.add(eventType);

        final Label eventStage = new Label(ID_PARAMETERS_EVENT_STAGE , new PropertyModel(recordModel,ID_PARAMETERS_EVENT_STAGE));
        eventStage.setOutputMarkupId(true);
        eventDetailsPanel.add(eventStage);

        final Label channel = new Label(ID_PARAMETERS_CHANNEL , new PropertyModel(recordModel,ID_PARAMETERS_CHANNEL));
        channel.setOutputMarkupId(true);
        eventDetailsPanel.add(channel);

        final Label eventOutcome = new Label(ID_PARAMETERS_EVENT_OUTCOME , new PropertyModel(recordModel,ID_PARAMETERS_EVENT_OUTCOME));
        eventOutcome.setOutputMarkupId(true);
        eventDetailsPanel.add(eventOutcome);

        final Label eventResult = new Label(ID_PARAMETERS_EVENT_RESULT , new PropertyModel(recordModel,ID_PARAMETERS_EVENT_RESULT));
        eventResult.setOutputMarkupId(true);
        eventDetailsPanel.add(eventResult);

        final Label parameter = new Label(ID_PARAMETERS_PARAMETER , new PropertyModel(recordModel,ID_PARAMETERS_PARAMETER));
        parameter.setOutputMarkupId(true);
        eventDetailsPanel.add(parameter);

        final Label message = new Label(ID_PARAMETERS_MESSAGE , new PropertyModel(recordModel,ID_PARAMETERS_MESSAGE));
        message.setOutputMarkupId(true);
        eventDetailsPanel.add(message);

//		AuditEventRecordPropertyType p = new AuditEventRecordPropertyType();
//		p.setName("PageResetPasswordConfirmation");
//		p.getValue().add("val1");
//		p.getValue().add("val2");
//		recordModel.getObject().getProperty().add(p);
//		AuditEventRecordPropertyType q = new AuditEventRecordPropertyType();
//		q.setName("property-q");
//		q.getValue().add(null);
//		recordModel.getObject().getProperty().add(q);
//		AuditEventRecordPropertyType q2 = new AuditEventRecordPropertyType();
//		q2.setName("ref");
//		q2.getValue().add(null);
//		recordModel.getObject().getProperty().add(q2);
//		AuditEventRecordReferenceType r = new AuditEventRecordReferenceType();
//		r.setName("ref");
//		AuditEventRecordReferenceValueType v = new AuditEventRecordReferenceValueType();
//		v.setOid("123");
//		v.setTargetName("object123");
//		r.getValue().add(v);
//		recordModel.getObject().getReference().add(r);

		ListView<AuditEventRecordItemValueDto> additionalItemsList = new ListView<AuditEventRecordItemValueDto>(
				ID_ADDITIONAL_ITEM_LINE,
				new AbstractReadOnlyModel<List<AuditEventRecordItemValueDto>>() {
					@Override
					public List<AuditEventRecordItemValueDto> getObject() {
						List<AuditEventRecordItemValueDto> rv = new ArrayList<>();
						AuditEventRecordType record = recordModel.getObject();
						// it is a multi map because there can be both property and reference with the same name
						// (also, more properties/references can have the same display name)
						// TODO take locale into account when sorting
						TreeMap<String, List<AuditEventRecordItemType>> sortedItems = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
						record.getProperty().forEach(p -> put(sortedItems, getDisplayName(p.getName()), p));
						record.getReference().forEach(p -> put(sortedItems, getDisplayName(p.getName()), p));
						for (Map.Entry<String, List<AuditEventRecordItemType>> entry : sortedItems.entrySet()) {
							for (AuditEventRecordItemType item : entry.getValue()) {
								String currentName = entry.getKey();
								if (item instanceof AuditEventRecordPropertyType) {
									for (String value : ((AuditEventRecordPropertyType) item).getValue()) {
										rv.add(new AuditEventRecordItemValueDto(currentName, value));
										currentName = null;
									}
								} else if (item instanceof AuditEventRecordReferenceType) {
									for (AuditEventRecordReferenceValueType value : ((AuditEventRecordReferenceType) item).getValue()) {
										rv.add(new AuditEventRecordItemValueDto(currentName, value.getTargetName() != null ?
												value.getTargetName() : value.getOid()));
										currentName = null;
									}
								} else {
									// should not occur
								}
							}
						}
						return rv;
					}

					private void put(TreeMap<String, List<AuditEventRecordItemType>> items, String displayName, AuditEventRecordItemType item) {
						items.computeIfAbsent(displayName, k -> new ArrayList<>()).add(item);
					}

					private String getDisplayName(String nameKey) {
						// null should not occur so we don't try to be nice when displaying it
						return nameKey != null ? createStringResource(nameKey).getString() : "(null)";
					}
				}) {
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

    private void initDeltasPanel(WebMarkupContainer eventPanel){
        List<ObjectDeltaOperationType> deltas = recordModel.getObject().getDelta();
        RepeatingView deltaScene = new RepeatingView(ID_DELTA_LIST_PANEL);

        for(ObjectDeltaOperationType deltaOp :deltas){
            ObjectDeltaOperationPanel deltaPanel = new ObjectDeltaOperationPanel(deltaScene.newChildId(), Model.of(deltaOp), this);
            deltaPanel.setOutputMarkupId(true);
            deltaScene.add(deltaPanel);


        }
        eventPanel.addOrReplace(deltaScene);

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
