package com.evolveum.midpoint.prism.query;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;

public abstract class PropertyValueFilter extends ValueFilter{

	private List<? extends PrismValue> values;

	public PropertyValueFilter(ItemPath path, ItemDefinition definition, List<? extends PrismValue> values) {
		super(path, definition);
		this.values = values;
	}
	
	
	public PropertyValueFilter(ItemPath path, ItemDefinition definition, PrismValue value) {
		super(path, definition);
		setValue(value);
	}
	
	public PropertyValueFilter(ItemPath path, ItemDefinition definition, Element expression) {
		super(path, definition, expression);
	}
	
	public List<? extends PrismValue> getValues() {
		return values;
	}
	
	public void setValues(List<? extends PrismValue> values) {
		this.values = values;
	}
	
	public void setValue(PrismValue value) {
		List<PrismValue> values = new ArrayList<PrismValue>();
		values.add(value);
		this.values = values;
	}
	
	protected void cloneValues(PropertyValueFilter clone) {
		super.cloneValues(clone);
		clone.values = getCloneValuesList();
	}
	private List<? extends PrismValue> getCloneValuesList() {
		if (values == null) {
			return null;
		}
		List<PrismValue> clonedValues = new ArrayList<PrismValue>(values.size());
		for(PrismValue value: values) {
			clonedValues.add(value.clone());
		}
		return clonedValues;
	}
	
	
}
