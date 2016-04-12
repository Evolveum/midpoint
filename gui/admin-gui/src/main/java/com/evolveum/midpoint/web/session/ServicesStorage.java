package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.search.Search;

public class ServicesStorage implements PageStorage{

	private Search servicesSearch;
	private ObjectPaging servicesPaging;
	
	@Override
	public Search getSearch() {
		return servicesSearch;
	}

	@Override
	public void setSearch(Search search) {
		this.servicesSearch = search;
	}

	@Override
	public void setPaging(ObjectPaging paging) {
		this.servicesPaging = paging;
		
	}

	@Override
	public ObjectPaging getPaging() {
		return servicesPaging;
	}

}
