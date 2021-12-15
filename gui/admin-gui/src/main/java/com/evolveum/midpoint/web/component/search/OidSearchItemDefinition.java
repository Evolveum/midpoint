/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import org.apache.wicket.model.IModel;

public class OidSearchItemDefinition extends SpecialSearchItemDefinition {

    private static String oid = "";

    private static IModel<String> valueModel = new IModel<String>() {
        @Override
        public void setObject(String value) {
            oid = value;
        }

        @Override
        public String getObject() {
            return oid;
        }
    };

    public OidSearchItemDefinition() {
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
    public boolean isSearchItemDisplayed() {
        return false;
    }

    @Override
    public SearchItem<OidSearchItemDefinition> createSearchItem() {
        return new OidSearchItem(null,this);
    }

}
