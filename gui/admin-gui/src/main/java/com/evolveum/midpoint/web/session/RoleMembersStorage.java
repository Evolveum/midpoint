package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.page.admin.roles.RoleMemberSearchDto;

public class RoleMembersStorage extends PageStorage{

	 /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private RoleMemberSearchDto roleMemberSearch;

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

	    public ObjectPaging getRolesPaging() {
	        return rolesPaging;
	    }

	    public void setRolesPaging(ObjectPaging rolesPaging) {
	        this.rolesPaging = rolesPaging;
	    }
	
}
