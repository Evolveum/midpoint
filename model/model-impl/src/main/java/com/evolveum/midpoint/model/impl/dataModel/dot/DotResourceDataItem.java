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

package com.evolveum.midpoint.model.impl.dataModel.dot;

import com.evolveum.midpoint.model.impl.dataModel.model.ResourceDataItem;
import com.evolveum.midpoint.prism.polystring.PolyString;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class DotResourceDataItem implements DotDataItem {

	private final ResourceDataItem dataItem;
	private final DotModel dotModel;

	public DotResourceDataItem(ResourceDataItem dataItem, DotModel dotModel) {
		this.dataItem = dataItem;
		this.dotModel = dotModel;
	}

	@Override
	public String getNodeName() {
		return "\"" + getResourceName() + ":" + dotModel.getObjectTypeName(dataItem.getRefinedObjectClassDefinition(), false) + ":" + dataItem.getItemPath() + "\"";
	}

	@Override
	public String getNodeLabel() {
		return dataItem.getLastItemName().getLocalPart();
	}

	@Override
	public String getNodeStyleAttributes() {
		return "";
	}

	@NotNull
	public String getResourceName() {
		PolyString name = dotModel.getDataModel().getResource(dataItem.getResourceOid()).getName();
		return name != null ? name.getOrig() : dataItem.getResourceOid();
	}

}
