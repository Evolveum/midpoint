/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.messagetemplate;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTemplateType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LocalizedContentCounter extends SimpleCounter<AssignmentHolderDetailsModel<MessageTemplateType>, MessageTemplateType> {

    public LocalizedContentCounter() {
        super();
    }

    @Override
    public int count(AssignmentHolderDetailsModel<MessageTemplateType> model, PageBase pageBase) {
        MessageTemplateType message = model.getObjectType();
        return message.getLocalizedContent().size();
    }
}
