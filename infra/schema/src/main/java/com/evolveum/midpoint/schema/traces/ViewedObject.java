/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@Experimental
public class ViewedObject {
    private String label;
    private PrismObject<?> object;

    public ViewedObject(String label, PrismObject<? extends ObjectType> object) {
        this.label = label;
        this.object = object;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public PrismObject<?> getObject() {
        return object;
    }
    public void setObject(PrismObject<?> object) {
        this.object = object;
    }


}
