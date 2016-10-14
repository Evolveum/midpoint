package com.evolveum.midpoint.web.page.admin.reports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordProvider;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditSearchDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/admin/auditLogViewer", action = {
		@AuthorizationAction(actionUri = PageAdminReports.AUTH_REPORTS_ALL,
				label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
				description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_AUDIT_LOG_VIEWER_URL,
		label = "PageAuditLogViewer.auth.auditLogViewer.label",
		description = "PageAuditLogViewer.auth.auditLogViewer.description")})
public class PageAuditLogViewer extends PageBase{

	private static final long serialVersionUID = 1L;

	private static final String ID_PARAMETERS_PANEL = "parametersPanel";
	private static final String ID_TABLE = "table";
	private static final String ID_FROM = "fromField";
	private static final String ID_TO = "toField";
	private static final String ID_INITIATOR_NAME = "initiatorNameField";
	private static final String ID_CHANNEL = "channelField";
	private static final String ID_HOST_IDENTIFIER = "hostIdentifierField";
	// private static final String ID_TARGET_NAME = "targetNameField";
	// private static final String ID_TARGET_OWNER_NAME = "targetOwnerNameField";
	private static final String ID_EVENT_TYPE = "eventTypeField";
	private static final String ID_EVENT_STAGE = "eventStageField";
	private static final String ID_OUTCOME = "outcomeField";

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_SEARCH_BUTTON = "searchButton";

	private IModel<AuditSearchDto> auditSearchDto;

	public PageAuditLogViewer(){
		auditSearchDto = new Model<AuditSearchDto>(new AuditSearchDto());
		initLayout();
	}

	private void initLayout(){
		Form mainForm = new Form(ID_MAIN_FORM);
		mainForm.setOutputMarkupId(true);
		add(mainForm);
		initParametersPanel(mainForm);
		initTable(mainForm);
	}

