/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.role;

import static com.evolveum.midpoint.repo.sqlbase.filtering.item.SimpleItemFilterProcessor.integerMapper;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType.F_DISPLAY_ORDER;

import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

/**
 * Mapping between {@link QService} and {@link ServiceType}.
 */
public class QServiceMapping
        extends QAbstractRoleMapping<ServiceType, QService, MService> {

    public static final String DEFAULT_ALIAS_NAME = "svc";

    public static final QServiceMapping INSTANCE = new QServiceMapping();

    private QServiceMapping() {
        super(QService.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ServiceType.class, QService.class);

        addItemMapping(F_DISPLAY_ORDER, integerMapper(path(q -> q.displayOrder)));
    }

    @Override
    protected QService newAliasInstance(String alias) {
        return new QService(alias);
    }

    @Override
    public ServiceSqlTransformer createTransformer(SqlTransformerSupport transformerSupport) {
        return new ServiceSqlTransformer(transformerSupport, this);
    }

    @Override
    public MService newRowObject() {
        return new MService();
    }
}
