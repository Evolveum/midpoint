package com.evolveum.midpoint.prism.query;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

public abstract class ValueFilter extends ObjectFilter {
	
	private ItemPath parentPath;
	private ItemDefinition definition;
	
	public ValueFilter() {
		// TODO Auto-generated constructor stub
	}
	
	public ValueFilter(ItemPath parentPath, ItemDefinition definition){
		this.parentPath = parentPath;
		this.definition = definition;
	}
	
	public ValueFilter(ItemPath parentPath, ItemDefinition definition, Element expression){
		super(expression);
		this.parentPath = parentPath;
		this.definition = definition;
	}
	
	public ItemDefinition getDefinition() {
		return definition;
	}
	
	public void setDefinition(ItemDefinition definition) {
		this.definition = definition;
	}
	
	public ItemPath getParentPath() {
		return parentPath;
	}
	
	public void setParentPath(ItemPath path) {
		this.parentPath = path;
	}
	
	protected void cloneValues(ValueFilter clone) {
		super.cloneValues(clone);
		clone.parentPath = this.parentPath;
		clone.definition = this.definition;
	}

}
