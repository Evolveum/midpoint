package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;

public class JasperReportValueDto implements Serializable {

	private static final long serialVersionUID = 1L;

	private Object value;
	private String displayName;

	public JasperReportValueDto() {

	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

}
