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
package com.evolveum.midpoint.web.page.admin.resources;

import javax.xml.namespace.QName;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ResourceContentRepositoryPanel extends ResourceContentPanel {
	private static final long serialVersionUID = 1L;

	public ResourceContentRepositoryPanel(String id, IModel<PrismObject<ResourceType>> resourceModel,
			QName objectClass, ShadowKindType kind, String intent, String searchMode, PageBase pageBase) {
		super(id, resourceModel, objectClass, kind, intent, searchMode, pageBase);
	}

	@Override
	protected SelectorOptions<GetOperationOptions> addAdditionalOptions() {
		return new SelectorOptions<GetOperationOptions>(GetOperationOptions.createNoFetch());
	}

	@Override
	protected boolean isUseObjectCounting() {
		return true;
	}

	@Override
	protected Search createSearch() {
		return SearchFactory.createSearch(ShadowType.class, getPageBase().getPrismContext(),
				getPageBase().getModelInteractionService());
	}

	@Override
	protected ModelExecuteOptions createModelOptions() {
		return ModelExecuteOptions.createRaw();
	}

}
