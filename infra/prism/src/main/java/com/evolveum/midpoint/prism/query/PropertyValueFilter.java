package com.evolveum.midpoint.prism.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

public abstract class PropertyValueFilter extends ValueFilter{

	private List<? extends PrismValue> values;

	PropertyValueFilter(){
		
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, List<? extends PrismValue> values) {
		super(path, definition);
		this.values = values;
	}
	
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, PrismValue value) {
		super(path, definition);
		setValue(value);
	}
	
	PropertyValueFilter(ItemPath path, ItemDefinition definition, Element expression) {
		super(path, definition, expression);
	}
	
	static PropertyValueFilter create(Class filterClass, ItemPath path, ItemDefinition itemDef, PrismValue value){
		if (filterClass.isAssignableFrom(EqualsFilter.class)){
			return EqualsFilter.createEqual(path, itemDef, value);
		} else if (filterClass.isAssignableFrom(LessFilter.class)){
			return LessFilter.createLessFilter(path, itemDef, value, false);
		} else if (filterClass.isAssignableFrom(GreaterFilter.class)){
			return GreaterFilter.createGreaterFilter(path, itemDef, value, false);
		}
		throw new IllegalArgumentException("Bad filter class");
	}

	static PropertyValueFilter create(Class filterClass, ItemPath path, ItemDefinition itemDef, List<PrismValue> values) {
		if (filterClass.isAssignableFrom(EqualsFilter.class)) {
			return EqualsFilter.createEqual(path, itemDef, values);
		}
		throw new IllegalArgumentException("Bad filter class");
	}
		
	public static PropertyValueFilter createPropertyFilter(Class filterClass, ItemPath parentPath, ItemDefinition item, Object realValue) {

		if (realValue == null){
			return create(filterClass, parentPath, item, new PrismPropertyValue(null));
		}
		if (List.class.isAssignableFrom(realValue.getClass())) {
			List<PrismValue> prismValues = new ArrayList<PrismValue>();
			for (Object o : (List) realValue) {
				if (o instanceof PrismPropertyValue) {
					prismValues.add((PrismPropertyValue) o);
				} else {
					PrismPropertyValue val = new PrismPropertyValue(o);
					prismValues.add(val);
				}
			}
			return create(filterClass, parentPath, item, prismValues);
		}
		PrismPropertyValue value = new PrismPropertyValue(realValue);
		return create(filterClass, parentPath, item, value);
	}

	
	public static PropertyValueFilter createPropertyFilter(Class filterClass, ItemPath parentPath, PrismContainerDefinition<? extends Containerable> containerDef,
			QName propertyName, PrismValue... values) throws SchemaException {
		ItemDefinition itemDef = containerDef.findItemDefinition(propertyName);
		if (itemDef == null) {
			throw new SchemaException("No definition for item " + propertyName + " in container definition "
					+ containerDef);
		}

		return create(filterClass, parentPath, itemDef, Arrays.asList(values));
	}

	public static PropertyValueFilter createPropertyFilter(Class filterClass, ItemPath parentPath, PrismContainerDefinition<? extends Containerable> containerDef,
			QName propertyName, Object realValue) throws SchemaException {
		ItemDefinition itemDef = containerDef.findItemDefinition(propertyName);
		if (itemDef == null) {
			throw new SchemaException("No definition for item " + propertyName + " in container definition "
					+ containerDef);
		}

		return createPropertyFilter(filterClass, parentPath, itemDef, realValue);
	}

	public static PropertyValueFilter createPropertyFilter(Class filterClass, Class<? extends Objectable> type, PrismContext prismContext, QName propertyName, Object realValue)
			throws SchemaException {
		PrismObjectDefinition<?> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		return createPropertyFilter(filterClass, null, objDef, propertyName, realValue);
	}
	
	public static PropertyValueFilter createPropertyFilter(Class filterClass, Class<? extends Objectable> type, 
			PrismContext prismContext, ItemPath propertyPath, Object realValue) throws SchemaException {
		PrismObjectDefinition<? extends Objectable> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		PrismContainerDefinition<? extends Containerable> containerDef = objDef;
		ItemPath parentPath = propertyPath.allExceptLast();
		if (!parentPath.isEmpty()) {
			containerDef = objDef.findContainerDefinition(parentPath);
			if (containerDef == null) {
				throw new SchemaException("No definition for container " + parentPath + " in object definition "
						+ objDef);
			}
		}
		return createPropertyFilter(filterClass, propertyPath.allExceptLast(), containerDef, ItemPath.getName(propertyPath.last()), realValue);
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
