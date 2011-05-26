package com.evolveum.midpoint.web.component.syntax;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIInput;

@FacesComponent("AceXmlInput")
public class AceXmlInput extends UIInput {
	
	public static final String ATTR_VALUE = "value";
	public static final String ATTR_WIDTH = "width";
	public static final String ATTR_HEIGHT = "height";
	public static final String ATTR_READONLY = "readonly";
	
	@Override
	public String getFamily() {
		return "AceXmlInput";
	}
}
