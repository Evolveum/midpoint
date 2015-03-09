package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;

import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.component.util.Selectable;

public class JasperReportFieldDto extends Selectable implements Serializable, Editable{

	private String name;
	private Class type;
	private String typeAsString;
	
	private boolean editing;
	
	public JasperReportFieldDto() {
		// TODO Auto-generated constructor stub
	}
	
	public JasperReportFieldDto(String name, Class type, String typeAsString) {
		this.name = name;
		this.type = type;
		this.typeAsString = typeAsString;
	}
	
	public String getName() {
		return name;
	}
	
	public Class getType() {
		return type;
	}
	
	public String getTypeAsString() {
		return typeAsString;
	}

	@Override
	public boolean isEditing() {
		return editing;
	}

	@Override
	public void setEditing(boolean editing) {
		this.editing = editing;
	}
}
