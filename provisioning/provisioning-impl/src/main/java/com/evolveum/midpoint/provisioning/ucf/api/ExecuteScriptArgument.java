package com.evolveum.midpoint.provisioning.ucf.api;

import java.util.List;

public class ExecuteScriptArgument {

	
	private String argumentName;
	private List<Object> argumentValue;

	public ExecuteScriptArgument() {
		
	}
	
	public ExecuteScriptArgument(String name, List<Object> value) {
		this.argumentName = name;
		this.argumentValue = value;
	}
	
	public String getArgumentName() {
		return argumentName;
	}

	public void setArgumentName(String argumentName) {
		this.argumentName = argumentName;
	}

	public List<Object> getArgumentValue() {
		return argumentValue;
	}

	public void setArgumentValue(List<Object> argumentValue) {
		this.argumentValue = argumentValue;
	}
}
