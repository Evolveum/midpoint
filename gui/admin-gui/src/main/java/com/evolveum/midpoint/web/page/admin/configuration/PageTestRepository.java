package com.evolveum.midpoint.web.page.admin.configuration;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

public class PageTestRepository extends PageAdminConfiguration {
	
	private static final Trace LOGGER = TraceManager.getTrace(PageTestRepository.class);
	
	private LoadableModel model;
	
	public PageTestRepository(){
	
		model = new LoadableModel() {
			@Override
			protected Object load() {
				return null;
			}
		};
		
		final Form mainForm = new Form("mainForm");
		add(mainForm);

		final WebMarkupContainer container = new WebMarkupContainer("container");
		container.setOutputMarkupId(true);
		mainForm.add(container);

		initButtons(container);
	}
	
	
	private void initButtons(final WebMarkupContainer container) {
		AjaxSubmitLinkButton saveFileButton = new AjaxSubmitLinkButton("testRepository",
				createStringResource("pageTestRepository.button.testRepo")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				testRepository(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		container.add(saveFileButton);

	}
	
	private void testRepository(AjaxRequestTarget target){
		Task task = getTaskManager().createTaskInstance("Repostiory self test (GUI)");
		
		OperationResult opResult = getModelDiagnosticService().repositorySelfTest(task);
		
		showResultInSession(opResult);
		target.add(getFeedbackPanel());
		
	}


}
