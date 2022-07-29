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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
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
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RelationPanel extends BasicWizardStepPanel<RequestAccess> implements AccessRequestStep {

    private static final long serialVersionUID = 1L;

    public static final String STEP_ID = "relation";

    private static final String DOT_CLASS = RelationPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_ASSIGNABLE_RELATIONS_LIST = DOT_CLASS + "loadAssignableRelationsList";

    private static final String DEFAULT_RELATION_ICON = "fa-solid fa-user";

    private static final String ID_LIST_CONTAINER = "listContainer";
    private static final String ID_LIST = "list";
    private static final String ID_TILE = "tile";

    private PageBase page;

    private IModel<List<Tile<QName>>> relations;

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
    public void init(WizardModel wizard) {
        super.init(wizard);

        if (canSkipStep()) {
            // no user input needed, we'll populate model with data
            submitData();

            wizard.next();
        }
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> !canSkipStep();
    }

    @Override
    protected void onBeforeRender() {
        // todo doesn't work properly, header stays hidden on next step
//        if (canSkipStep()) {
//            // there's only one relation, we don't have to make user choose it, we'll take it and skip this step
//            submitData();
//            getWizard().next();
//
//            throw new RestartResponseException(getPage());
//        }

        super.onBeforeRender();
    }

    private void initModels() {
        relations = new LoadableModel<>(false) {

            @Override
            protected List<Tile<QName>> load() {
                RelationSelectionType config = getRelationConfiguration();
                QName defaultRelation = null;
                if (config != null) {
                    defaultRelation = config.getDefaultRelation();
                }

                if (defaultRelation == null) {
                    defaultRelation = SchemaConstants.ORG_DEFAULT;
                }

                getModelObject().setRelation(defaultRelation);

                List<Tile<QName>> tiles = new ArrayList<>();

                List<QName> list = getAvailableRelationsList();
                for (QName name : list) {
                    Tile<QName> tile = createTileForRelation(name);
                    tile.setSelected(name.equals(defaultRelation));
                    tile.setValue(name);

                    if (BooleanUtils.isFalse(config.isAllowOtherRelations()) && !tile.isSelected()) {
                        // skip non default tiles as other relations are not allowed
                        continue;
                    }

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

    private Tile<QName> createTileForRelation(QName name) {
        RelationSelectionType config = getRelationConfiguration();

        String icon = DEFAULT_RELATION_ICON;
        String label = name.getLocalPart();

        for (RelationDefinitionType rel : config.getRelation()) {
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

        Tile tile = new Tile(icon, label);
        tile.setValue(name);

        return tile;
    }

    private List<QName> getAvailableRelationsList() {
        List<ObjectReferenceType> personsOfInterest = getModelObject().getPersonOfInterest();
        if (personsOfInterest.isEmpty()) {
            return new ArrayList<>();
        }

        ObjectReferenceType ref = personsOfInterest.get(0);

        FocusType focus;
        try {
            PrismObject<UserType> prismFocus = WebModelServiceUtils.loadObject(ref, page);
            focus = prismFocus.asObjectable();
        } catch (Exception ex) {
            page.error(getString("RelationPanel.loadRelationsError", ref.getTargetName(), ref.getOid()));
            return new ArrayList<>();
        }

        Task task = page.createSimpleTask(OPERATION_LOAD_ASSIGNABLE_RELATIONS_LIST);
        OperationResult result = task.getResult();
        List<QName> assignableRelationsList = WebComponentUtil.getAssignableRelationsList(
                focus.asPrismObject(), RoleType.class, WebComponentUtil.AssignmentOrder.ASSIGNMENT, result, task, page);

        if (CollectionUtils.isEmpty(assignableRelationsList)) {
            return WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.SELF_SERVICE, page);
        }

        return assignableRelationsList;
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

    private RelationSelectionType getRelationConfiguration() {
        AccessRequestType accessRequest = getAccessRequestConfiguration(page);
        if (accessRequest == null) {
            return null;
        }

        return accessRequest.getRelationSelection();
    }
}
