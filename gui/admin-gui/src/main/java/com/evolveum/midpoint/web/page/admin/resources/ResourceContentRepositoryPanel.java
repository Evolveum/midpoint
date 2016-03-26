package com.evolveum.midpoint.web.page.admin.resources;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

public class ResourceContentRepositoryPanel extends ResourceContentPanel{

	public ResourceContentRepositoryPanel(String id, IModel<PrismObject<ResourceType>> resourceModel,
			ShadowKindType kind, String intent, PageBase pageBase) {
		super(id, resourceModel, kind, intent, pageBase);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void initCustomLayout(IModel<PrismObject<ResourceType>> resource) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected ObjectQuery createQuery(IModel<PrismObject<ResourceType>> resourceModel)
			throws SchemaException {
		ObjectQuery baseQuery = null;
		if (StringUtils.isNotBlank(getIntent())) {
			baseQuery = ObjectQueryUtil.createResourceAndKindIntent(resourceModel.getObject().getOid(),
					getKind(), getIntent(), getPageBase().getPrismContext());
		} else {
			baseQuery = ObjectQueryUtil.createResourceAndKind(resourceModel.getObject().getOid(), getKind(),
					getPageBase().getPrismContext());
		}
		return baseQuery;
	}

	@Override
	protected SelectorOptions<GetOperationOptions> addAdditionalOptions() {
		return new SelectorOptions<GetOperationOptions>(GetOperationOptions.createNoFetch());
	}

	@Override
	protected boolean isUseObjectCounting(IModel<PrismObject<ResourceType>> resourceModel) {
		return true;
	}

}
