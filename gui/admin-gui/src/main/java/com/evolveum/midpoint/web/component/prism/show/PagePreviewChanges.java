/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.workflow.PageAdminWorkItems;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/workItem", encoder = OnePageParameterEncoder.class, action = {
		@AuthorizationAction(actionUri = PageAdminWorkItems.AUTH_WORK_ITEMS_ALL,
				label = PageAdminWorkItems.AUTH_WORK_ITEMS_ALL_LABEL,
				description = PageAdminWorkItems.AUTH_WORK_ITEMS_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_WORK_ITEM_URL,
				label = "PageWorkItem.auth.workItem.label",
				description = "PageWorkItem.auth.workItem.description")})
public class PagePreviewChanges extends PageAdminWorkItems {		// TODO extends

	private static final String ID_DATA_CONTEXT_PANEL = "dataContextPanel";
	private static final String ID_BACK = "back";

	private static final Trace LOGGER = TraceManager.getTrace(PagePreviewChanges.class);

	private IModel<DataSceneDto> contextDtoModel;

	public PagePreviewChanges(ModelContext<? extends ObjectType> modelContext, ModelInteractionService modelInteractionService) {
		List<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
		deltas.add(modelContext.getFocusContext().getPrimaryDelta());
		deltas.add(modelContext.getFocusContext().getSecondaryDelta());
		for (ModelProjectionContext projCtx : modelContext.getProjectionContexts()) {
			deltas.add(projCtx.getPrimaryDelta());
			deltas.add(projCtx.getSecondaryDelta());
		}
		LOGGER.info("Deltas:\n{}", DebugUtil.debugDump(deltas));

		Task task = createSimpleTask("visualize");
		final Scene scene = modelInteractionService.visualizeDeltas(deltas, task, task.getResult());
		LOGGER.info("Creating context DTO for:\n{}", scene.debugDump());

		final DataSceneDto context = new DataSceneDto(scene);
		contextDtoModel = new AbstractReadOnlyModel<DataSceneDto>() {
			@Override
			public DataSceneDto getObject() {
				return context;
			}
		};
		initLayout();
	}

	private void initLayout() {
		Form mainForm = new Form("mainForm");
		mainForm.setMultiPart(true);
		add(mainForm);

		mainForm.add(new DataScenePanel(ID_DATA_CONTEXT_PANEL, contextDtoModel, mainForm, this));
		initButtons(mainForm);
	}

	private void initButtons(Form mainForm) {
		AjaxButton cancel = new AjaxButton(ID_BACK, createStringResource("pageAccount.button.back")) {		// TODO key
			@Override
			public void onClick(AjaxRequestTarget target) {
				cancelPerformed(target);
			}
		};
		mainForm.add(cancel);
	}

	private void cancelPerformed(AjaxRequestTarget target) {
		goBack(PageDashboard.class);
	}

}
