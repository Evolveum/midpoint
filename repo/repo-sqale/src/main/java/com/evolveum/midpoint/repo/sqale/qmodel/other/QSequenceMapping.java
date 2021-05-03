/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.other;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SequenceType;

/**
 * Mapping between {@link QSequence} and {@link SequenceType}.
 */
public class QSequenceMapping extends QObjectMapping<SequenceType, QSequence, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "seq";

    public static final QSequenceMapping INSTANCE = new QSequenceMapping();

    private QSequenceMapping() {
        super(QSequence.TABLE_NAME, DEFAULT_ALIAS_NAME,
                SequenceType.class, QSequence.class);
    }

    @Override
    protected QSequence newAliasInstance(String alias) {
        return new QSequence(alias);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
