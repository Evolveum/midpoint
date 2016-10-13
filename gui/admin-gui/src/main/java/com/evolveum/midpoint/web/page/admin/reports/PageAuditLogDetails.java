package com.evolveum.midpoint.web.page.admin.reports;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractPropertyModel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.model.delta.DeltaPanel;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.component.prism.show.WrapperScene;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;


@PageDescriptor(url = "/admin/auditLogDetails", action = {
		@AuthorizationAction(actionUri = PageAdminReports.AUTH_REPORTS_ALL,
				label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
				description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_AUDIT_LOG_VIEWER_URL,
		label = "PageAuditLogViewer.auth.auditLogViewer.label",
		description = "PageAuditLogViewer.auth.auditLogViewer.description")})
public class PageAuditLogDetails extends PageBase{

	private static final Trace LOGGER = TraceManager.getTrace(PageNewReport.class);

	private static final long serialVersionUID = 1L;
	private static final String ID_EVENT_PANEL = "eventPanel";
	private static final String ID_DELTA_PANEL = "deltaPanel";
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

	private static final String ID_PARAMETERS_DELTA = "delta";
	private static final String ID_PARAMETERS_DELTA_SCENE = "deltaScene";
	private static final String ID_PARAMETERS_EXECUTION_RESULT ="executionResult";
	private static final String ID_PARAMETERS_OBJECT_NAME ="objectName";
	private static final String ID_PARAMETERS_RESOURCE_NAME ="resourceName";
	private static final String ID_PARAMETERS_DELTA_PARAMETERS ="deltaParams";

	private List <ObjectDeltaOperationType> deltas = null;








	private IModel<AuditEventRecordType> recordModel;
	//	private IModel<SceneDto> deltaModel;

	public PageAuditLogDetails(final AuditEventRecordType recordType) {

		recordModel = new LoadableModel<AuditEventRecordType>(false) {

			@Override
			protected AuditEventRecordType load() {
				return recordType;
			}
		};



		initLayout();
	}

	private void initLayout(){
		WebMarkupContainer eventPanel = new WebMarkupContainer(ID_EVENT_PANEL);
		initEventPanel(eventPanel);
		initDeltasPanel(eventPanel);
	}

	
	
	


	private void initEventPanel(WebMarkupContainer eventPanel){

		
		eventPanel.setOutputMarkupId(true);
		add(eventPanel);
		WebMarkupContainer eventDetailsPanel = new WebMarkupContainer(ID_EVENT_DETAILS_PANEL);
		eventDetailsPanel.setOutputMarkupId(true);
		eventPanel.add(eventDetailsPanel);

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

		final Label targetOwnerRef = new Label(ID_PARAMETERS_EVENT_TARGET_OWNER , new PropertyModel(recordModel,ID_PARAMETERS_EVENT_TARGET_OWNER));
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
	}
	private void initDeltasPanel(WebMarkupContainer eventPanel){
		List <ObjectDeltaOperationType> deltas = recordModel.getObject().getDelta();
		List<Scene> scenes= new ArrayList<Scene> (deltas.size());
		RepeatingView deltaScene = new RepeatingView(ID_PARAMETERS_DELTA_SCENE);

		for(ObjectDeltaOperationType deltaOp :deltas){
			WebMarkupContainer objectDeltaOperationPanel = new WebMarkupContainer(deltaScene.newChildId());
			ObjectDeltaType od = deltaOp.getObjectDelta();

			Label executionResult = new Label(ID_PARAMETERS_EXECUTION_RESULT, new PropertyModel(deltaOp.getExecutionResult(),ID_PARAMETERS_EXECUTION_RESULT));
			executionResult.setOutputMarkupId(true);
			objectDeltaOperationPanel.add(executionResult);

			Label resourceName = new Label(ID_PARAMETERS_RESOURCE_NAME, new PropertyModel(deltaOp.getExecutionResult(),ID_PARAMETERS_RESOURCE_NAME));
			resourceName.setOutputMarkupId(true);
			objectDeltaOperationPanel.add(resourceName);

			Label objectName = new Label(ID_PARAMETERS_OBJECT_NAME, new PropertyModel(deltaOp.getExecutionResult(),ID_PARAMETERS_OBJECT_NAME));
			objectName.setOutputMarkupId(true);
			objectDeltaOperationPanel.add(objectName);


			ObjectDelta<? extends ObjectType> delta;
			try {
				delta = DeltaConvertor.createObjectDelta(od, getPrismContext());
				Scene scene = getModelInteractionService().visualizeDelta( delta, createSimpleTask(ID_PARAMETERS_DELTA), new OperationResult(ID_PARAMETERS_DELTA));
				final SceneDto deltaSceneDto = new SceneDto(scene);
				deltaSceneDto.setMinimized(true);
				IModel<SceneDto> deltaModel = new AbstractReadOnlyModel<SceneDto>(){
					public SceneDto getObject() {
						return deltaSceneDto;
					}

				};
				ScenePanel deltaPanel = new ScenePanel(ID_DELTA_PANEL, deltaModel);
				deltaPanel.setOutputMarkupId(true);
				objectDeltaOperationPanel.add(deltaPanel);
				
				
			} catch (SchemaException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				LOGGER.error("error when conveting to DeltaObject.");
				throw new RestartResponseException(this);
			}
			deltaScene.add(objectDeltaOperationPanel);
		}
		WebMarkupContainer deltaListPanel = new WebMarkupContainer(ID_DELTA_LIST_PANEL);
		deltaListPanel.add(deltaScene);
		eventPanel.add(deltaListPanel);

	}


}
