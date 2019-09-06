/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid.query;

import org.identityconnectors.framework.common.objects.filter.Filter;

import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;

public class FilterInterpreter {
	
	private ObjectClassComplexTypeDefinition objectClassDefinition;
	
	public FilterInterpreter(ObjectClassComplexTypeDefinition objectClassDefinition){
		this.objectClassDefinition = objectClassDefinition;
	}
	
	public Filter interpret(ObjectFilter filter, ConnIdNameMapper icfNameMapper) throws SchemaException{
		
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
		
		return operation.interpret(filter, icfNameMapper);
		
	}

	public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
		return objectClassDefinition;
	}
	
}
