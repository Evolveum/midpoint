package com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CardObjectVisualizationPanel extends CardOutlineLeftPanel<ObjectVisualization> {

    private static final long serialVersionUID = 1L;

    private static final String ID_OBJECT_VISUALIZATION = "objectVisualization";

    public CardObjectVisualizationPanel(String id, IModel<ObjectVisualization> model) {
        super(id, model);

        initLayout();
    }

    // todo fix header icon, title & so on

    private void initLayout() {
        add(AttributeAppender.append("class", () -> VisualizationGuiUtil.createChangeTypeCssClassForOutlineCard(getModelObject().getChangeType())));

        ObjectVisualizationPanel objectVisualization = new ObjectVisualizationPanel(ID_OBJECT_VISUALIZATION, getModel());
        add(objectVisualization);
    }
}
