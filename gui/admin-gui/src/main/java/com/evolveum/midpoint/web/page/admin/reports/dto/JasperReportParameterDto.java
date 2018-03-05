package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.Validatable;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRPropertiesMap;

public class JasperReportParameterDto extends Selectable implements Serializable, Editable, Validatable {

   private static final long serialVersionUID = 1L;
	private String name;
    private Class<?> type;
    private String typeAsString;
    private String description;
    private Class<?> nestedType;
    private String nestedTypeAsString;
    private boolean forPrompting = false;
    private List<JasperReportValueDto> value;
    private JasperReportParameterPropertiesDto properties;

    private boolean editing;

    public JasperReportParameterDto() {
    }


    public JasperReportParameterDto(JRParameter param) {
        this.name = param.getName();
        this.typeAsString = param.getValueClassName();
        this.type = (Class<?>) param.getValueClass();
        this.forPrompting = param.isForPrompting();

        if (param.getDescription() != null){
    		this.description = param.getDescription();
    	}
    	this.nestedType = param.getNestedType();
    	this.nestedTypeAsString = param.getNestedTypeName();

    	this.value = new ArrayList<>();
    	this.value.add(new JasperReportValueDto());

    	this.properties = new JasperReportParameterPropertiesDto(param.getPropertiesMap());


    }

    @Override
    public List<JasperReportValueDto> getValue() {
		return value;
	}

    public void setValue(List<JasperReportValueDto> value) {
		this.value = value;
	}

    public void addValue() {
		getValue().add(new JasperReportValueDto());
	}

	public void removeValue(JasperReportValueDto realValue) {
		getValue().remove(realValue);
		if (getValue().isEmpty()) {
			getValue().add(new JasperReportValueDto());
		}
	}


    public boolean isForPrompting() {
        return forPrompting;
    }

    public void setForPrompting(boolean forPrompting) {
        this.forPrompting = forPrompting;
    }

    public boolean getForPrompting() {
        return forPrompting;
    }

    public String getName() {
        return name;
    }

    public String getTypeAsString() {
        return typeAsString;
    }

    public String getNestedTypeAsString() {
		return nestedTypeAsString;
	}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public JasperReportParameterPropertiesDto getProperties() {
		return properties;
	}

    public JRPropertiesMap getJRProperties() {
		if (properties == null) {
			return null;
		}

		return properties.getPropertiesMap();
	}

    public void setProperties(JasperReportParameterPropertiesDto properties) {
		this.properties = properties;
	}

    public Class<?> getType() throws ClassNotFoundException {
        if (type == null) {
            if (StringUtils.isNotBlank(typeAsString)) {
                type = (Class<?>) Class.forName(typeAsString);
            } else {
                type = (Class<?>) Object.class;
            }
        }
        return type;
    }

    public Class<?> getNestedType() throws ClassNotFoundException {
    	if (StringUtils.isBlank(nestedTypeAsString)) {
    		return null;
    	}
    	nestedType = Class.forName(nestedTypeAsString);
        return nestedType;
    }

    public boolean isMultiValue() throws ClassNotFoundException{
    	if (List.class.isAssignableFrom(getType())){
    		return true;
    	}

    	return false;
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
    public boolean isEmpty() {
        if (StringUtils.isBlank(name) && StringUtils.isBlank(typeAsString)) {
            return true;
        }
        return false;
    }
}
