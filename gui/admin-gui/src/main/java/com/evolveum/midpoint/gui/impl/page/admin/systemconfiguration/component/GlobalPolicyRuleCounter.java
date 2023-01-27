/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;

/**
 * Created by Viliam Repan (lazyman).
 *
 * TODO actual implementation not very nice since it contains multiple ifs based on for which
 * {@link com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType} and specific field we're doing the count
 * <p/>
 * It either should be multiple classes per concrete use (type and itempath) or probably better pointer to container
 * which values we want to count (in this case GlobalPolicyRuleType container)
 */
public class GlobalPolicyRuleCounter extends SimpleCounter<AssignmentHolderDetailsModel<AssignmentHolderType>, AssignmentHolderType> {

    public GlobalPolicyRuleCounter() {
        super();
    }

    @Override
    public int count(AssignmentHolderDetailsModel<AssignmentHolderType> model, PageBase pageBase) {
        AssignmentHolderType object = model.getObjectType();
        if (object instanceof SystemConfigurationType) {
            return ((SystemConfigurationType) object).getGlobalPolicyRule().size();
        }

        if (object instanceof TagType) {
            return ((TagType) object).getPolicyRule().size();
        }

        return 0;
    }
}

