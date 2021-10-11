package com.evolveum.midpoint.prism.metadata;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.OriginType;

// FIXME: This should be part of midpoint schema / model
public interface MidpointOriginMetadata {

    void setOriginObject(Objectable source);

    void setOriginType(OriginType type);

    OriginType getOriginType();

    Objectable getOriginObject();

}
