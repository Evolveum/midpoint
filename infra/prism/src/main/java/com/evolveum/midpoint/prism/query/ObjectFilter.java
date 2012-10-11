package com.evolveum.midpoint.prism.query;

import java.io.Serializable;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;



public abstract class ObjectFilter implements Dumpable, DebugDumpable, Serializable{

	private Element expression;

	ObjectFilter(Element expression) {
		this.expression = expression;
	}
	
	ObjectFilter() {
	}
	
	public Element getExpression() {
		return expression;
	}

	public void setExpression(Element expression) {
		this.expression = expression;
	}	
	
}
