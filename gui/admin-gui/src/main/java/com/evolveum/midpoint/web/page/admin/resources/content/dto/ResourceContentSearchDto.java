package com.evolveum.midpoint.web.page.admin.resources.content.dto;

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

public class ResourceContentSearchDto implements Serializable{
	
	
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
	
	
	
	

}
