/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;

/**
 * @author lazyman
 */
public class InfoTooltipBehavior extends TooltipBehavior {

    private boolean isContainerModal = false;

    public InfoTooltipBehavior(){

    }

    public InfoTooltipBehavior(boolean isContainerModal){
        this.isContainerModal = isContainerModal;
    }

    @Override
    public String getModalContainer(Component component) {
        String id = component.getMarkupId();

        if(component.getParent() != null){
            Component parent = component.getParent();

            id = parent.getParent() != null? parent.getParent().getMarkupId() : parent.getMarkupId();
        }

        return id;
    }

    @Override
    public void onConfigure(Component component) {
        super.onConfigure(component);

        String cssClass = getCssClass();
        if (cssClass != null) {
            component.add(AttributeModifier.replace("class", cssClass));
        }
    }

    @Override
    public boolean isInsideModal() {
        return isContainerModal;
    }

    /**
     *  Override to provide custom css class (image, icon) for the tooltip
     * */
    public String getCssClass(){
        return "fa fa-fw fa-info-circle text-info";
    }
}
