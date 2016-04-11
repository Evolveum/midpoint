package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.page.admin.roles.RoleMemberSearchDto;

public class RoleMembersStorage implements PageStorage{

	 /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private RoleMemberSearchDto roleMemberSearch;
	
	private Search search;

	    /**
	     *  Paging DTO used in table on page {@link com.evolveum.midpoint.web.page.admin.roles.PageRoles}
	     * */
	    private ObjectPaging rolesPaging;

	    public RoleMemberSearchDto getRoleMemberSearch() {
	        return roleMemberSearch;
	    }

	    public void setRoleMemberSearch(RoleMemberSearchDto roleMemberSearch) {
	        this.roleMemberSearch = roleMemberSearch;
	    }

	    @Override
	    public ObjectPaging getPaging() {
	        return rolesPaging;
	    }

	    @Override
	    public void setPaging(ObjectPaging rolesPaging) {
	        this.rolesPaging = rolesPaging;
	    }
	    
	    @Override
	    public Search getSearch() {
			return search;
		}
	    
	    @Override
	    public void setSearch(Search search) {
	    	this.search = search;
	    }
	
}
