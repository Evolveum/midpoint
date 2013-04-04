package com.evolveum.midpoint.provisioning.ucf.query;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.provisioning.ucf.util.UcfUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

public class ValueOperation extends Operation {

	public ValueOperation(FilterInterpreter interpreter) {
		super(interpreter);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Filter interpret(ObjectFilter objectFilter) throws SchemaException {

		OperationResult parentResult = new OperationResult("interpret");

		ValueFilter valueFilter= (ValueFilter) objectFilter;
		if (valueFilter.getParentPath() != null && !valueFilter.getParentPath().isEmpty()
				&& valueFilter.getParentPath().equals(new ItemPath(ShadowType.F_ATTRIBUTES))) {
			try {
				QName propName = valueFilter.getDefinition().getName();
				String icfName = UcfUtil.convertAttributeNameToIcf(propName, getInterpreter()
						.getResourceSchemaNamespace());
				
				if (objectFilter instanceof EqualsFilter) {
					EqualsFilter eq = (EqualsFilter) objectFilter;
					
					List<Object> convertedValues = new ArrayList<Object>();
					for (PrismValue value : eq.getValues()) {
						Object converted = UcfUtil.convertValueToIcf(value, null, propName);
						convertedValues.add(converted);
					}

					return FilterBuilder.equalTo(AttributeBuilder.build(icfName, convertedValues));
				
				
				} else if (objectFilter instanceof SubstringFilter) {
					SubstringFilter substring = (SubstringFilter) objectFilter;
					Object converted = UcfUtil.convertValueToIcf(substring.getValue(), null, propName);
					return FilterBuilder.contains(AttributeBuilder.build(icfName, converted));
				} else {
					throw new UnsupportedOperationException("Unsupported filter type: " + objectFilter.dump());
//					throw new UnsupportedOperationException("Unsupported equals filter: " + eq.dump());
				}
			} catch (SchemaException ex) {
				throw ex;

			}
		}
		
		throw new UnsupportedOperationException("Unsupported filter type: " + objectFilter.dump());
		
	}

}
