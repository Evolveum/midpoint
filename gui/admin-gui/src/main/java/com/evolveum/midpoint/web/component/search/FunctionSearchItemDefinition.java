/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

import java.util.function.Function;

public class FunctionSearchItemDefinition extends SpecialSearchItemDefinition<Function<Search, SearchItem>> {

    public FunctionSearchItemDefinition(IModel<Function<Search, SearchItem>> valueModel) {
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
    public SearchItem<FilterSearchItemDefinition> createSearchItem() {
        return null; //todo implement
    }

}
