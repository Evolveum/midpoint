/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author skublik
 *
 */
public class ShadowAssociationReferenceWrapperImpl<R extends Referencable> extends PrismReferenceWrapperImpl<R> {

    private static final long serialVersionUID = 1L;

    private String displayName;

    public ShadowAssociationReferenceWrapperImpl(PrismContainerValueWrapper<?> parent, PrismReference item,
            ItemStatus status) {
        super(parent, item, status);
    }


    @Override
    public String getDisplayName() {
        if(displayName != null) {
            return displayName;
        }
        return super.getDisplayName();
    }

    @Override
    public QName getTargetTypeName() {
        return ShadowType.COMPLEX_TYPE;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
