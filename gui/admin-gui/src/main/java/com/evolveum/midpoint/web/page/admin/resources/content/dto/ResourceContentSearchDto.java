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
package com.evolveum.midpoint.web.page.admin.resources.content.dto;

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

public class ResourceContentSearchDto implements Serializable, DebugDumpable {
	private static final long serialVersionUID = 1L;

	private Boolean resourceSearch = Boolean.FALSE;
	private ShadowKindType kind;
	private String intent;
	private QName objectClass;

	private boolean isUseObjectClass = false;

	public ResourceContentSearchDto(ShadowKindType kind) {
		this.kind = kind;
		if (kind == null) {
			this.isUseObjectClass = true;
		}
	}

	public Boolean isResourceSearch() {
		return resourceSearch;
	}
	public void setResourceSearch(Boolean resourceSearch) {
		this.resourceSearch = resourceSearch;
	}
	public ShadowKindType getKind() {
		return kind;
	}
	public void setKind(ShadowKindType kind) {
		this.kind = kind;
	}
	public String getIntent() {
		return intent;
	}
	public void setIntent(String intent) {
		this.intent = intent;
	}

	public QName getObjectClass() {
		return objectClass;
	}

	public void setObjectClass(QName objectClass) {
		this.objectClass = objectClass;
	}

	public boolean isUseObjectClass() {
		return isUseObjectClass;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ResourceContentSearchDto\n");
		DebugUtil.debugDumpWithLabelLn(sb, "resourceSearch", resourceSearch, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "kind", kind==null?null:kind.toString(), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "intent", intent, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "objectClass", objectClass, indent+1);
		DebugUtil.debugDumpWithLabel(sb, "isUseObjectClass", isUseObjectClass, indent+1);
		return sb.toString();
	}

}
