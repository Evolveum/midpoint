/*
 * Copyright (c) 2010-2017 Evolveum
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ResourceContentResourcePanel extends ResourceContentPanel {
	private static final long serialVersionUID = 1L;

	private static final String DOT_CLASS = ResourceContentResourcePanel.class.getName() + ".";

	public ResourceContentResourcePanel(String id, IModel<PrismObject<ResourceType>> resourceModel,
			QName objectClass, ShadowKindType kind, String intent, String searchMode, PageBase pageBase) {
		super(id, resourceModel, objectClass, kind, intent, searchMode, pageBase);
	}

	@Override
	protected SelectorOptions<GetOperationOptions> addAdditionalOptions() {
		return null;
	}

	@Override
	protected boolean isUseObjectCounting() {
		return ResourceTypeUtil.isCountObjectsCapabilityEnabled(getResourceModel().getObject().asObjectable());
	}

	@Override
	protected Search createSearch() {
		Map<ItemPath, ItemDefinition> availableDefs = new HashMap<>();
		availableDefs.putAll(createAttributeDefinitionList());
		return new Search(ShadowType.class, availableDefs);
	}

	private <T extends ObjectType> Map<ItemPath, ItemDefinition> createAttributeDefinitionList() {

		Map<ItemPath, ItemDefinition> map = new HashMap<>();

		RefinedObjectClassDefinition ocDef = null;
		try {

			if (getKind() != null) {

				ocDef = getDefinitionByKind();

			} else if (getObjectClass() != null) {
				ocDef = getDefinitionByObjectClass();

			}
		} catch (SchemaException e) {
			warn("Could not get determine object class definition");
			return map;
		}

		if (ocDef == null) {
			return map;
		}
		
		ItemPath attributePath = new ItemPath(ShadowType.F_ATTRIBUTES);

		for (ItemDefinition def : (List<ItemDefinition>) ocDef.getDefinitions()) {
			if (!(def instanceof PrismPropertyDefinition) && !(def instanceof PrismReferenceDefinition)) {
				continue;
			}

			map.put(new ItemPath(attributePath, def.getName()), def);
		}

		return map;
	}

	@Override
	protected ModelExecuteOptions createModelOptions() {
		return null;
	}

	@Override
	protected void initShadowStatistics(WebMarkupContainer totals) {
		totals.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isVisible() {
				return false;
			}
		});
		
	}

}
