/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.model.api.authentication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CollectionSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DistinctSearchOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectListViewAdditionalPanelsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxConfigurationType;

/**
 * @author semancik
 *
 */
@Experimental
public class CompiledObjectCollectionView implements DebugDumpable, Serializable {
	private static final long serialVersionUID = 1L;
	
	private final QName objectType;
	private final String viewName;
	
	private List<GuiActionType> actions = new ArrayList<>();
	private CollectionSpecificationType collection;
	private List<GuiObjectColumnType> columns = new ArrayList<>();
	private DisplayType display;
	private GuiObjectListViewAdditionalPanelsType additionalPanels;
	private DistinctSearchOptionType distinct;
	private Boolean disableSorting;
	private SearchBoxConfigurationType searchBoxConfiguration;
	
	// Only used to construct "default" view definition. May be not needed later on.
	public CompiledObjectCollectionView() {
		super();
		objectType = null;
		viewName = null;
	}

	public CompiledObjectCollectionView(QName objectType, String viewName) {
		super();
		this.objectType = objectType;
		this.viewName = viewName;
	}

	public QName getObjectType() {
		return objectType;
	}

	public String getViewName() {
		return viewName;
	}

	@NotNull
	public List<GuiActionType> getActions() {
		return actions;
	}

	public CollectionSpecificationType getCollection() {
		return collection;
	}

	public void setCollection(CollectionSpecificationType collection) {
		this.collection = collection;
	}

	/**
	 * Returns column definition list (already ordered).
	 * May return empty list if there is no definition. Which means that default columns should be used.
	 */
	public List<GuiObjectColumnType> getColumns() {
		return columns;
	}
	
	public DisplayType getDisplay() {
		return display;
	}

	public void setDisplay(DisplayType display) {
		this.display = display;
	}

	public GuiObjectListViewAdditionalPanelsType getAdditionalPanels() {
		return additionalPanels;
	}
		
	public void setAdditionalPanels(GuiObjectListViewAdditionalPanelsType additionalPanels) {
		this.additionalPanels = additionalPanels;
	}

	public DistinctSearchOptionType getDistinct() {
		return distinct;
	}
	
	public void setDistinct(DistinctSearchOptionType distinct) {
		this.distinct = distinct;
	}

	public Boolean isDisableSorting() {
		return disableSorting;
	}
	
	public Boolean getDisableSorting() {
		return disableSorting;
	}

	public void setDisableSorting(Boolean disableSorting) {
		this.disableSorting = disableSorting;
	}
	
	public SearchBoxConfigurationType getSearchBoxConfiguration() {
		return searchBoxConfiguration;
	}

	public void setSearchBoxConfiguration(SearchBoxConfigurationType searchBoxConfiguration) {
		this.searchBoxConfiguration = searchBoxConfiguration;
	}

	public boolean match(QName expectedObjectType, String expectedViewName) {
		if (!QNameUtil.match(objectType, expectedObjectType)) {
			return false;
		}
		if (expectedViewName == null) {
			if (isAllObjectsView()) {
				return true;
			} else {
				return false;
			}
		}
		return expectedViewName.equals(viewName);
	}
	
	public boolean match(QName expectedObjectType) {
		return QNameUtil.match(objectType, expectedObjectType);
	}

	
	private boolean isAllObjectsView() {
		return collection == null;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(CompiledObjectCollectionView.class, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "objectType", objectType, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "viewName", viewName, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "actions", actions, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "columns", columns, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "display", display, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "additionalPanels", additionalPanels, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "distinct", distinct, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "disableSorting", disableSorting, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "searchBoxConfiguration", searchBoxConfiguration, indent + 1);
		// TODO
		return sb.toString();
	}
	
}
