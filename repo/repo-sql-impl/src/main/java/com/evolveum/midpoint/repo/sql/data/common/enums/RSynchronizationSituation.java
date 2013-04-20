package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;

public enum RSynchronizationSituation implements SchemaEnum<SynchronizationSituationType> {

    DELETED(SynchronizationSituationType.DELETED),
    UNMATCHED(SynchronizationSituationType.UNMATCHED),
    DISPUTED(SynchronizationSituationType.DISPUTED),
    LINKED(SynchronizationSituationType.LINKED),
    UNLINKED(SynchronizationSituationType.UNLINKED);

    private SynchronizationSituationType syncType;

    private RSynchronizationSituation(SynchronizationSituationType syncType) {
        this.syncType = syncType;
    }

    @Override
    public SynchronizationSituationType getSchemaValue() {
        return syncType;
    }
}
