package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sf.jasperreports.engine.JRPropertiesMap;

public class JasperReportValueDto<T> implements Serializable{

	
	
	private T value;
	
	public JasperReportValueDto() {
		
	}
 	
	
public T getValue() {
	return value;
}

public void setValue(T value) {
	this.value = value;
}
	
	
	
	
}
