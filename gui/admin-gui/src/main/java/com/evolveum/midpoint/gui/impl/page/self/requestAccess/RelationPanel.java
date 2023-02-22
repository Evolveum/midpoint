/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardStepPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.page.self.PageRequestAccess;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RelationPanel extends BasicWizardStepPanel<RequestAccess> implements AccessRequestStep {

    private static final long serialVersionUID = 1L;

    public static final String STEP_ID = "relation";

    private static final String ID_PANEL = "panel";

    private PageBase page;


    public RelationPanel(IModel<RequestAccess> model, PageBase page) {
        super(model);

        this.page = page;

        initLayout();
    }

    @Override
    public String getStepId() {
        return STEP_ID;
    }

    private boolean canSkipStep() {
        List<Tile<QName>> list = getPanel().getRelations();
        if (list.size() != 1) {
            return false;
        }

        Tile<QName> tile = list.get(0);
        return tile.isSelected();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        getPanel().resetModel();
    }

    @Override
    protected void onBeforeRender() {
        if (getModelObject().getPersonOfInterest().size() == 0) {
            PageParameters params = new PageParameters();
            params.set(WizardModel.PARAM_STEP, PersonOfInterestPanel.STEP_ID);

            throw new RestartResponseException(new PageRequestAccess(params, getWizard()));
        }

        super.onBeforeRender();
    }

    @Override
    public void init(WizardModel wizard) {
        super.init(wizard);

        if (canSkipStep()) {
            // no user input needed, we'll populate model with data
            submitData();
        }
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> !canSkipStep();
    }

    private ChooseRelationPanel getPanel() {
        return (ChooseRelationPanel) get(ID_PANEL);
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("RelationPanel.title");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("RelationPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("RelationPanel.subtext");
    }

    private void initLayout() {
        ChooseRelationPanel panel = new ChooseRelationPanel(
                ID_PANEL,
                () -> {
                    RequestAccess ra = getModelObject();
                    return ra.getAvailableRelations(page);
                }) {
            @Override
            protected void onTileClick(AjaxRequestTarget target) {
                target.add(getWizard().getPanel());
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new EnableBehaviour(() -> getPanel().isSelectedRelation());
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        submitData();

        getWizard().next();
        target.add(getWizard().getPanel());

        return false;
    }

    private void submitData() {
        QName selected = getPanel().getSelectedRelation();
        if (selected == null) {
            return;
        }

        getModelObject().setRelation(selected);
    }
}
