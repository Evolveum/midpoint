/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class UserSqlTransformer
        extends FocusSqlTransformer<UserType, QUser, MUser> {

    public UserSqlTransformer(
            SqlTransformerSupport transformerSupport, QUserMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull MUser toRowObjectWithoutFullObject(
            UserType schemaObject, JdbcSession jdbcSession) {
        MUser mUser = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        // TODO user fields

        return mUser;
    }
}
