package com.evolveum.midpoint.prism.query;

import java.io.Serializable;
import com.evolveum.midpoint.util.Dumpable;

public class ObjectQuery implements Dumpable, Serializable{
	
	private ObjectFilter filter;
	
	
	public ObjectFilter getFilter() {
		return filter;
	}
	
	public void setFilter(ObjectFilter filter) {
		this.filter = filter;
	}
	
	public static ObjectQuery createObjectQuery(ObjectFilter filter){
		ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);
		return query;
	}

	
	@Override
	public String dump() {
		StringBuilder sb = new StringBuilder();
		
		if (filter == null){
			return "filter is null";
		}
		sb.append("Appending filter: ");
		sb.append("\n");
		sb.append(filter.dump());
		return sb.toString();
	}

}
