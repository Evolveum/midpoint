/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.other;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormType;

/**
 * Mapping between {@link QForm} and {@link FormType}.
 */
public class QFormMapping extends QObjectMapping<FormType, QForm, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "form";

    public static final QFormMapping INSTANCE = new QFormMapping();

    private QFormMapping() {
        super(QForm.TABLE_NAME, DEFAULT_ALIAS_NAME,
                FormType.class, QForm.class);
    }

    @Override
    protected QForm newAliasInstance(String alias) {
        return new QForm(alias);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
