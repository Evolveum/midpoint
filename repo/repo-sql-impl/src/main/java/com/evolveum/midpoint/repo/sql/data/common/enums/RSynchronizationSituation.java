/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

/**
 * @author lazyman
 */
@JaxbType(type = SynchronizationSituationType.class)
public enum RSynchronizationSituation implements SchemaEnum<SynchronizationSituationType> {

    DELETED(SynchronizationSituationType.DELETED),
    UNMATCHED(SynchronizationSituationType.UNMATCHED),
    DISPUTED(SynchronizationSituationType.DISPUTED),
    LINKED(SynchronizationSituationType.LINKED),
    UNLINKED(SynchronizationSituationType.UNLINKED);

    private SynchronizationSituationType syncType;

    RSynchronizationSituation(SynchronizationSituationType syncType) {
        this.syncType = syncType;
        RUtil.register(this);
    }

    @Override
    public SynchronizationSituationType getSchemaValue() {
        return syncType;
    }
}
