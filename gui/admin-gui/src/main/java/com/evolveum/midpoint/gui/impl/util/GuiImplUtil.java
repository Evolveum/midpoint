/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.util;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractFormItemType;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.lang.reflect.Field;

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
            PrismContainerValueWrapper<C> containerValue = (PrismContainerValueWrapper<C>) object;
            if (containerValue.getParent() instanceof ShadowWrapper) {
                if (((ShadowWrapper) containerValue.getParent()).getProjectionStatus().equals(UserDtoStatus.DELETE)) {
                    return "table-danger";
                }
                if (((ShadowWrapper) containerValue.getParent()).getProjectionStatus().equals(UserDtoStatus.UNLINK)) {
                    return "table-warning";
                }
            }

            switch (containerValue.getStatus()) {
                case ADDED:
                    return "table-success";
                case DELETED:
                    return "table-danger";
                case NOT_CHANGED:
                default:
                    return null;
            }
        }

        if (object instanceof PrismContainerWrapper) {
            PrismContainerWrapper<C> container = (PrismContainerWrapper<C>) object;

            if (container.getParent() instanceof ShadowWrapper) {
                if (((ShadowWrapper) container.getParent()).getProjectionStatus().equals(UserDtoStatus.DELETE)) {
                    return "table-danger";
                }
                if (((ShadowWrapper) container.getParent()).getProjectionStatus().equals(UserDtoStatus.UNLINK)) {
                    return "table-warning";
                }
            }

            switch (container.getStatus()) {
                case ADDED:
                    return "table-success";
                case DELETED:
                    return "table-danger";
                case NOT_CHANGED:
                default:
                    return null;
            }
        }
        return null;
    }

    public static <C extends Containerable> QName getContainerableTypeName(@NotNull Class<C> containerable) throws IllegalAccessException, NoSuchFieldException {
        Field field = FieldUtils.getField(containerable, "COMPLEX_TYPE");
        if (field == null) {
            throw new NoSuchFieldException("Field COMPLEX_TYPE not found in " + containerable.getName());
        }
        return (QName) field.get(containerable);
    }
}
