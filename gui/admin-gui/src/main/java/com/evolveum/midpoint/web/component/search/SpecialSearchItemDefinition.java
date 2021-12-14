package com.evolveum.midpoint.web.component.search;

import org.apache.wicket.model.IModel;

public abstract class SpecialSearchItemDefinition<O extends Object> extends AbstractSearchItemDefinition {

    IModel<O> valueModel;

    public SpecialSearchItemDefinition(IModel<O> valueModel) {
        this.valueModel = valueModel;
    }
}
