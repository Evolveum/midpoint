/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.prism.wrapper;

import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

/**
 * @author skublik
 *
 */
public interface ShadowWrapper extends PrismObjectWrapper<ShadowType> {

    UserDtoStatus getProjectionStatus();
    void setProjectionStatus(UserDtoStatus status);
    boolean isLoadWithNoFetch();
    void setLoadWithNoFetch(boolean noFetch);

    void setRelation(QName relation);
    QName getRelation();


    boolean isProtected();
    boolean isDead();
}
