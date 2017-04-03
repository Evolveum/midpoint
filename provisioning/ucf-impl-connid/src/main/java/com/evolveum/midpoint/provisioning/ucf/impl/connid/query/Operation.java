/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.provisioning.ucf.impl.connid.query;

import org.identityconnectors.framework.common.objects.filter.Filter;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper;
import com.evolveum.midpoint.util.exception.SchemaException;

public abstract class Operation {
	
	private FilterInterpreter interpreter;
	
	
	public Operation(FilterInterpreter interpreter){
		this.interpreter = interpreter;
	}
	
	// TODO: HACK: FIXME: this is wrong! it brings ICF concepts (Filter, IcfNameMapper) to non-ICF interface
	public abstract <T> Filter interpret(ObjectFilter objectFilter, ConnIdNameMapper icfNameMapper) throws SchemaException;
	

	public FilterInterpreter getInterpreter() {
		return interpreter;
	}
	
	public void setInterpreter(FilterInterpreter interpreter) {
		this.interpreter = interpreter;
	}
}
