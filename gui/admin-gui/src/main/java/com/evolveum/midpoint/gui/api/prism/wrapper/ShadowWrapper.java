/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
