package com.evolveum.midpoint.web.page.admin.roles;

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class RoleMemberSearchDto implements Serializable {

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
}
