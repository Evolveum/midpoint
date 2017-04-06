package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sf.jasperreports.engine.JRPropertiesMap;

public class JasperReportValueDto<T> implements Serializable{

	private static final String PROPERTY_KEY = "key";
	private static final String PROPERTY_LABEL = "lable";
	private static final String PROPERTY_TARGET_TYPE = "targetType";
	private static final String PROPERTY_MULTIVALUE = "multivalue";
	
	private List<JasperReportRealValueDto> value;
	private String key;
	private String label;
	private String targetType;
	private String isMultiValue;
	
	private JRPropertiesMap propertiesMap;
	
	public JasperReportValueDto(JRPropertiesMap propertiesMap) {
		value = new ArrayList<JasperReportRealValueDto>();
		value.add(new JasperReportRealValueDto());
		this.propertiesMap = propertiesMap;
		if (propertiesMap == null) {
			return;
		}
		
		this.key = propertiesMap.getProperty(PROPERTY_KEY);
		this.label = propertiesMap.getProperty(PROPERTY_LABEL);
		this.targetType = propertiesMap.getProperty(PROPERTY_TARGET_TYPE);
		this.isMultiValue = propertiesMap.getProperty(PROPERTY_MULTIVALUE);
	}
 
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		if (propertiesMap == null) {
			propertiesMap = new JRPropertiesMap();
		}
		
		propertiesMap.setProperty(PROPERTY_LABEL, label);
	}

	public String getIsMultiValue() {
		return isMultiValue;

	}
	
	public void setIsMultiValue(String isMultiValue) {
		if (propertiesMap == null) {
			propertiesMap = new JRPropertiesMap();
		}
		
		propertiesMap.setProperty(PROPERTY_MULTIVALUE, isMultiValue);
	}
		
	public String getTargetType() {
		return targetType;
	}
	
	public void setTargetType(String targetType) {
		if (propertiesMap == null) {
			propertiesMap = new JRPropertiesMap();
		}
		
		propertiesMap.setProperty(PROPERTY_TARGET_TYPE, targetType);
	}

	
	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		if (propertiesMap == null) {
			propertiesMap = new JRPropertiesMap();
		}
		
		propertiesMap.setProperty(PROPERTY_KEY, key);
	}
	
	public List<JasperReportRealValueDto> getValue() {
		return value;
	}
	
	public void setValue(List<JasperReportRealValueDto> value) {
		this.value = value;
	}
	
	public JRPropertiesMap getPropertiesMap() {
		return propertiesMap;
	}
	
	
}
