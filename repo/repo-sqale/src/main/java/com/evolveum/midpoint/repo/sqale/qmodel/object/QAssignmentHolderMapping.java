/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

/**
 * Mapping for {@link AssignmentHolderType} - this is served by M_OBJECT mapping.
 * Technically, many mappings should be derived from this, but they are derived from
 * {@link QObjectMapping} instead and this class is ignored, except for mapping - just to support
 * queries typed to {@link AssignmentHolderType}.
 */
public class QAssignmentHolderMapping extends QObjectMapping<
        AssignmentHolderType,
        QAssignmentHolderMapping.QAssignmentHolder,
        QAssignmentHolderMapping.MAssignmentHolder> {

    public static final String DEFAULT_ALIAS_NAME = "ah";

    public static final QAssignmentHolderMapping INSTANCE = new QAssignmentHolderMapping();

    protected QAssignmentHolderMapping() {
        super(QObject.TABLE_NAME, DEFAULT_ALIAS_NAME,
                AssignmentHolderType.class, QAssignmentHolder.class);
    }

    public static class MAssignmentHolder extends MObject {
    }

    public static class QAssignmentHolder extends QObject<MAssignmentHolder> {
        private QAssignmentHolder() {
            super(MAssignmentHolder.class, DEFAULT_ALIAS_NAME);
        }
    }
}
