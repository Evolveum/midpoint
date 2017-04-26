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

import com.evolveum.midpoint.model.impl.dataModel.model.RepositoryDataItem;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang.StringUtils;

/**
 * @author mederly
 */
public class DotRepositoryDataItem implements DotDataItem {

	private static final String COLOR_USER = "darkred";
	private static final String COLOR_ROLE = "darkgreen";
	private static final String COLOR_ORG = "darkorange";
	private static final String COLOR_DEFAULT = "black";
	private static final String COLOR_FILL = "grey92";

	RepositoryDataItem dataItem;

	public DotRepositoryDataItem(RepositoryDataItem dataItem) {
		this.dataItem = dataItem;
	}

	@Override
	public String getNodeName() {
		return "\"" + dataItem.getTypeName().getLocalPart() + "." + dataItem.getItemPath() + "\"";
	}

	@Override
	public String getNodeLabel() {
		String entity = StringUtils.removeEnd(dataItem.getTypeName().getLocalPart(), "Type");
		String pathString = dataItem.getItemPath().toString();
		final String EXT = "extension/";
		if (pathString.startsWith(EXT)) {
			entity += " extension";
			pathString = pathString.substring(EXT.length());
		}
		return entity + "&#10;" + pathString;
	}

	@Override
	public String getNodeStyleAttributes() {
		return "style=filled, fillcolor=" + COLOR_FILL + ", color=" + getBorderColor();
	}

	private String getBorderColor() {
		if (QNameUtil.match(UserType.COMPLEX_TYPE, dataItem.getTypeName())) {
			return COLOR_USER;
		} else if (QNameUtil.match(RoleType.COMPLEX_TYPE, dataItem.getTypeName())) {
			return COLOR_ROLE;
		} else if (QNameUtil.match(OrgType.COMPLEX_TYPE, dataItem.getTypeName())) {
			return COLOR_ORG;
		} else {
			return COLOR_DEFAULT;
		}
	}

}
