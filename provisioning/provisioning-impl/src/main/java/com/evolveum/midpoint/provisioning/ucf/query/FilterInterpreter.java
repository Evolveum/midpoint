/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.provisioning.ucf.query;

import org.identityconnectors.framework.common.objects.filter.Filter;

import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnIdNameMapper;
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
