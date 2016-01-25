package com.evolveum.midpoint.web.page.admin.resources;

import javax.xml.namespace.QName;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class ResourceSummaryPanel extends ObjectSummaryPanel<ResourceType>{


	public ResourceSummaryPanel(String id, IModel<PrismObject<ResourceType>> model) {
		super(id, model);
		
		boolean down = ResourceTypeUtil.isDown(model.getObject().asObjectable());
		Label summaryTag  = new Label("summaryTag", down ? "DOWN" : "UP");
		((WebMarkupContainer) get("summaryBox")).add(summaryTag);
	}
	
	@Override
	protected String getIconCssClass() {
		return "fa fa-laptop";
	}

	@Override
	protected String getIconBoxAdditionalCssClass() {
		return "summary-panel-resource";
	}

	@Override
	protected String getBoxAdditionalCssClass() {
		return "summary-panel-resource";
	}

	@Override
	protected QName getDisplayNamePropertyName() {
		return ResourceType.F_NAME;
	}

}
