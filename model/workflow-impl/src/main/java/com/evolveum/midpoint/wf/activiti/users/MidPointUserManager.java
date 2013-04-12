/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf.activiti.users;

import org.activiti.engine.identity.User;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.activiti.engine.impl.persistence.entity.UserEntityManager;

import java.util.List;

public class MidPointUserManager extends UserEntityManager {

    @Override
    public User createNewUser(String userId) {
        throw new UnsupportedOperationException("MidPoint user manager doesn't support creating a new user");
    }

    @Override
    public void insertUser(User user) {
        throw new UnsupportedOperationException("MidPoint user manager doesn't support inserting a new user");
    }

    @Override
    public void updateUser(UserEntity updatedUser) {
        throw new UnsupportedOperationException("MidPoint user manager doesn't support updating a user");
    }

    @Override
    public UserEntity findUserById(String userId) {
        throw new UnsupportedOperationException("MidPoint user manager doesn't support finding a user by id");
    }

    @Override
    public void deleteUser(String userId) {
        throw new UnsupportedOperationException("MidPoint user manager doesn't support deleting a user");
    }

//    @Override
//    public List<User> findUserByQueryCriteria(Object query, Page page) {
//
//        throw new UnsupportedOperationException("MidPoint user manager doesn't support finding a user by query criteria");

//        List<User> userList = new ArrayList<User>();
//
//        // Query is a UserQueryImpl instance
//        UserQueryImpl userQuery = (UserQueryImpl) query;
//        StringBuilder searchQuery = new StringBuilder();
//        if (StringUtils.isNotEmpty(userQuery.getId())) {
//            searchQuery.append("(uid=").append(userQuery.getId()).append(")");
//
//        } else if (StringUtils.isNotEmpty(userQuery.getLastName())) {
//            searchQuery.append("(sn=").append(userQuery.getLastName()).append(")");
//
//        } else {
//            searchQuery.append("(uid=*)");
//        }
//        LdapConnection connection = LDAPConnectionUtil.openConnection(connectionParams);
//        try {
//            Cursor<SearchResponse> cursor = connection.search(USER_GROUP, searchQuery.toString(), SearchScope.ONELEVEL, "*");
//            while (cursor.next()) {
//                User user = new UserEntity();
//                SearchResultEntry response = (SearchResultEntry) cursor.get();
//                Iterator<EntryAttribute> itEntry = response.getEntry().iterator();
//                while (itEntry.hasNext()) {
//                    EntryAttribute attribute = itEntry.next();
//                    String key = attribute.getId();
//                    if ("uid".equalsIgnoreCase(key)) {
//                        user.setId(attribute.getString());
//
//                    } else if ("sn".equalsIgnoreCase(key)) {
//                        user.setLastName(attribute.getString());
//
//                    } else if ("cn".equalsIgnoreCase(key)) {
//                        user.setFirstName(attribute.getString().substring(0, attribute.getString().indexOf(" ")));
//                    }
//                }
//
//                userList.add(user);
//            }
//
//            cursor.close();
//
//        } catch (Exception e) {
//            throw new ActivitiException("LDAP connection search failure", e);
//        }
//
//        LDAPConnectionUtil.closeConnection(connection);
//
//        return userList;
//    }

//    @Override
//    public long findUserCountByQueryCriteria(Object query) {
//        return findUserByQueryCriteria(query, null).size();
//    }

    @Override
    public Boolean checkPassword(String userId, String password) {
        return true;
    }
}
