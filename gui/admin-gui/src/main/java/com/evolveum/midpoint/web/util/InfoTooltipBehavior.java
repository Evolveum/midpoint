/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

        component.add(AttributeModifier.replace("class", "fa fa-fw fa-info-circle text-info"));
    }

    @Override
    public boolean isInsideModal() {
        return isContainerModal;
    }
}
