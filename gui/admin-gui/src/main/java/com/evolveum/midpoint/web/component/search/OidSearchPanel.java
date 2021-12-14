package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.web.component.input.TextPanel;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

public class OidSearchPanel extends SearchSpecialItemPanel {

    public OidSearchPanel(String id, IModel<OidSearchItem> model) {
        super(id, model);
    }

    @Override
    protected WebMarkupContainer initSearchItemField(String id) {
        TextPanel<String> inputPanel = new TextPanel<String>(id, getModelValue());
        inputPanel.getBaseFormComponent().add(AttributeAppender.append("style", "width: 220px; max-width: 400px !important;"));
        return inputPanel;
    }

    @Override
    protected IModel<String> createLabelModel() {
        return getPageBase().createStringResource("SearchPanel.oid");
    }

    @Override
    protected IModel<String> createHelpModel() {
        return getPageBase().createStringResource("SearchPanel.oid.help");
    }
}
