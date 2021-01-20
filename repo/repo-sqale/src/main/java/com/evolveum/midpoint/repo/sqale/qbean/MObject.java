/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qbean;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.MObjectTypeMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.QObject;

/**
 * Querydsl "row bean" type related to {@link QObject}.
 */
public class MObject {

    public String oid;
    public int objectTypeClass = MObjectTypeMapping.OBJECT.code();
    public String nameNorm;
    public String nameOrig;
    public byte[] fullObject;
    public Integer createChannelId;

    public Integer modifyChannelId;

    /*
    fullObject BYTEA,
    createChannel INTEGER REFERENCES m_qname(id),
    createTimestamp TIMESTAMPTZ,
    creatorRef_relation VARCHAR(157),
    creatorRef_targetOid VARCHAR(36),
    creatorRef_targetType INTEGER,
    lifecycleState VARCHAR(255),
    modifierRef_relation VARCHAR(157),
    modifierRef_targetOid VARCHAR(36),
    modifierRef_targetType INTEGER,
    modifyChannel INTEGER REFERENCES m_qname(id),
    modifyTimestamp TIMESTAMPTZ,
    tenantRef_relation VARCHAR(157),
    tenantRef_targetOid VARCHAR(36),
    tenantRef_targetType INTEGER,
    version INTEGER NOT NULL DEFAULT 1,
    -- add GIN index for concrete tables where more than thousands of entries are expected (see m_user)
    ext JSONB,
    */

    public PolyString getName() {
        return new PolyString(nameOrig, nameNorm);
    }

}
