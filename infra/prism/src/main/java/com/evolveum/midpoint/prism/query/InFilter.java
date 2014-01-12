package com.evolveum.midpoint.prism.query;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;

public class InFilter<T> extends PropertyValueFilter<PrismPropertyValue>{
	
	InFilter(ItemPath path, PrismPropertyDefinition definition, QName matchingRule, List<PrismPropertyValue> values ) {
		super(path, definition, matchingRule, values);
	}
	
	public static <V> InFilter createIn(ItemPath path, PrismPropertyDefinition definition, QName matchingRule, V values){
		
		List<PrismPropertyValue<V>> pVals = createPropertyList(definition, values);
		
		return new InFilter(path, definition, matchingRule, pVals);
	}
	
	
	public static <V> InFilter createIn(ItemPath path, PrismPropertyDefinition definition, QName matchingRule, PrismPropertyValue<V>... values) {

		List<PrismPropertyValue<V>> pVals = createPropertyList(definition, values);

		return new InFilter(path, definition, matchingRule, pVals);
	}
	
	public static <V> InFilter createIn(ItemPath path, PrismPropertyDefinition definition, PrismPropertyValue<V>... values) {
		return createIn(path, definition, null, values);
	}
	
	public static <V> InFilter createIn(ItemPath path, Class type, PrismContext prismContext, QName matchingRule, V values) {

		PrismPropertyDefinition definition = (PrismPropertyDefinition) findItemDefinition(path, type, prismContext);
		return createIn(path, definition, null, values);
	}
	
	

	@Override
	public QName getElementName() {
		return getDefinition().getName();
	}

	@Override
	public PrismContext getPrismContext() {
		return getDefinition().getPrismContext();
	}

	@Override
	public ItemPath getPath() {
		return getFullPath();
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
		sb.append("IN: ");
		return debugDump(indent, sb);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("IN: ");
		return toString(sb);
	}

	@Override
	public InFilter clone() {
		return new InFilter(getFullPath(), getDefinition(),getMatchingRule(), getValues());
	}
	
	@Override
	public PrismPropertyDefinition getDefinition() {
		return (PrismPropertyDefinition) super.getDefinition();
	}


}
