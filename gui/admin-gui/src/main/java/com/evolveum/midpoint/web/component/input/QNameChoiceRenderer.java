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
package com.evolveum.midpoint.web.component.input;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.MidPointConstants;

/**
 * This renderer shouldn't be used unless absolutely necessary. Think twice whether you need {@link QName}
 * in your dropdown, because it's hard to translate {@link QName} values.
 *
 * Most of the time {@link com.evolveum.midpoint.schema.constants.ObjectTypes} and such should be used.
 */
public class QNameChoiceRenderer implements IChoiceRenderer<QName> {
	private static final long serialVersionUID = 1L;

	private static Map<String, String> prefixMap;

	static {
		prefixMap = new HashMap<>();
		prefixMap.put(SchemaConstantsGenerated.NS_ICF_SCHEMA, "icfs:");
		prefixMap.put(SchemaConstantsGenerated.NS_CAPABILITIES, "cap:");
		prefixMap.put(SchemaConstantsGenerated.NS_COMMON, "c:");
		prefixMap.put(SchemaConstantsGenerated.NS_QUERY, "q:");
		prefixMap.put(MidPointConstants.NS_RI, "ri:");
	}

	private boolean usePrefix = false;

	public QNameChoiceRenderer() {
		this(false);
	}

	public QNameChoiceRenderer(boolean usePrefix) {
		super();
		this.usePrefix = usePrefix;
	}

	@Override
	public QName getObject(String id, IModel<? extends List<? extends QName>> choices) {
		if (StringUtils.isEmpty(id)) {
			return null;
		}
		return choices.getObject().get(Integer.parseInt(id));
	}

	@Override
	public Object getDisplayValue(QName object) {
		StringBuilder sb = new StringBuilder();
		if (usePrefix){
			String prefix = prefixMap.get(object.getNamespaceURI());
			if (StringUtils.isNotBlank(prefix)){
				sb.append(prefix);
			}
		}

		sb.append(object.getLocalPart());
		return sb.toString();
	}

	@Override
	public String getIdValue(QName object, int index) {
		return Integer.toString(index);
	}


}
