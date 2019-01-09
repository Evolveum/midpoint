package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_4.AbstractRoleType;

public class VirtualAssignmenetSpecification<R extends AbstractRoleType> {
	
	private ObjectFilter filter;
	private Class<R> type;
	
	public ObjectFilter getFilter() {
		return filter;
	}
	
	public Class<R> getType() {
		return type;
	}	

	public void setFilter(ObjectFilter filter) {
		this.filter = filter;
	}
	
	public void setType(Class<R> type) {
		this.type = type;
	}
}
