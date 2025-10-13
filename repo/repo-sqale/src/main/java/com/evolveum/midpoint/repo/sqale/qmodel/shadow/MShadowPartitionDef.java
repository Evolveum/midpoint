/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import java.util.UUID;

public class MShadowPartitionDef {

    public UUID resourceOid;
    public Integer objectClassId;
    public boolean partition;
    public boolean attached;
    public String table;

}
