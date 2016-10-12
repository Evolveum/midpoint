package com.evolveum.midpoint.web.page.admin.reports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordProvider;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditSearchDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

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

	private List<AuditEventRecord> auditEventRecordList;

	Map<String, Object> params = new HashMap<>();

	private static final String ID_PARAMETERS_PANEL = "parametersPanel";
	private static final String ID_TABLE = "table";
	private static final String ID_FROM = "fromField";
	private static final String ID_TO = "toField";
	private static final String ID_INITIATOR_NAME = "initiatorNameField";
	private static final String ID_CHANNEL = "channelField";
	private static final String ID_HOST_IDENTIFIER = "hostIdentifierField";
	private static final String ID_TARGET_NAME = "targetNameField";
	private static final String ID_TARGET_TYPE = "targetTypeField";
	private static final String ID_TARGET_OWNER_NAME = "targetOwnerNameField";
	private static final String ID_EVENT_TYPE = "eventTypeField";
	private static final String ID_EVENT_STAGE = "eventStageField";
	private static final String ID_OUTCOME = "outcomeField";
	
	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_SEARCH_BUTTON = "searchButton";

	/*
	private IModel<XMLGregorianCalendar> fromModel;
	private IModel<XMLGregorianCalendar> toModel;
	private IModel<String> initiatorNameModel;
	private IModel<String> channelModel;
	private IModel<String> channelListModel;
	 */

	private IModel<AuditSearchDto> auditSearchDto;

	public PageAuditLogViewer(){
		/*
		fromModel = new Model();
		toModel = new Model();
		initiatorNameModel = new Model();
		channelModel = new Model();
		channelListModel = new ListModel(new WebComponentUtil().getChannelList());
		 */
		auditSearchDto = new Model(new AuditSearchDto());
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

		IModel<XMLGregorianCalendar> fromModel = new PropertyModel<>(auditSearchDto, AuditSearchDto.F_FROM_GREG);
		DatePanel from = new DatePanel(ID_FROM, fromModel);
		from.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		from.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		from.setOutputMarkupId(true);
		parametersPanel.add(from);

		IModel<XMLGregorianCalendar> toModel = new PropertyModel<>(auditSearchDto, AuditSearchDto.F_TO_GREG);
		DatePanel to = new DatePanel(ID_TO, toModel);
		to.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		to.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		to.setOutputMarkupId(true);
		parametersPanel.add(to);

		IModel<String> initiatorNameModel = new PropertyModel<>(auditSearchDto, AuditSearchDto.F_INITIATOR_NAME);
		TextPanel initiatorName = new TextPanel(ID_INITIATOR_NAME, initiatorNameModel);
		initiatorName.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		initiatorName.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		initiatorName.setOutputMarkupId(true);
		parametersPanel.add(initiatorName);

		IModel<String> channelListModel = new ListModel(new WebComponentUtil().getChannelList());
		IModel<String> channelModel = new PropertyModel<>(auditSearchDto, AuditSearchDto.F_CHANNEL);
		DropDownChoicePanel channel = new DropDownChoicePanel(ID_CHANNEL, channelModel, channelListModel);
		channel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		channel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		channel.setOutputMarkupId(true);
		parametersPanel.add(channel);

		IModel<String> hostIdentifierModel = new PropertyModel<>(auditSearchDto, AuditSearchDto.F_HOST_IDENTIFIER);
		TextPanel hostIdentifier = new TextPanel(ID_HOST_IDENTIFIER, hostIdentifierModel);
		hostIdentifier.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		hostIdentifier.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		hostIdentifier.setOutputMarkupId(true);
		parametersPanel.add(hostIdentifier);

		IModel<String> targetNameModel = new PropertyModel<>(auditSearchDto, AuditSearchDto.F_TARGET_NAME);
		TextPanel targetName = new TextPanel(ID_TARGET_NAME, targetNameModel);
		targetName.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		targetName.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		targetName.setOutputMarkupId(true);
		parametersPanel.add(targetName);

		IModel<String> targetTypeModel = new PropertyModel<>(auditSearchDto, AuditSearchDto.F_TARGET_TYPE);
		TextPanel targetType = new TextPanel(ID_TARGET_TYPE, targetTypeModel);
		targetType.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		targetType.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		targetType.setOutputMarkupId(true);
		parametersPanel.add(targetType);

		IModel<String> targetOwnerNameModel = new PropertyModel<>(auditSearchDto, AuditSearchDto.F_TARGET_OWNER_NAME);
		TextPanel targetOwnerName = new TextPanel(ID_TARGET_OWNER_NAME, targetOwnerNameModel);
		targetOwnerName.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		targetOwnerName.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		targetOwnerName.setOutputMarkupId(true);
		parametersPanel.add(targetOwnerName);

		IModel<String> eventTypeModel = new PropertyModel<>(auditSearchDto, AuditSearchDto.F_EVENT_TYPE);
		TextPanel eventType = new TextPanel(ID_EVENT_TYPE, eventTypeModel);
		eventType.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		eventType.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		eventType.setOutputMarkupId(true);
		parametersPanel.add(eventType);

		IModel<String> eventStageModel = new PropertyModel<>(auditSearchDto, AuditSearchDto.F_EVENT_STAGE);
		TextPanel eventStage = new TextPanel(ID_EVENT_STAGE, eventStageModel);
		eventStage.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		eventStage.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		eventStage.setOutputMarkupId(true);
		parametersPanel.add(eventStage);

		IModel<String> outcomeModel = new PropertyModel<>(auditSearchDto, AuditSearchDto.F_OUTCOME);
		TextPanel outcome = new TextPanel(ID_OUTCOME, outcomeModel);
		outcome.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		outcome.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		outcome.setOutputMarkupId(true);
		parametersPanel.add(outcome);

		AjaxButton ajaxButton = new AjaxButton(ID_SEARCH_BUTTON, createStringResource("BasicSearchPanel.search")) {
			@Override
			public void onClick(AjaxRequestTarget arg0) {
				Form mainForm = (Form)getParent().getParent();
				refreshTable(mainForm);
				arg0.add(mainForm);
				// TODO Auto-generated method stub
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
		/*
		System.out.println("1:" + auditSearchDto.getObject().getFromGreg());
    	System.out.println("1:" + auditSearchDto.getObject().getToGreg());
    	System.out.println("1:" + auditSearchDto.getObject().getInitiatorName());
    	System.out.println("1:" + auditSearchDto.getObject().getChannel());
		 */
		AuditEventRecordProvider provider = new AuditEventRecordProvider(PageAuditLogViewer.this){
			public Map<String, Object> getParameters() {
				Map<String, Object> parameters = new HashMap<String, Object>();
				parameters.put("from", auditSearchDto.getObject().getFromGreg());
				parameters.put("to", auditSearchDto.getObject().getToGreg());
				parameters.put("initiatorName", auditSearchDto.getObject().getInitiatorName());
				parameters.put("channel", auditSearchDto.getObject().getChannel());
				parameters.put("hostIdentifier", auditSearchDto.getObject().getHostIdentifier());
				parameters.put("targetName", auditSearchDto.getObject().getTargetName());
				parameters.put("targetType", auditSearchDto.getObject().getTargetType());
				parameters.put("targetOwnerName", auditSearchDto.getObject().getTargetOwnerName());
				parameters.put("eventType", auditSearchDto.getObject().getEventType());
				parameters.put("eventStage", auditSearchDto.getObject().getEventStage());
				parameters.put("outcome", auditSearchDto.getObject().getOutcome());
				/*
		    	System.out.println("2:" + auditSearchDto.getObject().getFromGreg());
		    	System.out.println("2:" + auditSearchDto.getObject().getToGreg());
		    	System.out.println("2:" + auditSearchDto.getObject().getInitiatorName());
		    	System.out.println("2:" + auditSearchDto.getObject().getChannel());
				 */
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
		List<IColumn<AuditEventRecordType, String>> columns = new ArrayList<>();
		IColumn<AuditEventRecordType, String> linkColumn = new LinkColumn<AuditEventRecordType>(createStringResource("PageAuditLogViewer.column.time"), "timestamp"){
			@Override
			public void onClick(AjaxRequestTarget target, IModel<AuditEventRecordType> rowModel) {
				setResponsePage(new PageAuditLogDetails(rowModel.getObject()));
			}
		};
		columns.add(linkColumn);
		IColumn timeColumn = new PropertyColumn(createStringResource("PageAuditLogViewer.column.time"), "timestamp");
		columns.add(timeColumn);
		IColumn initiatorColumn = new PropertyColumn(createStringResource("PageAuditLogViewer.column.initiatorRef"), "initiatorRef");
		columns.add(initiatorColumn);
		IColumn taskIdentifierColumn = new PropertyColumn(createStringResource("PageAuditLogViewer.column.taskIdentifier"), "taskIdentifier");
		columns.add(taskIdentifierColumn);
		IColumn channelColumn = new PropertyColumn(createStringResource("PageAuditLogViewer.column.channel"), "channel");
		columns.add(channelColumn);
		IColumn deltaColumn = new PropertyColumn(createStringResource("PageAuditLogViewer.column.delta"), "delta");
		columns.add(deltaColumn);
		return columns;
	}

}
