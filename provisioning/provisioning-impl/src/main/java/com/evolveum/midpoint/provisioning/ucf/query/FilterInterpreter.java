package com.evolveum.midpoint.provisioning.ucf.query;

import org.identityconnectors.framework.common.objects.filter.Filter;

import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.util.exception.SchemaException;

public class FilterInterpreter {
	
	private String resourceSchemaNamespace;
	
	public FilterInterpreter(String resourceSchemaNamespace){
		this.resourceSchemaNamespace = resourceSchemaNamespace;
	}
	
	public Filter interpret(ObjectFilter filter) throws SchemaException{
		
		Operation operation = null;
		
		if (LogicalFilter.class.isAssignableFrom(filter.getClass())){
			operation = new LogicalOperation(this);
		}
		
		if (ValueFilter.class.isAssignableFrom(filter.getClass())){
			operation = new ValueOperation(this);
		}
		
		if (operation == null){
			throw new UnsupportedOperationException("Unssupported filter type: " + filter.getClass().getSimpleName());
		}
		
		return operation.interpret(filter);
		
	}
	
	public String getResourceSchemaNamespace() {
		return resourceSchemaNamespace;
	}

}
