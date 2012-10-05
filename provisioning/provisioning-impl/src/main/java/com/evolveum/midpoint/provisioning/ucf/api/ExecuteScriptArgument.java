package com.evolveum.midpoint.provisioning.ucf.api;

import java.util.Collection;

public class ExecuteScriptArgument {

	
	private String argumentName;
	private Collection<Object> argumentValue;

	public ExecuteScriptArgument() {
		
	}
	
	public ExecuteScriptArgument(String name, Collection<Object> value) {
		this.argumentName = name;
		this.argumentValue = value;
	}
	
	public String getArgumentName() {
		return argumentName;
	}

	public void setArgumentName(String argumentName) {
		this.argumentName = argumentName;
	}

	public Collection<Object> getArgumentValue() {
		return argumentValue;
	}

	public void setArgumentValue(Collection<Object> argumentValue) {
		this.argumentValue = argumentValue;
	}
}
