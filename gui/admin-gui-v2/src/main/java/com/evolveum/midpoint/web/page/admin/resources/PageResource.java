/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

public class PageResource extends PageAdminResources {

	public static final String PARAM_RESOURCE_ID = "userId";
	private static final String OPERATION_LOAD_RESOURCE = "pageResource.loadResource";
	private ResourceType resourceObj;

	private IModel<ResourceDto> model;

	public PageResource() {
		model = new LoadableModel<ResourceDto>() {

			@Override
			protected ResourceDto load() {
				return loadResource();
			}
		};
		initLayout();
	}

	private ResourceDto loadResource() {
		OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
		PrismObject<ResourceType> resource = null;

		try {
			Collection<PropertyPath> resolve = MiscUtil.createCollection(new PropertyPath(
					ResourceType.F_CONNECTOR));

			TaskManager taskManager = getTaskManager();
			Task task = taskManager.createTaskInstance(OPERATION_LOAD_RESOURCE);

			StringValue resourceOid = getPageParameters().get(PARAM_RESOURCE_ID);
			resource = getModelService().getObject(ResourceType.class, resourceOid.toString(), resolve, task,
					result);

			result.recordSuccess();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get resource.", ex);
		}

		if (!result.isSuccess()) {
			showResult(result);
		}
		resourceObj = resource.asObjectable();
		return new ResourceDto(resourceObj, resource.asObjectable().getConnector());
	}

	@Override
	protected IModel<String> createPageTitleModel() {
		return new LoadableModel<String>(false) {

			@Override
			protected String load() {
				String name = model.getObject().getName();
				return new StringResourceModel("page.title", PageResource.this, null, null, name).getString();
			}
		};
	}

	private void initLayout() {
		initResourceColumns();
		initConnectorDetails();

		AjaxLink<String> link = new AjaxLink<String>("seeDebug",
				createStringResource("pageResource.seeDebug")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				PageParameters parameters = new PageParameters();
				parameters.add(PageDebugView.PARAM_OBJECT_ID, model.getObject().getOid());
				setResponsePage(PageDebugView.class, parameters);
			}
		};
		add(link);
	}

	private void initResourceColumns() {
		add(new Label("resourceOid", new PropertyModel<Object>(model, "oid")));
		add(new Label("resourceName", new PropertyModel<Object>(model, "name")));
		add(new Label("resourceType", new PropertyModel<Object>(model, "type")));
		add(new Label("resourceVersion", new PropertyModel<Object>(model, "version")));
		add(new Label("resourceProgress", new PropertyModel<Object>(model, "progress")));
	}

	private void initConnectorDetails() {
		add(new Image("overallStatus", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				return new PackageResourceReference(PageResource.class, model.getObject().getState()
						.getOverall().getIcon());
			}
		}));

		add(new Image("confValidation", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				return new PackageResourceReference(PageResource.class, model.getObject().getState()
						.getConfValidation().getIcon());
			}
		}));

		add(new Image("conInitialization", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {

				return new PackageResourceReference(PageResource.class, model.getObject().getState()
						.getConInitialization().getIcon());
			}
		}));

		add(new Image("conConnection", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				return new PackageResourceReference(PageResource.class, model.getObject().getState()
						.getConConnection().getIcon());
			}
		}));

		add(new Image("conSanity", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				return new PackageResourceReference(PageResource.class, model.getObject().getState()
						.getConSanity().getIcon());
			}
		}));

		add(new Image("conSchema", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				return new PackageResourceReference(PageResource.class, model.getObject().getState()
						.getConSchema().getIcon());
			}
		}));
	}

	private List<String> initCapabilities() {
		OperationResult result = new OperationResult("Load resource capabilities");
		List<String> capabilitiesName = new ArrayList<String>();
		try {
			List<Object> capabilitiesList = ResourceTypeUtil.listEffectiveCapabilities(resourceObj);

			if (capabilitiesList != null && !capabilitiesList.isEmpty()) {
				for (int i = 0; i < capabilitiesList.size(); i++) {
					capabilitiesName.add(ResourceTypeUtil.getCapabilityDisplayName(capabilitiesList.get(i)));
				}
			}
		} catch (Exception ex) {
			result.recordFatalError("Couldn't load resource capabilities for resource'"
					+ model.getObject().getName() + ".", ex);

		}
		return capabilitiesName;
	}
}
