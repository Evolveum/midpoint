package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.configuration.dto.LoggerConfiguration;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceState;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceStatus;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

public class PageResource extends PageAdminResources {

	public static final String PARAM_RESOURCE_ID = "userId";
	private static final String OPERATION_LOAD_RESOURCE = "pageResource.loadResource";

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
		
		
		return new ResourceDto(resource.asObjectable(), resource.asObjectable().getConnector());
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
		//initConnectorDetails();
		
		AjaxLink<String> link = new AjaxLink<String>("seeDebug", createStringResource("pageResource.seeDebug")) {

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
		ResourceDto dto = model.getObject();
		final ResourceState state = dto.getState();
		
		add(new Image("overallStatus", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				return new PackageResourceReference(PageResource.class, state.getOverall().getIcon());
			}
		}));
		
		add(new Image("confValidation", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				return new PackageResourceReference(PageResource.class, state.getConfValidation().getIcon());
			}
		}));
		
		add(new Image("conInitialization", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				
				return new PackageResourceReference(PageResource.class, state.getConInitialization().getIcon());
			}
		}));
		
		add(new Image("conConnection", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				return new PackageResourceReference(PageResource.class, state.getConConnection().getIcon());
			}
		}));
		
		add(new Image("conSanity", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				return new PackageResourceReference(PageResource.class, state.getConSanity().getIcon());
			}
		}));
		
		add(new Image("conSanity", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				return new PackageResourceReference(PageResource.class, state.getConSchema().getIcon());
			}
		}));
	}
}
