package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.Validatable;

public class JasperReportParameterDto extends Selectable implements Serializable, Editable, Validatable{
	
	private String name;
	private Class type;
	private String typeAsString;
	
	private boolean editing;
	
	public JasperReportParameterDto() {
		// TODO Auto-generated constructor stub
	}
	
	public JasperReportParameterDto(String name, Class type, String typeAsString) {
		this.name = name;
		this.typeAsString = typeAsString;
		this.type = type;
	}
	
	public String getName() {
		return name;
	}
	
	public String getTypeAsString() {
		return typeAsString;
	}
	
	public Class getType() {
		return type;
	}

	@Override
	public boolean isEditing() {
		return editing;
	}

	@Override
	public void setEditing(boolean editing) {
		this.editing = editing;
	}

	@Override
	public boolean isEmpty(){
		if (StringUtils.isBlank(name) && StringUtils.isBlank(typeAsString)){
			return true;
		}
		return false;
	}
}
