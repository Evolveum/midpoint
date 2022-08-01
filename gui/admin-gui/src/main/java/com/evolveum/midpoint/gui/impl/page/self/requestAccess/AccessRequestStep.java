/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import org.apache.wicket.Page;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessRequestType;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface AccessRequestStep {

    default AccessRequestType getAccessRequestConfiguration(Page page) {
        CompiledGuiProfile profile = WebComponentUtil.getCompiledGuiProfile(page);
        if (profile == null) {
            return null;
        }

        return profile.getAccessRequest();
    }
}
