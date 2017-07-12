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
package com.evolveum.midpoint.web.page.admin.roles;

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class RoleMemberSearchDto implements Serializable, DebugDumpable {
	private static final long serialVersionUID = 1L;

	public static String F_TYPE = "type";
	public static String F_TENANT = "tenant";
	public static String F_PROJECT = "project";
	
	public static String F_TEXT = "text";
	
	private QName type;
	private OrgType tenant;
	private OrgType project;
	private String text;
	
	
	public void setType(QName type) {
		this.type = type;
	}
	
	public QName getType() {
		if (type == null){
			return UserType.COMPLEX_TYPE;
		}
		return type;
	}
	
	public OrgType getTenant() {
		return tenant;
	}
	
	public void setTenant(OrgType tenant) {
		this.tenant = tenant;
	}
	
	public OrgType getProject() {
		return project;
	}
	
	public void setProject(OrgType project) {
		this.project = project;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("RoleMemberSearchDto\n");
		DebugUtil.debugDumpWithLabelLn(sb, "type", type, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "tenant", tenant==null?null:tenant.toString(), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "project", project==null?null:project.toString(), indent+1);
		DebugUtil.debugDumpWithLabel(sb, "text", text, indent+1);
		return sb.toString();
	}
}
