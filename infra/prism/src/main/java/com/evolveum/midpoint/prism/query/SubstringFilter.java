package com.evolveum.midpoint.prism.query;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.util.DebugUtil;

public class SubstringFilter extends StringValueFilter {

	public SubstringFilter(PropertyPath path, ItemDefinition definition, String value) {
		super(path, definition, value);
		// TODO Auto-generated constructor stub
	}

	public static SubstringFilter createSubstring(PropertyPath path, ItemDefinition definition, String value) {
		return new SubstringFilter(path, definition, value);
	}
	
	public static SubstringFilter createSubstring(Class clazz, PrismContext prismContext, QName propertyName, String value) {
		PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
		ItemDefinition itemDef = objDef.findItemDefinition(propertyName);
		return new SubstringFilter(null, itemDef, value);
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
		if (getDefinition() != null) {
			sb.append(getDefinition().debugDump(indent + 1));
			sb.append("\n");
		} else {
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append("null\n");
		}

		if (getValue() != null) {
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append(getValue());
		} else {
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append("null\n");
		}
		return sb.toString();
	}

}