	private void initParametersPanel(Form mainForm){
		WebMarkupContainer parametersPanel = new WebMarkupContainer(ID_PARAMETERS_PANEL);
		parametersPanel.setOutputMarkupId(true);
		mainForm.add(parametersPanel);

		PropertyModel<XMLGregorianCalendar> fromModel = new PropertyModel<XMLGregorianCalendar>(auditSearchDto, AuditSearchDto.F_FROM_GREG);
		DatePanel from = new DatePanel(ID_FROM, fromModel);
		from.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		from.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		// from.get("date").add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		// from.get("hours").add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		// from.get("minutes").add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		// from.get("amOrPmChoice").add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		from.setOutputMarkupId(true);
		parametersPanel.add(from);

		PropertyModel<XMLGregorianCalendar> toModel = new PropertyModel<XMLGregorianCalendar>(auditSearchDto, AuditSearchDto.F_TO_GREG);
		DatePanel to = new DatePanel(ID_TO, toModel);
		to.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		to.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		// to.get("date").add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		// to.get("hours").add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		// to.get("minutes").add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		// to.get("amOrPmChoice").add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		to.setOutputMarkupId(true);
		parametersPanel.add(to);

		// PropertyModel<String> initiatorNameModel = new PropertyModel<String>(auditSearchDto, AuditSearchDto.F_INITIATOR_NAME);
		// TextPanel<String> initiatorName = new TextPanel<String>(ID_INITIATOR_NAME, initiatorNameModel);
		// initiatorName.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		// initiatorName.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		// initiatorName.setOutputMarkupId(true);
		// parametersPanel.add(initiatorName);

		PropertyModel<String> hostIdentifierModel = new PropertyModel<String>(auditSearchDto, AuditSearchDto.F_HOST_IDENTIFIER);
		TextPanel<String> hostIdentifier = new TextPanel<String>(ID_HOST_IDENTIFIER, hostIdentifierModel);
		hostIdentifier.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		hostIdentifier.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		hostIdentifier.setOutputMarkupId(true);
		parametersPanel.add(hostIdentifier);

		// PropertyModel<String> targetNameModel = new PropertyModel<String>(auditSearchDto, AuditSearchDto.F_TARGET_NAME);
		// TextPanel<String> targetName = new TextPanel<String>(ID_TARGET_NAME, targetNameModel);
		// targetName.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		// targetName.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		// targetName.setOutputMarkupId(true);
		// parametersPanel.add(targetName);

		// PropertyModel<String> targetOwnerNameModel = new PropertyModel<String>(auditSearchDto, AuditSearchDto.F_TARGET_OWNER_NAME);
		// TextPanel<String> targetOwnerName = new TextPanel<String>(ID_TARGET_OWNER_NAME, targetOwnerNameModel);
		// targetOwnerName.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		// targetOwnerName.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		// targetOwnerName.setOutputMarkupId(true);
		// parametersPanel.add(targetOwnerName);

		ListModel<AuditEventTypeType> eventTypeListModel = new ListModel<AuditEventTypeType>(Arrays.asList(AuditEventTypeType.values()));
		PropertyModel<AuditEventTypeType> eventTypeModel = new PropertyModel<AuditEventTypeType>(auditSearchDto, AuditSearchDto.F_EVENT_TYPE);
		DropDownChoicePanel<AuditEventTypeType> eventType = new DropDownChoicePanel<AuditEventTypeType>(ID_EVENT_TYPE, eventTypeModel, eventTypeListModel, new EnumChoiceRenderer<AuditEventTypeType>(), true);
		eventType.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		eventType.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		eventType.setOutputMarkupId(true);
		parametersPanel.add(eventType);

		ListModel<AuditEventStageType> eventStageListModel = new ListModel<AuditEventStageType>(Arrays.asList(AuditEventStageType.values()));
		PropertyModel<AuditEventStageType> eventStageModel = new PropertyModel<AuditEventStageType>(auditSearchDto, AuditSearchDto.F_EVENT_STAGE);
		DropDownChoicePanel<AuditEventStageType> eventStage = new DropDownChoicePanel<AuditEventStageType>(ID_EVENT_STAGE, eventStageModel, eventStageListModel, new EnumChoiceRenderer<AuditEventStageType>(), true);
		eventStage.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		eventStage.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		eventStage.setOutputMarkupId(true);
		parametersPanel.add(eventStage);

		ListModel<OperationResultStatusType> outcomeListModel = new ListModel<OperationResultStatusType>(Arrays.asList(OperationResultStatusType.values()));
		PropertyModel<OperationResultStatusType> outcomeModel = new PropertyModel<OperationResultStatusType>(auditSearchDto, AuditSearchDto.F_OUTCOME);
		DropDownChoicePanel<OperationResultStatusType> outcome = new DropDownChoicePanel<OperationResultStatusType>(ID_OUTCOME, outcomeModel, outcomeListModel, new EnumChoiceRenderer<OperationResultStatusType>(), true);
		outcome.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		outcome.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		outcome.setOutputMarkupId(true);
		parametersPanel.add(outcome);

		List<String> channelList = WebComponentUtil.getChannelList();
		List<QName> channelQnameList = new ArrayList<QName>();
		for (int i = 0; i < channelList.size(); i++) 
		{
			String channel = channelList.get(i);
			if (channel != null) 
			{
				QName channelQName = QNameUtil.uriToQName(channel);
				channelQnameList.add(channelQName);
			}
		}
		ListModel<QName> channelListModel = new ListModel<QName>(channelQnameList);
		PropertyModel<QName> channelModel = new PropertyModel<QName>(auditSearchDto, AuditSearchDto.F_CHANNEL);
		DropDownChoicePanel<QName> channel = new DropDownChoicePanel<QName>(ID_CHANNEL, channelModel, channelListModel, new QNameChoiceRenderer(), true);
		channel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		channel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		channel.setOutputMarkupId(true);
		parametersPanel.add(channel);

		AjaxButton ajaxButton = new AjaxButton(ID_SEARCH_BUTTON, createStringResource("BasicSearchPanel.search")) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onClick(AjaxRequestTarget arg0) {
				Form mainForm = (Form)getParent().getParent();
				refreshTable(mainForm);
				arg0.add(mainForm);
			}
		};
		ajaxButton.setOutputMarkupId(true);
		parametersPanel.add(ajaxButton);
	}

	private void initTable(Form mainForm){
		AuditEventRecordProvider provider = new AuditEventRecordProvider(PageAuditLogViewer.this);
		BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider,
				initColumns(),
				UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER,
				(int) getItemsPerPage(UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER));
		table.setShowPaging(true);
		table.setOutputMarkupId(true);
		mainForm.addOrReplace(table);
	}

	private void refreshTable(Form mainForm){
		AuditEventRecordProvider provider = new AuditEventRecordProvider(PageAuditLogViewer.this) {
			private static final long serialVersionUID = 1L;
			public Map<String, Object> getParameters() {
				Map<String, Object> parameters = new HashMap<String, Object>();
				// System.out.println(auditSearchDto.getObject().getFromGreg());
				// System.out.println(auditSearchDto.getObject().getToGreg());
				parameters.put("from", auditSearchDto.getObject().getFromGreg());
				parameters.put("to", auditSearchDto.getObject().getToGreg());
				// parameters.put("initiatorName", auditSearchDto.getObject().getInitiatorName());
				if (auditSearchDto.getObject().getChannel() != null) 
				{
					parameters.put("channel", QNameUtil.qNameToUri(auditSearchDto.getObject().getChannel()));
				}
				parameters.put("hostIdentifier", auditSearchDto.getObject().getHostIdentifier());
				// parameters.put("targetName", auditSearchDto.getObject().getTargetName());
				// parameters.put("targetOwnerName", auditSearchDto.getObject().getTargetOwnerName());
				parameters.put("eventType", auditSearchDto.getObject().getEventType());
				parameters.put("eventStage", auditSearchDto.getObject().getEventStage());
				parameters.put("outcome", auditSearchDto.getObject().getOutcome());
				return parameters;
			}
		};
		BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider,
				initColumns(),
				UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER,
				(int) getItemsPerPage(UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER));
		table.setShowPaging(true);
		table.setOutputMarkupId(true);
		mainForm.addOrReplace(table);
	}

	private List<IColumn<AuditEventRecordType, String>> initColumns() {
		List<IColumn<AuditEventRecordType, String>> columns = new ArrayList<IColumn<AuditEventRecordType, String>>();
		IColumn<AuditEventRecordType, String> linkColumn = 
				new LinkColumn<AuditEventRecordType>(createStringResource("PageAuditLogViewer.column.time"), "timestamp") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onClick(AjaxRequestTarget target, IModel<AuditEventRecordType> rowModel) {
				setResponsePage(new PageAuditLogDetails(rowModel.getObject()));
			}
			@Override
			public void populateItem(Item<ICellPopulator<AuditEventRecordType>> cellItem, String componentId,
					IModel<AuditEventRecordType> rowModel) {
				cellItem.add(new AttributeModifier("style", new Model<String>("width: 15%;")));
				super.populateItem(cellItem, componentId, rowModel);
			}
		};
		columns.add(linkColumn);
		// IColumn<AuditEventRecordType, String> timeColumn = new PropertyColumn<AuditEventRecordType, String>(createStringResource("PageAuditLogViewer.column.time"), "timestamp");
		// columns.add(timeColumn);

		IColumn<AuditEventRecordType, String> initiatorColumn = new PropertyColumn<AuditEventRecordType, String>(createStringResource("PageAuditLogViewer.column.initiatorRef"), "initiatorRef") {
			private static final long serialVersionUID = 1L;
			@Override
			public void populateItem(Item item, String componentId, IModel rowModel) {
				AuditEventRecordType auditEventRecordType = (AuditEventRecordType)rowModel.getObject();
				ObjectReferenceType initiatorRef = auditEventRecordType.getInitiatorRef();
				String return_ = WebModelServiceUtils.resolveReferenceName(
						initiatorRef, 
						PageAuditLogViewer.this, 
						createSimpleTask(ID_INITIATOR_NAME), 
						new OperationResult(ID_INITIATOR_NAME));
				item.add(new Label(componentId, return_));
				item.add(new AttributeModifier("style", new Model<String>("width: 10%;")));
			}
		};
		columns.add(initiatorColumn);
		IColumn<AuditEventRecordType, String> taskIdentifierColumn = new PropertyColumn<AuditEventRecordType, String>(createStringResource("PageAuditLogViewer.column.taskIdentifier"), "taskIdentifier") {
			private static final long serialVersionUID = 1L;
			@Override
			public void populateItem(Item item, String componentId, IModel rowModel) {
				item.add(new AttributeModifier("style", new Model<String>("width: 10%;")));
				super.populateItem(item, componentId, rowModel);
			}
		};
		columns.add(taskIdentifierColumn);
		IColumn<AuditEventRecordType, String> channelColumn = new PropertyColumn<AuditEventRecordType, String>(createStringResource("PageAuditLogViewer.column.channel"), "channel") {
			private static final long serialVersionUID = 1L;
			@Override
			public void populateItem(Item item, String componentId, IModel rowModel) {
				AuditEventRecordType auditEventRecordType = (AuditEventRecordType)rowModel.getObject();
				String channel = auditEventRecordType.getChannel();
				if (channel != null) 
				{
					QName channelQName = QNameUtil.uriToQName(channel);
					String return_ = channelQName.getLocalPart();
					item.add(new Label(componentId, return_));
				} 
				else 
				{
					item.add(new Label(componentId, ""));
				}
				item.add(new AttributeModifier("style", new Model<String>("width: 10%;")));
			}
		};
		columns.add(channelColumn);
		IColumn<AuditEventRecordType, String> deltaColumn = new PropertyColumn<AuditEventRecordType, String>(createStringResource("PageAuditLogViewer.column.delta"), "delta"){
			private static final long serialVersionUID = 1L;
			@Override
			public void populateItem(Item item, String componentId, IModel rowModel) {
				RepeatingView repeatingView = new RepeatingView(componentId);
				AuditEventRecordType auditEventRecordType = (AuditEventRecordType)rowModel.getObject();
				List<ObjectDeltaOperationType> deltaList = auditEventRecordType.getDelta();
				for (int i = 0; i < deltaList.size(); i++) 
				{
					ObjectDeltaOperationType objectDeltaOperationType = deltaList.get(i);
					ObjectDeltaType objectDeltaType = objectDeltaOperationType.getObjectDelta();
					try 
					{
						ObjectDelta objectDelta = DeltaConvertor.createObjectDelta(objectDeltaType, getPrismContext());
						repeatingView.add(new Label(repeatingView.newChildId(), objectDelta.toString()));
					} 
					catch (Exception e) 
					{
						repeatingView.add(new Label(repeatingView.newChildId(), 
								"[Convertion from objectDeltaType to ObjectDelta fails]: " + objectDeltaType.toString()));
					}
				}
				item.add(repeatingView);
			}
		};
		columns.add(deltaColumn);
		return columns;
	}

}
