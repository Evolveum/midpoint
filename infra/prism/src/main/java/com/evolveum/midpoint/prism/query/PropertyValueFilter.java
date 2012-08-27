package com.evolveum.midpoint.prism.query;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;

public abstract class PropertyValueFilter extends ValueFilter{

	private List<PrismValue> values;

	public PropertyValueFilter(PropertyPath path, ItemDefinition definition, List<PrismValue> values) {
		super(path, definition);
		this.values = values;
	}
	
	public PropertyValueFilter(PropertyPath path, ItemDefinition definition, PrismValue value) {
		super(path, definition);
		List<PrismValue> values = new ArrayList<PrismValue>();
		values.add(value);
		this.values = values;
	}
	
	public List<PrismValue> getValues() {
		return values;
	}
	
	public void setValues(List<PrismValue> values) {
		this.values = values;
	}
	
}
