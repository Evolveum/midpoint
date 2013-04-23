/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.page.admin.users.dto.UsersDto;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class UsersStorage implements Serializable {

    /**
     * DTO used for search in {@link com.evolveum.midpoint.web.page.admin.users.PageUsers}
     */
    private UsersDto usersSearch;
    /**
     * Paging DTO used in table on page {@link com.evolveum.midpoint.web.page.admin.users.PageUsers}
     */
    private ObjectPaging usersPaging;

    public ObjectPaging getUsersPaging() {
        return usersPaging;
    }

    public void setUsersPaging(ObjectPaging usersPaging) {
        this.usersPaging = usersPaging;
    }

    public UsersDto getUsersSearch() {
        return usersSearch;
    }

    public void setUsersSearch(UsersDto usersSearch) {
        this.usersSearch = usersSearch;
    }
}
