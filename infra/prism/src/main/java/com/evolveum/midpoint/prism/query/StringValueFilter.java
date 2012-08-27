package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PropertyPath;

public abstract class StringValueFilter extends ValueFilter{

	private String value;
	
	public StringValueFilter(PropertyPath path, ItemDefinition definition, String value) {
		super(path, definition);
		this.value = value;
	}	
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	
}
