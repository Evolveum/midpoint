package com.evolveum.midpoint.prism.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

public class EqualsFilter extends PropertyValueFilter {

	public EqualsFilter(PropertyPath path, ItemDefinition definition, List<PrismValue> values) {
		super(path, definition, values);
	}

	public EqualsFilter(PropertyPath path, ItemDefinition definition, PrismValue value) {
		super(path, definition, value);
	}

	// public EqualsFilter(ItemDefinition item, List<PrismValue> value) {
	// this.item = item;
	// this.value = value;
	// }

	public static EqualsFilter createEqual(PropertyPath path, ItemDefinition item, PrismValue value) {
		List<PrismValue> values = new ArrayList<PrismValue>();
		values.add(value);
		return new EqualsFilter(path, item, values);
	}

	public static EqualsFilter createEqual(PropertyPath path, ItemDefinition item, List<PrismValue> values) {
		return new EqualsFilter(path, item, values);
	}

	public static EqualsFilter createEqual(PropertyPath path, ItemDefinition item, Object realValue) {

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
			return createEqual(path, item, prismValues);
		}
		PrismPropertyValue value = new PrismPropertyValue(realValue);
		return createEqual(path, item, value);
	}

	public static EqualsFilter createReferenceEqual(PropertyPath path, ItemDefinition item, String oid) {
		PrismReferenceValue value = new PrismReferenceValue(oid);
		return createEqual(path, item, value);
	}

	public static EqualsFilter createReferenceEqual(Class type, QName propertyName, PrismContext prismContext,
			String oid) throws SchemaException {
		PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		return createReferenceEquals(null, objDef, propertyName, oid);

	}

	public static EqualsFilter createReferenceEquals(PropertyPath path, PrismContainerDefinition containerDef,
			QName propertyName, String realValue) throws SchemaException {
		ItemDefinition itemDef = containerDef.findItemDefinition(propertyName);
		if (itemDef == null) {
			throw new SchemaException("No definition for item " + propertyName + " in container definition "
					+ containerDef);
		}
		return createReferenceEqual(path, itemDef, realValue);
	}

	public static EqualsFilter createReferenceEqual(PropertyPath path, ItemDefinition item, PrismReferenceValue ref) {
		return createEqual(path, item, ref);
	}

	public static EqualsFilter createReferenceEqual(PropertyPath path, ItemDefinition item, List<Object> refs) {
		return createEqual(path, item, refs);
	}

	public static EqualsFilter createEquals(PropertyPath path, PrismContainerDefinition containerDef,
			QName propertyName, PrismValue... values) throws SchemaException {
		ItemDefinition itemDef = containerDef.findItemDefinition(propertyName);
		if (itemDef == null) {
			throw new SchemaException("No definition for item " + propertyName + " in container definition "
					+ containerDef);
		}

		return createEqual(path, itemDef, values);
	}

	public static EqualsFilter createEquals(PropertyPath path, PrismContainerDefinition containerDef,
			QName propertyName, Object realValue) throws SchemaException {
		ItemDefinition itemDef = containerDef.findItemDefinition(propertyName);
		if (itemDef == null) {
			throw new SchemaException("No definition for item " + propertyName + " in container definition "
					+ containerDef);
		}

		return createEqual(path, itemDef, realValue);
	}

	public static EqualsFilter createEqual(Class type, PrismContext prismContext, QName propertyName, Object realValue)
			throws SchemaException {
		PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		return createEquals(null, objDef, propertyName, realValue);
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
		// sb.append("item def: ");
		if (getDefinition() != null) {
			sb.append(getDefinition().debugDump(indent + 1));
			sb.append("\n");
		} else {
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append("null\n");
		}
		// sb.append("value: ");
		if (getValues() != null) {
			indent += 1;
			for (PrismValue val : getValues()) {
				sb.append(val.debugDump(indent));
			}
		} else {
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append("null\n");
		}
		return sb.toString();

	}

}
