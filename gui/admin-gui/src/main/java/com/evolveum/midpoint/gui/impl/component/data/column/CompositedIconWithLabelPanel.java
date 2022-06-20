package com.evolveum.midpoint.gui.impl.component.data.column;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

public class CompositedIconWithLabelPanel extends CompositedIconPanel {

    private static final String ID_LABEL = "label";

    private IModel<String> labelModel;

    public CompositedIconWithLabelPanel(String id, IModel<CompositedIcon> compositedIcon, IModel<String> labelModel) {
        super(id, compositedIcon);
        this.labelModel = labelModel;
    }

    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Label label = new Label(ID_LABEL, labelModel);
        label.setOutputMarkupId(true);
        add(label);
    }
}
