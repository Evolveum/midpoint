package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.AbstractSearchItemWrapper;
import org.apache.wicket.model.IModel;

public class AbstractSearchItemPanel<W extends AbstractSearchItemWrapper> extends BasePanel<W> {

    public AbstractSearchItemPanel(String id, IModel<W> model) {
        super(id, model);
    }
}
