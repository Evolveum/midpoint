package com.evolveum.midpoint.provisioning.ucf.query;

import org.identityconnectors.framework.common.objects.filter.Filter;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.exception.SchemaException;

public abstract class Operation {
	
	private FilterInterpreter interpreter;
	
	
	public Operation(FilterInterpreter interpreter){
		this.interpreter = interpreter;
	}
	
	
	public abstract Filter interpret(ObjectFilter objectFilter) throws SchemaException;
	

	public FilterInterpreter getInterpreter() {
		return interpreter;
	}
	
	public void setInterpreter(FilterInterpreter interpreter) {
		this.interpreter = interpreter;
	}
}
