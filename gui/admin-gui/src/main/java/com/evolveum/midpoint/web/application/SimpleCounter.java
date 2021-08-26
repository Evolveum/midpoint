/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;

import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.model.IModel;

public class SimpleCounter<ODM extends ObjectDetailsModels<O>, O extends ObjectType> {

    private ObjectDetailsModels objectDetailsModels;

    public SimpleCounter() {

    }

    public int count(ODM objectDetailsModels) {
        return 0;
    }

    public SimpleCounter(ObjectDetailsModels objectDetailsModels) {
        this.objectDetailsModels = objectDetailsModels;
    }

    public ObjectDetailsModels getObjectDetailsModels() {
        return objectDetailsModels;
    }
}
