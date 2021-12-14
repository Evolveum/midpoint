/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import org.apache.wicket.model.IModel;

public class OidSearchItemDefinition extends SpecialSearchItemDefinition {

    public OidSearchItemDefinition(IModel valueModel) {
        super(valueModel);
    }

    @Override
    public String getName() {
        return ""; //todo implement
    }

    @Override
    public String getHelp(){
        return ""; //todo implement
    }

    @Override
    public SearchItem<OidSearchItemDefinition> createSearchItem() {
        return null; //todo implement
    }

}
