/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

/**
 * Mapping between {@link QObjectTemplate} and {@link ObjectTemplateType}.
 */
public class QObjectTemplateMapping
        extends QObjectMapping<ObjectTemplateType, QObjectTemplate, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "ot";

    public static final QObjectTemplateMapping INSTANCE = new QObjectTemplateMapping();

    private QObjectTemplateMapping() {
        super(QObjectTemplate.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ObjectTemplateType.class, QObjectTemplate.class);
    }

    @Override
    protected QObjectTemplate newAliasInstance(String alias) {
        return new QObjectTemplate(alias);
    }

    @Override
    public ObjectTemplateSqlTransformer createTransformer(
            SqlTransformerSupport transformerSupport) {
        return new ObjectTemplateSqlTransformer(transformerSupport, this);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
