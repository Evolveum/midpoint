package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;

public class JasperReportRealValueDto<T> implements Serializable{

	private T value;
	
	public T getValue() {
		return value;
	}
	
	public void setValue(T value) {
		this.value = value;
	}
}
