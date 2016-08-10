package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.ResourceContentSearchDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

public class ResourceContentStorage implements PageStorage {

	private static final long serialVersionUID = 1L;
	
	private ResourceContentSearchDto accountContentSearch;
	private ResourceContentSearchDto entitlementContentSearch;
	private ResourceContentSearchDto genericContentSearch;
	private ResourceContentSearchDto objectClassContentSearch;

	@Override
	public Search getSearch() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSearch(Search search) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPaging(ObjectPaging paging) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ObjectPaging getPaging() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public ResourceContentSearchDto getAccountContentSearch() {
		if (accountContentSearch == null) {
			return new ResourceContentSearchDto(ShadowKindType.ACCOUNT);
		}
		return accountContentSearch;
	}
	
	public void setAccountContentSearch(ResourceContentSearchDto accountContentSearch) {
		this.accountContentSearch = accountContentSearch;
	}
	
	public ResourceContentSearchDto getEntitlementContentSearch() {
		if (entitlementContentSearch == null) {
			return new ResourceContentSearchDto(ShadowKindType.ENTITLEMENT);
		}
		return entitlementContentSearch;
	}
	
	public void setEntitlementContentSearch(ResourceContentSearchDto entitlementContentSearch) {
		this.entitlementContentSearch = entitlementContentSearch;
	}
	
	public ResourceContentSearchDto getGenericContentSearch() {
		if (genericContentSearch == null) {
			return new ResourceContentSearchDto(ShadowKindType.GENERIC);
		}
		return genericContentSearch;
	}
	
	public void setGenericContentSearch(ResourceContentSearchDto genericContentSearch) {
		this.genericContentSearch = genericContentSearch;
	}
	
	public ResourceContentSearchDto getObjectClassContentSearch() {
		if (objectClassContentSearch == null) {
			return new ResourceContentSearchDto(null);
		}
		return objectClassContentSearch;
	}
	
	public void setObjectClassContentSearch(ResourceContentSearchDto objectClassContentSearch) {
		this.objectClassContentSearch = objectClassContentSearch;
	}
	

}
