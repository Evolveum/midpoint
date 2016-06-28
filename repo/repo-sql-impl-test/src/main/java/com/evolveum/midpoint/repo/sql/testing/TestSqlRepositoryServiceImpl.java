/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.repo.sql.testing;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.sql.SqlRepositoryFactory;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * @author lazyman
 */
@Repository
public class TestSqlRepositoryServiceImpl extends SqlRepositoryServiceImpl {

    public TestSqlRepositoryServiceImpl(SqlRepositoryFactory repositoryFactory) {
        super(repositoryFactory);
    }

    @Override
    public <T extends ObjectType> String addObject(PrismObject<T> object, RepoAddOptions options, OperationResult result) throws ObjectAlreadyExistsException, SchemaException {
        try {
            return super.addObject(object, options, result);
        } finally {
//            DBValidator.validateOwners(object.getCompileTimeClass(), getConfiguration(), getSessionFactory());
        }
    }

    @Override
    public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult result) throws ObjectNotFoundException {
        try {
            super.deleteObject(type, oid, result);
        } finally {
//            DBValidator.validateOwners(type, getConfiguration(), getSessionFactory());
        }
    }

    @Override
    public <T extends ObjectType> void modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        try {
            super.modifyObject(type, oid, modifications, result);
        } finally {
//            DBValidator.validateOwners(type, getConfiguration(), getSessionFactory());
        }
    }

    @Override
    public <T extends ObjectType> void modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
                                                    RepoModifyOptions options, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        try {
            super.modifyObject(type, oid, modifications, options, result);
        } finally {
//            DBValidator.validateOwners(type, getConfiguration(), getSessionFactory());
        }
    }
}
