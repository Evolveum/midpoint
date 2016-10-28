package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.Validatable;

import net.sf.jasperreports.engine.JRPropertiesMap;

public class JasperReportParameterDto extends Selectable implements Serializable, Editable, Validatable {

    private String name;
    private Class type;
    private String typeAsString;
//	private ItemPath path;
    private String description;
    private Class nestedType;
    private boolean forPrompting = false;
    private Object value;

    private JRPropertiesMap properties;

    private boolean editing;

    public JasperReportParameterDto() {
        // TODO Auto-generated constructor stub
    }

    public void setNestedType(Class nestedType) {
        this.nestedType = nestedType;
    }

    public Class getNestedType() {
        return nestedType;
    }

    public JasperReportParameterDto(String name, Class type, String typeAsString, boolean forPrompting) {
        this.name = name;
        this.typeAsString = typeAsString;
        this.type = type;
        this.forPrompting = forPrompting;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setProperties(JRPropertiesMap properties) {
        this.properties = properties;
    }

    public JRPropertiesMap getProperties() {
        if (properties == null) {
            return null;
        }
        JRPropertiesMap clearedMap = new JRPropertiesMap(); // discard null properties
        for (String prop : properties.getPropertyNames()) {
            if (properties.getProperty(prop) != null) {
                clearedMap.setProperty(prop, properties.getProperty(prop));
            }
        }
        return (clearedMap.isEmpty()) ? null : clearedMap;
    }

    public String getPropertyLabel() {
        if (properties != null) {
            return properties.getProperty("label");
        } else {
            return null;
        }
    }

    public void setPropertyLabel(String val) {
        if (properties == null) {
            properties = new JRPropertiesMap();
        }
        properties.setProperty("label", val);
    }
    
    public String getPropertyKey() {
        if (properties != null) {
            return properties.getProperty("key");
        } else {
            return null;
        }
    }

    public void setPropertyKey(String val) {
        if (properties == null) {
            properties = new JRPropertiesMap();
        }
        properties.setProperty("key", val);
    }

    public String getPropertyTargetType() {
        if (properties != null) {
            return properties.getProperty("targetType");
        } else {
            return null;
        }
    }

    public void setPropertyTargetType(String val) {
        if (properties == null) {
            properties = new JRPropertiesMap();
        }
        properties.setProperty("targetType", val);

    }

    public Class getType() throws ClassNotFoundException {
        if (type == null) {
            if (StringUtils.isNotBlank(typeAsString)) {
                type = Class.forName(typeAsString);
            } else {
                type = Object.class;
            }
        }
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
    public boolean isEmpty() {
        if (StringUtils.isBlank(name) && StringUtils.isBlank(typeAsString)) {
            return true;
        }
        return false;
    }
}
