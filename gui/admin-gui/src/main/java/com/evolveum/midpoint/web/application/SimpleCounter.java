/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class SimpleCounter<ODM extends ObjectDetailsModels<O>, O extends ObjectType> {

    public SimpleCounter() {

    }

    public int count(ODM objectDetailsModels, PageBase pageBase) {
        return 0;
    }

}
