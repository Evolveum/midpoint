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
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordProvider;
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
	private static final String ID_INITIATOR = "initiatorField";
	private static final String ID_CHANNEL = "channelField";
	private static final String ID_PROPERTY = "propertyField";
	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_SEARCH_BUTTON = "searchButton";


	private IModel<XMLGregorianCalendar> fromModel;
	private IModel<XMLGregorianCalendar> toModel;
	private IModel<String> initiatorModel;
	private IModel<String> channelModel;
	private IModel<String> channelListModel;
	private IModel<String> propertyModel;

	public PageAuditLogViewer(){
		fromModel = new Model();
		toModel = new Model();
		initiatorModel = new Model();
		channelModel = new Model();
		channelListModel = new ListModel(new WebComponentUtil().getChannelList());
		propertyModel = new Model();
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
		
		final DatePanel from = new DatePanel(ID_FROM, fromModel);
		from.setOutputMarkupId(true);
		parametersPanel.add(from);

		final DatePanel to = new DatePanel(ID_TO, toModel);
		to.setOutputMarkupId(true);
		parametersPanel.add(to);

		final TextPanel initiator = new TextPanel(ID_INITIATOR, initiatorModel);
		initiator.setOutputMarkupId(true);
		parametersPanel.add(initiator);
		
		final DropDownChoicePanel channel = new DropDownChoicePanel(ID_CHANNEL, channelModel, channelListModel);
		channel.setOutputMarkupId(true);
		parametersPanel.add(channel);
		
		final TextPanel property = new TextPanel(ID_PROPERTY, propertyModel);
		property.setOutputMarkupId(true);
		parametersPanel.add(property);
		
		AjaxButton ajaxButton = new AjaxButton(ID_SEARCH_BUTTON, createStringResource("BasicSearchPanel.search")) {
			@Override
			public void onClick(AjaxRequestTarget arg0) {
				Form mainForm = (Form)get(ID_MAIN_FORM);
				initTable(mainForm);
				arg0.add(mainForm);
				// TODO Auto-generated method stub
			}
		};
		ajaxButton.setOutputMarkupId(true);
		parametersPanel.add(ajaxButton);
	}

	private void initTable(Form mainForm){
		IModel<List<AuditEventRecordType>> model = new IModel<List<AuditEventRecordType>>() {
			@Override
			public List<AuditEventRecordType> getObject() {
				return getAuditEventRecordList();
			}

			@Override
			public void setObject(List<AuditEventRecordType> auditEventRecord) {

			}

    private void initTable(Form mainForm){
        AuditEventRecordProvider provider = new AuditEventRecordProvider(PageAuditLogViewer.this);
        BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider,
                initColumns(),
                UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER,
                (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER)) {
	private List<IColumn<SelectableBean<AuditEventRecordType>, String>> initColumns() {
		List<IColumn<SelectableBean<AuditEventRecordType>, String>> columns = new ArrayList<>();

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

	private AuditEventRecordType getAuditEventRecordType(AuditEventRecord record){
		AuditEventRecordType recordType = record.createAuditEventRecordType();	
		// AuditEventRecordType newRecord = new AuditEventRecordType();
		// newRecord.setTimestamp(MiscUtil.asXMLGregorianCalendar(new Date(record.getTimestamp())));
		// TODO fill in others fields
		return recordType;
	}

	private String generateFullQuery(String query){
		if (params.get("from") != null) {
			query += "(aer.timestamp >= :from) and ";
		} else {
			params.remove("from");
		}
		if (params.get("to") != null) {
			query += "(aer.timestamp <= :to) and ";
		} else {
			params.remove("to");
		}
		if (params.get("eventType") != null) {
			query += "(aer.eventType = :eventType) and ";
		} else {
			params.remove("eventType");
		}
		if (params.get("eventStage") != null) {
			query += "(aer.eventStage = :eventStage) and ";
		} else {
			params.remove("eventStage");
		}
		if (params.get("outcome") != null) {
			query += "(aer.outcome = :outcome) and ";
		} else {
			params.remove("outcome");
		}
		if (params.get("initiatorName") != null) {
			query += "(aer.initiatorName = :initiatorName) and ";
		} else {
			params.remove("initiatorName");
		}
		if (params.get("targetName") != null) {
			query += "(aer.targetName = :targetName) and ";
		} else {
			params.remove("targetName");
		}

        return columns;
    }

}
