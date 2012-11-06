package com.evolveum.midpoint.prism.query;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

public abstract class ValueFilter extends ObjectFilter {
	
	private ItemPath path;
	private ItemDefinition definition;
	
	public ValueFilter(ItemPath path, ItemDefinition definition){
		this.path = path;
		this.definition = definition;
	}
	
	public ValueFilter(ItemPath path, ItemDefinition definition, Element expression){
		super(expression);
		this.path = path;
		this.definition = definition;
	}
	
	public ItemDefinition getDefinition() {
		return definition;
	}
	
	public void setDefinition(ItemDefinition definition) {
		this.definition = definition;
	}
	
	public ItemPath getPath() {
		return path;
	}
	
	public void setPath(ItemPath path) {
		this.path = path;
	}

}
