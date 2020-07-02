/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.util;

import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractFormItemType;

/**
 * Class for misc GUI util methods (impl).
 *
 * @author semancik
 *
 */
public class GuiImplUtil {

    public static ItemPath getItemPath(AbstractFormItemType aItem) {
        if (aItem != null && aItem.getBinding() != null && aItem.getBinding().getPath() != null) {
            return aItem.getBinding().getPath().getItemPath();
        } else {
            return null;
        }
    }

    public static <C extends Containerable> String getObjectStatus(Object object) {

        if (object instanceof PrismContainerValueWrapper) {
            PrismContainerValueWrapper<C> container = (PrismContainerValueWrapper<C>) object;
            if (container.getParent() instanceof ShadowWrapper) {
                if (((ShadowWrapper) container.getParent()).getProjectionStatus().equals(UserDtoStatus.DELETE)) {
                    return "danger";
                }
                if (((ShadowWrapper) container.getParent()).getProjectionStatus().equals(UserDtoStatus.UNLINK)) {
                    return "warning";
                }
            }

            switch (container.getStatus()) {
                case ADDED:
                    return "success";
                case DELETED:
                    return "danger";
                case NOT_CHANGED:
                default:
                    return null;
            }
        }
        return null;
    }

}
