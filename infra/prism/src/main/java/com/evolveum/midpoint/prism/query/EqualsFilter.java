package com.evolveum.midpoint.prism.query;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class EqualsFilter extends PropertyValueFilter implements Itemable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3284478412180258355L;

	EqualsFilter(){	
	}
	
	EqualsFilter(ItemPath parentPath, ItemDefinition definition, String matchingRule, List<PrismValue> values) {
		super(parentPath, definition, matchingRule, values);
	}

	EqualsFilter(ItemPath parentPath, ItemDefinition definition, String matchingRule, PrismValue value) {
		super(parentPath, definition, value);
	}
	
	EqualsFilter(ItemPath parentPath, ItemDefinition definition, Element expression) {
		super(parentPath, definition, expression);
	}
	
	EqualsFilter(ItemPath parentPath, ItemDefinition definition, String matchingRule, Element expression) {
		super(parentPath, definition, matchingRule, expression);
	}
	
	public static EqualsFilter createEqual(ItemPath path, ItemDefinition itemDef, PrismValue value) {
		Validate.notNull(itemDef, "Item definition in the equals filter must not be null");
		return new EqualsFilter(path, itemDef, null, value);
	}
	public static EqualsFilter createEqual(ItemPath path, ItemDefinition itemDef, String matchingRule, PrismValue value) {
		Validate.notNull(itemDef, "Item definition in the equals filter must not be null");
		return new EqualsFilter(path, itemDef, matchingRule, value);
	}

	public static EqualsFilter createEqual(ItemPath path, ItemDefinition itemDef, String matchingRule, List<PrismValue> values) {
		Validate.notNull(itemDef, "Item definition in the equals filter must not be null");
		return new EqualsFilter(path, itemDef, matchingRule, values);
	}

	public static EqualsFilter createEqual(ItemPath path, ItemDefinition itemDef, Element expression) {
		Validate.notNull(itemDef, "Item definition in the equals filter must not be null");
		return new EqualsFilter(path, itemDef, expression);
	}
	
	public static EqualsFilter createEqual(ItemPath path, ItemDefinition itemDef, String matchingRule, Element expression) {
		Validate.notNull(itemDef, "Item definition in the equals filter must not be null");
		return new EqualsFilter(path, itemDef, matchingRule, expression);
	}

	public static EqualsFilter createEqual(ItemPath parentPath, ItemDefinition item, Object realValue) {
		return (EqualsFilter) createPropertyFilter(EqualsFilter.class, parentPath, item, realValue);
	}	
	
	public static EqualsFilter createEqual(ItemPath parentPath, PrismContainerDefinition<? extends Containerable> containerDef,
			QName propertyName, PrismValue... values) throws SchemaException {
		return (EqualsFilter) createPropertyFilter(EqualsFilter.class, parentPath, containerDef, propertyName, values);
	}

	public static EqualsFilter createEqual(ItemPath parentPath, PrismContainerDefinition<? extends Containerable> containerDef,
			QName propertyName, Object realValue) throws SchemaException {
		return (EqualsFilter) createPropertyFilter(EqualsFilter.class, parentPath, containerDef, propertyName, realValue);
	}

	public static EqualsFilter createEqual(Class<? extends Objectable> type, PrismContext prismContext, QName propertyName, Object realValue)
			throws SchemaException {
		return (EqualsFilter) createPropertyFilter(EqualsFilter.class, type, prismContext, propertyName, realValue);
	}
	
	public static EqualsFilter createEqual(Class<? extends Objectable> type, PrismContext prismContext, ItemPath propertyPath, Object realValue)
			throws SchemaException {
		return (EqualsFilter) createPropertyFilter(EqualsFilter.class, type, prismContext, propertyPath, realValue);
	}
	
	@Override
	public EqualsFilter clone() {
		EqualsFilter clone = new EqualsFilter(getParentPath(), getDefinition(), getMatchingRule(), (List<PrismValue>) getValues());
		cloneValues(clone);
		return clone;
	}

	@Override
	public String dump() {
		return debugDump(0);
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("EQUALS: \n");
		
		if (getParentPath() != null){
			DebugUtil.indentDebugDump(sb, indent+1);
			sb.append("PATH: ");
			sb.append(getParentPath().toString());
			sb.append("\n");
		} 
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("DEF: ");
		if (getDefinition() != null) {
			sb.append(getDefinition().debugDump(indent));
			sb.append("\n");
		} else {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("null\n");
		}
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("VALUE: ");
		if (getValues() != null) {
			indent += 1;
			for (PrismValue val : getValues()) {
				sb.append(val.debugDump(indent));
				
			}
			sb.append("\n");
		} else {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("null\n");
		}
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("MATCHING: ");
		if (getMatchingRule() != null) {
			indent += 1;
				sb.append(getMatchingRule());
		} else {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("default\n");
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("EQUALS: ");
		if (getParentPath() != null){
			sb.append(getParentPath().toString());
			sb.append(", ");
		}
		if (getDefinition() != null){
			sb.append(getDefinition().getName().getLocalPart());
			sb.append(", ");
		}
		if (getValues() != null){
			for (int i = 0; i< getValues().size() ; i++){
				sb.append(getValues().get(i).toString());
				if ( i != getValues().size() -1){
					sb.append(", ");
				}
			}
		}
		return sb.toString();
	}

	@Override
	public QName getName() {
		return getDefinition().getName();
	}

	@Override
	public PrismContext getPrismContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ItemPath getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object) {
		return super.match(object);
	}

}
