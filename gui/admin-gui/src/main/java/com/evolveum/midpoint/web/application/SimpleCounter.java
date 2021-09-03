/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
