package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PropertyPath;

public abstract class ValueFilter extends ObjectFilter {
	
	private PropertyPath path;
	private ItemDefinition definition;
	
	public ValueFilter(PropertyPath path, ItemDefinition definition){
		this.path = path;
		this.definition = definition;
	}
	
	public ItemDefinition getDefinition() {
		return definition;
	}
	
	public void setDefinition(ItemDefinition definition) {
		this.definition = definition;
	}
	
	public PropertyPath getPath() {
		return path;
	}
	
	public void setPath(PropertyPath path) {
		this.path = path;
	}

}
