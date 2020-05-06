/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import org.apache.commons.lang3.BooleanUtils;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author skublik
 *
 */
public class ShadowWrapperImpl extends PrismObjectWrapperImpl<ShadowType> implements ShadowWrapper {

    private static final long serialVersionUID = 1L;

    UserDtoStatus status;
    boolean noFetch = false;

    public ShadowWrapperImpl(PrismObject<ShadowType> item, ItemStatus status) {
        super(item, status);
    }

    @Override
    public UserDtoStatus getProjectionStatus() {
        return status;
    }

    @Override
    public void setProjectionStatus(UserDtoStatus status) {
        this.status = status;
    }

    @Override
    public boolean isLoadWithNoFetch() {
        return noFetch;
    }

    @Override
    public void setLoadWithNoFetch(boolean noFetch) {
        this.noFetch = noFetch;
    }


    @Override
    public boolean isProtected() {
        if (getObject() == null) {
            return false;
        }

        ShadowType shadowType = getObject().asObjectable();
        return BooleanUtils.isTrue(shadowType.isProtectedObject());
    }
}
