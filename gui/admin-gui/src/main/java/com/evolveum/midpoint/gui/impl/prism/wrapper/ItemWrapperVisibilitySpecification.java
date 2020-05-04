/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;

public class ItemWrapperVisibilitySpecification<IW extends ItemWrapper<?,?>> {

    private IW itemWrapper;

    public ItemWrapperVisibilitySpecification(IW itemWrapper) {
        this.itemWrapper = itemWrapper;
    }

    //TODO visible for object always true


//    public boolean isVisible(PrismContainerWrapper containerWrapper, ItemVisibilityHandler visibilityHandler) {
//
//        if (containerWrapper == null) {
//             return false;
//        }
//
//        if (containerWrapper instanceof PrismObjectWrapper) {
//            return true;
//        }
//
//        if (containerWrapper.isVirtual() && containerWrapper.getVisibleOverwrite() != null
//                && UserInterfaceElementVisibilityType.HIDDEN == containerWrapper.getVisibleOverwrite()) {
//            return false;
//        }
//
//        PrismContainerValueWrapper<?> parent = containerWrapper.getParent();
//        if (containerWrapper.getComplexTypeDefinition().getTypeName().equals(MetadataType.COMPLEX_TYPE)) {
//            return (parent != null && parent.isShowMetadata());
//        }
//
//        // pretend that object is always expanded. it is becasue all other containers are children of it
//        // and it can influence visibility behavior on different tabs.
//        boolean parentExpanded = parent instanceof PrismObjectValueWrapper ? true : parent.isExpanded();
//        return isVisibleByVisibilityHandler(parentExpanded, containerWrapper, visibilityHandler);
//    }

//    public <IW extends ItemWrapper<?,?>> boolean isVisible(ItemStatus objectStatus, ItemVisibilityHandler visibilityHandler) {
//
//        PrismContainerValueWrapper<?> parentContainer = itemWrapper.getParent();
//
//        if (!isVisibleByVisibilityHandler(parentContainer.isExpanded(), itemWrapper, visibilityHandler)) {
//            return false;
//        }
//
//        if (!parentContainer.isVirtual() && itemWrapper.isShowInVirtualContainer()) {
//            return false;
//        }
//
//        switch (objectStatus) {
//            case NOT_CHANGED:
//                return isVisibleForModify(parentContainer.isShowEmpty(), itemWrapper);
//            case ADDED:
//                return isVisibleForAdd(parentContainer.isShowEmpty(), itemWrapper);
//            case DELETED:
//                return false;
//        }
//
//        return false;
//    }
//
//
//    protected <IW extends ItemWrapper<?,?>> boolean isVisibleByVisibilityHandler(boolean parentExpanded, IW itemWrapper, ItemVisibilityHandler visibilityHandler) {
//        if (!parentExpanded) {
//            return false;
//        }
//
//
//        if (visibilityHandler != null) {
//            ItemVisibility visible = visibilityHandler.isVisible(itemWrapper);
//            if (visible != null) {
//                switch (visible) {
//                    case HIDDEN:
//                        return false;
//                    default:
//                        // automatic, go on ...
//                }
//            }
//        }
//
//        return true;
//
//    }
//
//    private <IW extends ItemWrapper<?,?>> boolean isVisibleForModify(boolean parentShowEmpty, IW itemWrapper) {
//        if (parentShowEmpty) {
//            return true;
//        }
//
//        return itemWrapper.isEmphasized() || !itemWrapper.isEmpty();
//    }
//
//    private <IW extends ItemWrapper<?,?>> boolean isVisibleForAdd(boolean parentShowEmpty, IW itemWrapper) {
//        if (parentShowEmpty) {
//            return true;
//        }
//
//        return itemWrapper.isEmphasized() || !itemWrapper.isEmpty();
//    }
}
