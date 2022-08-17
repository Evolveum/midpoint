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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardStepPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
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

    private static final String DEFAULT_RELATION_ICON = "fa-solid fa-user";

    private static final String ID_LIST_CONTAINER = "listContainer";
    private static final String ID_LIST = "list";
    private static final String ID_TILE = "tile";

    private PageBase page;

    private LoadableModel<List<Tile<QName>>> relations;

    public RelationPanel(IModel<RequestAccess> model, PageBase page) {
        super(model);

        this.page = page;

        initModels();
        initLayout();
    }

    @Override
    public String getStepId() {
        return STEP_ID;
    }

    private boolean canSkipStep() {
        List<Tile<QName>> list = relations.getObject();
        if (list.size() != 1) {
            return false;
        }

        Tile<QName> tile = list.get(0);
        return tile.isSelected();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        relations.reset();
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

    private void initModels() {
        relations = new LoadableModel<>(false) {

            @Override
            protected List<Tile<QName>> load() {
                RequestAccess ra = getModelObject();

                QName currentRelation = ra.getRelation();
                if (currentRelation == null) {
                    currentRelation = ra.getDefaultRelation();
                    getModelObject().setRelation(currentRelation);
                }

                List<Tile<QName>> tiles = new ArrayList<>();

                List<QName> availableRelations = ra.getAvailableRelations(page);
                for (QName name : availableRelations) {
                    Tile<QName> tile = createTileForRelation(name);
                    tile.setSelected(name.equals(currentRelation));
                    tile.setValue(name);

                    tiles.add(tile);
                }

                return tiles;
            }
        };
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
        WebMarkupContainer listContainer = new WebMarkupContainer(ID_LIST_CONTAINER);
        listContainer.setOutputMarkupId(true);
        add(listContainer);

        ListView<Tile<QName>> list = new ListView<>(ID_LIST, relations) {

            @Override
            protected void populateItem(ListItem<Tile<QName>> item) {
                TilePanel tp = new TilePanel(ID_TILE, item.getModel()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        Tile<QName> tile = item.getModelObject();
                        boolean wasSelected = tile.isSelected();

                        relations.getObject().forEach(t -> t.setSelected(false));
                        tile.setSelected(!wasSelected);

                        target.add(getWizard().getPanel());
                    }
                };
                item.add(tp);
            }
        };
        listContainer.add(list);
    }

    private String getDefaultRelationIcon(QName name) {
        if (SchemaConstants.ORG_DEFAULT.equals(name)) {
            return "fa-solid fa-user";
        } else if (SchemaConstants.ORG_MANAGER.equals(name)) {
            return "fa-solid fa-user-tie";
        } else if (SchemaConstants.ORG_APPROVER.equals(name)) {
            return "fa-solid fa-clipboard-check";
        } else if (SchemaConstants.ORG_OWNER.equals(name)) {
            return "fa-solid fa-crown";
        }

        return DEFAULT_RELATION_ICON;
    }

    private Tile<QName> createTileForRelation(QName name) {
        List<RelationDefinitionType> relations = getSystemRelations();

        String icon = getDefaultRelationIcon(name);
        String label = name.getLocalPart();

        if (relations == null) {
            return createTile(icon, label, name);
        }

        for (RelationDefinitionType rel : relations) {
            if (!name.equals(rel.getRef())) {
                continue;
            }

            DisplayType display = rel.getDisplay();
            if (display == null) {
                break;
            }

            IconType it = display.getIcon();
            if (it != null && it.getCssClass() != null) {
                icon = it.getCssClass();
            }

            label = WebComponentUtil.getTranslatedPolyString(display.getLabel());

            break;
        }

        return createTile(icon, label, name);
    }

    private Tile<QName> createTile(String icon, String label, QName value) {
        Tile tile = new Tile(icon, label);
        tile.setValue(value);

        return tile;
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new EnableBehaviour(() -> relations.getObject().stream().filter(t -> t.isSelected()).count() > 0);
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        submitData();

        getWizard().next();
        target.add(getWizard().getPanel());

        return false;
    }

    private void submitData() {
        Tile<QName> selected = relations.getObject().stream().filter(t -> t.isSelected()).findFirst().orElse(null);

        getModelObject().setRelation(selected.getValue());
    }

    private List<RelationDefinitionType> getSystemRelations() {
        SystemConfigurationType sys = MidPointApplication.get().getSystemConfigurationIfAvailable();
        if (sys == null) {
            return null;
        }

        RoleManagementConfigurationType roleManagement = sys.getRoleManagement();
        if (roleManagement == null) {
            return null;
        }

        RelationsDefinitionType relations = roleManagement.getRelations();
        return relations != null ? relations.getRelation() : null;
    }
}
