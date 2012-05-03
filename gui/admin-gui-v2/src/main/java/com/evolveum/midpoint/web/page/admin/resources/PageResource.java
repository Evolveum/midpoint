package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import org.apache.wicket.model.StringResourceModel;

public class PageResource extends PageAdminResources {

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
        //todo implement
        return new ResourceDto();
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

    }
}
