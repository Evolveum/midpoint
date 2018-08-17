package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

public class SearchItemDefinition implements Serializable {

	private ItemPath path;
	private ItemDefinition def;
	
	private List<?> allowedValues; 
	
	public SearchItemDefinition(ItemPath path, ItemDefinition def, List<?> allowedValues) {
		this.path = path;
		this.def = def;
		this.allowedValues = allowedValues;
	}

	public ItemPath getPath() {
		return path;
	}

	public ItemDefinition getDef() {
		return def;
	}

	public List<?> getAllowedValues() {
		return allowedValues;
	}
	
	
}
