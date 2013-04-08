package com.evolveum.midpoint.prism.query;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

public class SubstringFilter extends StringValueFilter {

	SubstringFilter(ItemPath parentPath, ItemDefinition definition, String value) {
		super(parentPath, definition, value);
	}
	
	SubstringFilter(ItemPath parentPath, ItemDefinition definition, String matchingRule, String value) {
		super(parentPath, definition, matchingRule, value);
	}

	public static SubstringFilter createSubstring(ItemPath path, ItemDefinition definition, String value) {
		return new SubstringFilter(path, definition, value);
	}
	
	public static SubstringFilter createSubstring(ItemPath path, ItemDefinition definition, String matchingRule, String value) {
		return new SubstringFilter(path, definition, matchingRule, value);
	}
	
	public static SubstringFilter createSubstring(Class clazz, PrismContext prismContext, QName propertyName, String value) {
		PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
		ItemDefinition itemDef = objDef.findItemDefinition(propertyName);
		return new SubstringFilter(null, itemDef, value);
	}

	@Override
	public SubstringFilter clone() {
		return new SubstringFilter(getParentPath(),getDefinition(),getValue());
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
		sb.append("SUBSTRING: \n");
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
		if (getValue() != null) {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append(getValue());
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
		sb.append("SUBSTRING: ");
		if (getParentPath() != null){
			sb.append(getParentPath().toString());
			sb.append(", ");
		}
		if (getDefinition() != null){
			sb.append(getDefinition().getName().getLocalPart());
			sb.append(", ");
		}
		if (getValue() != null){
			sb.append(getValue());
		}
		return sb.toString();
	}

	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object) {
		ItemPath path = null;
		if (getParentPath() != null){
			path = new ItemPath(getParentPath(), getDefinition().getName());
		} else{
			path = new ItemPath(getDefinition().getName());
		}
		
		Item item = object.findItem(path);
		
		for (Object val : item.getValues()){
			if (val instanceof PrismPropertyValue){
				Object value = ((PrismPropertyValue) val).getValue();
				if (value instanceof PolyStringType){
					if (StringUtils.contains(((PolyStringType) value).getNorm(), getValue())){
						return true;
					}
				} else if (value instanceof PolyString){
					if (StringUtils.contains(((PolyString) value).getNorm(), getValue())){
						return true;
					}
				} else if (value instanceof String){
					if (StringUtils.contains((String)value, getValue())){
						return true;
					}
				}
			}
			if (val instanceof PrismReferenceValue) {
				throw new UnsupportedOperationException(
						"matching substring on the prism reference value not supported yet");
			}
		}
		
		return false;
	}

}
