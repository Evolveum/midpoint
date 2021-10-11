/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.prism;

import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author skublik
 *
 */
public interface ShadowWrapper extends PrismObjectWrapper<ShadowType> {

    UserDtoStatus getProjectionStatus();
    void setProjectionStatus(UserDtoStatus status);
    boolean isLoadWithNoFetch();
    void setLoadWithNoFetch(boolean noFetch);


    boolean isProtected();
}
