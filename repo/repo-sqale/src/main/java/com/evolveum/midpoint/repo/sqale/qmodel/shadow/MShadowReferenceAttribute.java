package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;

import java.util.UUID;

public class MShadowReferenceAttribute extends MReference {

    // Helps with joins with partitioning
    public Integer pathId;
    public Integer ownerObjectClassId;
    public UUID resourceOid;

}
