package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

public abstract class StringValueFilter extends ValueFilter{

	private String value;
	
	public StringValueFilter(ItemPath parentPath, ItemDefinition definition, String value) {
		super(parentPath, definition);
		this.value = value;
	}
	
	public StringValueFilter(ItemPath parentPath, ItemDefinition definition, String matchingRule, String value) {
		super(parentPath, definition, matchingRule);
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	
}
