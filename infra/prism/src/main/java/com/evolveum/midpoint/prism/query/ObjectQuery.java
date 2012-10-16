package com.evolveum.midpoint.prism.query;

import java.io.Serializable;
import com.evolveum.midpoint.util.Dumpable;

public class ObjectQuery implements Dumpable, Serializable {

	private ObjectFilter filter;
	private ObjectPaging paging;

	public ObjectFilter getFilter() {
		return filter;
	}

	public void setFilter(ObjectFilter filter) {
		this.filter = filter;
	}

	public void setPaging(ObjectPaging paging) {
		this.paging = paging;
	}

	public ObjectPaging getPaging() {
		return paging;
	}

	public static ObjectQuery createObjectQuery(ObjectFilter filter) {
		ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);
		return query;
	}

	public static ObjectQuery createObjectQuery(ObjectPaging paging) {
		ObjectQuery query = new ObjectQuery();
		query.setPaging(paging);
		return query;
	}

	public static ObjectQuery createObjectQuery(ObjectFilter filter, ObjectPaging paging) {
		ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);
		query.setPaging(paging);
		return query;
	}

	@Override
	public String dump() {
		StringBuilder sb = new StringBuilder();

		if (filter == null) {
			sb.append("filter is null");
		} else {
			sb.append("Appending filter: ");
			sb.append("\n");
			sb.append(filter.dump());
		}
		if (paging == null) {
			sb.append("paging is null");
		} else {
			sb.append(paging.dump());
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Q{");
		if (filter != null) {
			sb.append(filter.toString());
			sb.append(",");
		} else {
			sb.append("null filter");
		}
		if (paging != null) {
			sb.append(paging.toString());
			sb.append(",");
		} else {
			sb.append("null paging");
		}
		return sb.toString();
	}

}
