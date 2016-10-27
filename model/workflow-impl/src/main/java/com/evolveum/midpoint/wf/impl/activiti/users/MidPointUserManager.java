/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.activiti.users;

import org.activiti.engine.identity.User;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.activiti.engine.impl.persistence.entity.UserEntityManager;

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
    public UserEntity findUserById(String userId) {
        throw new UnsupportedOperationException("MidPoint user manager doesn't support finding a user by id");
    }

    @Override
    public void deleteUser(String userId) {
        throw new UnsupportedOperationException("MidPoint user manager doesn't support deleting a user");
    }

    @Override
    public Boolean checkPassword(String userId, String password) {
        return true;
    }
}
