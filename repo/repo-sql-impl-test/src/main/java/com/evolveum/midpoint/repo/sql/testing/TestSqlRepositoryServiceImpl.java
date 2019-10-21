/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.testing;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.DeleteObjectResult;
import com.evolveum.midpoint.repo.api.ModifyObjectResult;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.sql.SqlRepositoryFactory;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * @author lazyman
 */
//@Repository           // (perhaps) temporarily disabled
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
    @NotNull
    public <T extends ObjectType> DeleteObjectResult deleteObject(Class<T> type, String oid, OperationResult result) throws ObjectNotFoundException {
        try {
            return super.deleteObject(type, oid, result);
        } finally {
//            DBValidator.validateOwners(type, getConfiguration(), getSessionFactory());
        }
    }

    @Override
    @NotNull
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        try {
            return super.modifyObject(type, oid, modifications, result);
        } finally {
//            DBValidator.validateOwners(type, getConfiguration(), getSessionFactory());
        }
    }

    @Override
    @NotNull
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
                                                    RepoModifyOptions options, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        try {
            return super.modifyObject(type, oid, modifications, options, result);
        } finally {
//            DBValidator.validateOwners(type, getConfiguration(), getSessionFactory());
        }
    }
}
