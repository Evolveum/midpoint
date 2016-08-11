package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.ResourceContentSearchDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

public class ResourceContentStorage implements PageStorage {

	private static final long serialVersionUID = 1L;
	
	private ResourceContentSearchDto contentSearch;

	private Search attributeSearch;
	private ObjectPaging paging;
	
	private ShadowKindType kind;
	
	public ResourceContentStorage(ShadowKindType kind) {
		this.kind = kind;
	}
	
	@Override
	public Search getSearch() {
		return attributeSearch;
	}

	@Override
	public void setSearch(Search search) {
		this.attributeSearch = search;
	}

	@Override
	public void setPaging(ObjectPaging paging) {
		this.paging = paging;
		
	}

	@Override
	public ObjectPaging getPaging() {
		return paging;
	}
	
	public ResourceContentSearchDto getContentSearch() {
		if (contentSearch == null) {
			return new ResourceContentSearchDto(kind);
		}
		return contentSearch;
	}
	
	public void setContentSearch(ResourceContentSearchDto contentSearch) {
		this.contentSearch = contentSearch;
	}
	
}
