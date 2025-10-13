/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.wf;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import org.apache.wicket.Component;

public class WfGuiUtil {

    /**
     * Creates localized process instance name from the workflow context (if possible); otherwise returns null.
     */
    public static String getLocalizedProcessName(ApprovalContextType wfc, Component component) {
//        if (wfc != null && !wfc.getLocalizableProcessInstanceName().isEmpty()) {
//            return wfc.getLocalizableProcessInstanceName().stream()
//                    .map(p -> WebComponentUtil.resolveLocalizableMessage(p, component))
//                    .collect(Collectors.joining(" "));
//        } else {
//            return null;
//        }
        return null;        // todo get from the CaseType
    }

    public static String getLocalizedTaskName(ApprovalContextType wfc, Component component) {
//        if (wfc != null && wfc.getLocalizableTaskName() != null) {
//            return WebComponentUtil.resolveLocalizableMessage(wfc.getLocalizableTaskName(), component);
//        } else {
//            return null;
//        }
        return null;        // todo get from the CaseType
    }

}
