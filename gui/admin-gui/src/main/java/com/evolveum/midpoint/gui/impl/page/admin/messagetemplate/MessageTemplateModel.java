/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.messagetemplate;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTemplateType;

import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MessageTemplateModel extends AssignmentHolderDetailsModel<MessageTemplateType> {

    public MessageTemplateModel(LoadableDetachableModel<PrismObject<MessageTemplateType>> prismObjectModel, ModelServiceLocator serviceLocator) {
        super(prismObjectModel, serviceLocator);
    }
}
