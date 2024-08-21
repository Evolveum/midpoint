/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
