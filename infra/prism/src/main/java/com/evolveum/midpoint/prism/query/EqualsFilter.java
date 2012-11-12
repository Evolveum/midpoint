package com.evolveum.midpoint.prism.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.sun.tools.xjc.reader.xmlschema.parser.SchemaConstraintChecker;

public class EqualsFilter extends PropertyValueFilter implements Itemable{

	EqualsFilter(ItemPath path, ItemDefinition definition, List<PrismValue> values) {
		super(path, definition, values);
	}

	EqualsFilter(ItemPath path, ItemDefinition definition, PrismValue value) {
		super(path, definition, value);
	}
	
	EqualsFilter(ItemPath path, ItemDefinition definition, Element expression) {
		super(path, definition, expression);
	}

	public static EqualsFilter createEqual(ItemPath path, ItemDefinition itemDef, PrismValue value) {
		Validate.notNull(itemDef, "Item definition in the equals filter must not be null");
		return new EqualsFilter(path, itemDef, value);
	}

	public static EqualsFilter createEqual(ItemPath path, ItemDefinition itemDef, List<PrismValue> values) {
		Validate.notNull(itemDef, "Item definition in the equals filter must not be null");
		return new EqualsFilter(path, itemDef, values);
	}

	public static EqualsFilter createEqual(ItemPath path, ItemDefinition itemDef, Element expression) {
		Validate.notNull(itemDef, "Item definition in the equals filter must not be null");
		return new EqualsFilter(path, itemDef, expression);
	}

	public static EqualsFilter createEqual(ItemPath parentPath, ItemDefinition item, Object realValue) {

		if (realValue == null){
			return createEqual(parentPath, item, new PrismPropertyValue(null));
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
			return createEqual(parentPath, item, prismValues);
		}
		PrismPropertyValue value = new PrismPropertyValue(realValue);
		return createEqual(parentPath, item, value);
	}

	
	public static EqualsFilter createEqual(ItemPath parentPath, PrismContainerDefinition<? extends Containerable> containerDef,
			QName propertyName, PrismValue... values) throws SchemaException {
		ItemDefinition itemDef = containerDef.findItemDefinition(propertyName);
		if (itemDef == null) {
			throw new SchemaException("No definition for item " + propertyName + " in container definition "
					+ containerDef);
		}

		return createEqual(parentPath, itemDef, values);
	}

	public static EqualsFilter createEqual(ItemPath parentPath, PrismContainerDefinition<? extends Containerable> containerDef,
			QName propertyName, Object realValue) throws SchemaException {
		ItemDefinition itemDef = containerDef.findItemDefinition(propertyName);
		if (itemDef == null) {
			throw new SchemaException("No definition for item " + propertyName + " in container definition "
					+ containerDef);
		}

		return createEqual(parentPath, itemDef, realValue);
	}

	public static EqualsFilter createEqual(Class<? extends Objectable> type, PrismContext prismContext, QName propertyName, Object realValue)
			throws SchemaException {
		PrismObjectDefinition<?> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		return createEqual(null, objDef, propertyName, realValue);
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
		} else {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("null\n");
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
	public ItemPath getPath(ItemPath pathPrefix) {
		// TODO Auto-generated method stub
		return null;
	}

}
