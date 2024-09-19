/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction;

import com.evolveum.midpoint.gui.api.component.wizard.WizardListener;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@PanelType(name = "arw-construction-membership")
@PanelInstance(identifier = "arw-construction-membership",
        applicableForType = AbstractRoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRole.wizard.step.construction.membership", icon = "fa fa-database"),
        containerPath = "empty")
public class ConstructionResourceObjectTypeMembershipStepPanel<AR extends AbstractRoleType>
        extends AbstractWizardStepPanel<FocusDetailsModels<AR>> implements WizardListener {

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionResourceObjectTypeMembershipStepPanel.class);

    public static final String PANEL_TYPE = "arw-construction-membership";

    private static final String ID_TILES_CONTAINER = "tilesContainer";
    private static final String ID_TILES = "tiles";
    private static final String ID_TILE = "tile";

    private String oldOidResource;
    private ShadowKindType oldKind;
    private String oldIntent;

    private final IModel<PrismContainerValueWrapper<ConstructionType>> valueModel;
    private LoadableModel<List<Tile<ResourceObjectTypeWrapper>>> tilesModel;

    public ConstructionResourceObjectTypeMembershipStepPanel(
            FocusDetailsModels<AR> model, IModel<PrismContainerValueWrapper<AssignmentType>> valueModel) {
        super(model);
        this.valueModel = new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerValueWrapper<ConstructionType> load() {
                try {
                    PrismContainerValueWrapper value =
                            valueModel.getObject().findContainer(AssignmentType.F_CONSTRUCTION).getValue();
                    return value;
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't find construction container in assignment");
                }
                return null;
            }
        };
    }

    @Override
    protected void onBeforeRender() {
        if (tilesModel == null || tilesModel.getObject().isEmpty()) {
            getPageBase().info(getPageBase().createStringResource("ConstructionResourceObjectTypeStepPanel.emptyList").getString());
            getFeedback();
        }
        super.onBeforeRender();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initTilesModel();
        initLayout();
    }

    private void initTilesModel() {
        if (tilesModel == null) {
            tilesModel = new LoadableModel<>(false) {
                @Override
                protected List<Tile<ResourceObjectTypeWrapper>> load() {
                    List<Tile<ResourceObjectTypeWrapper>> list = new ArrayList<>();
                    ConstructionType construction = valueModel.getObject().getRealValue();
                    if (construction == null) {
                        return list;
                    }
                    PrismObject<ResourceType> resource =
                            ProvisioningObjectsUtil.getConstructionResource(construction, "load resource", getPageBase());
                    if (resource == null) {
                        return list;
                    }

                    try {
                        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
                        if (schema == null) {
                            return list;
                        }
                        ResourceObjectDefinition resourceObjectDefinition = ProvisioningObjectsUtil.getResourceObjectDefinition(construction, getPageBase());
                        ResourceObjectTypeDefinition actualOc = resourceObjectDefinition instanceof ResourceObjectTypeDefinition
                                ? (ResourceObjectTypeDefinition) resourceObjectDefinition
                                : null;
                        PrismContainerValueWrapper<AssignmentType> actualNewAssignment = getActualNewAssignment();

                        schema.getObjectTypeDefinitions().stream()
                                .filter(objectTypeDef -> actualOc == null
                                        || objectTypeDef.getKind() != actualOc.getKind()
                                        || !StringUtils.equals(objectTypeDef.getIntent(), actualOc.getIntent()))
                                .forEach(oc -> {
                                    String icon = IconAndStylesUtil.createShadowIcon(oc.getKind());

                                    String description = oc.getDescription();

                                    String name;
                                    if (StringUtils.isNotBlank(oc.getDisplayName())) {
                                        name = oc.getDisplayName();
                                    } else if (oc.isDefaultForKind()) {
                                        name = createStringResource(
                                                "ConstructionResourceObjectTypeStepPanel.isDefaultForKindName",
                                                createStringResource(oc.getKind()).getString()).getString();
                                    } else {
                                        name = createStringResource(
                                                "ConstructionResourceObjectTypeStepPanel.kindIntentName",
                                                createStringResource(oc.getKind()).getString(),
                                                oc.getIntent()).getString();
                                    }
                                    TemplateTile<ResourceObjectTypeWrapper> t = new TemplateTile<>(
                                            icon, name, new ResourceObjectTypeWrapper(oc))
                                            .description(description);
                                    t.setSelected(matchResourceObjectTypes(actualNewAssignment, oc));
                                    list.add(t);
                                });
                    } catch (CommonException e) {
                        LOGGER.error("Couldn't create ResourceSchema for resource: " + resource, e);
                    }
                    return list;
                }
            };
        }
    }

    @Override
    public void init(WizardModel wizard) {
        super.init(wizard);
        wizard.addWizardListener(ConstructionResourceObjectTypeMembershipStepPanel.this);
    }

    private boolean matchResourceObjectTypes(PrismContainerValueWrapper<AssignmentType> actualValue, ResourceObjectTypeDefinition oc) {
        if (actualValue == null || actualValue.getRealValue() == null) {
            return false;
        }

        AssignmentType assignment = actualValue.getRealValue();

        return oc.matches(assignment.getConstruction().getKind(), assignment.getConstruction().getIntent());
    }

    private PrismContainerValueWrapper<AssignmentType> getActualNewAssignment() throws CommonException {
        PrismContainerValueWrapper<ConstructionType> construction = valueModel.getObject();
        if (construction == null || construction.getRealValue() == null) {
            return null;
        }

        ResourceObjectDefinition resourceObjectDefinition = ProvisioningObjectsUtil.getResourceObjectDefinition(construction.getRealValue(), getPageBase());
        ResourceObjectTypeDefinition actualOc = resourceObjectDefinition instanceof ResourceObjectTypeDefinition
                ? (ResourceObjectTypeDefinition) resourceObjectDefinition
                : null;

        if (actualOc == null) {
            return null;
        }

        PrismContainerValueWrapper<AssignmentType> assignmentValue = construction.getParentContainerValue(AssignmentType.class);
        if (assignmentValue == null || assignmentValue.getParent() == null) {
            return null;
        }

        Optional<PrismContainerValueWrapper<AssignmentType>> newInducement =
                ((List<PrismContainerValueWrapper<AssignmentType>>) assignmentValue.getParent().getValues()).stream()
                .filter(value -> value.getStatus() == ValueStatus.ADDED)
                .filter(value -> value.getRealValue() != null && value.getRealValue().getConstruction() != null
                        && value.getRealValue().getConstruction().getKind() != actualOc.getKind()
                        && !StringUtils.equals(value.getRealValue().getConstruction().getIntent(), actualOc.getIntent()))
                .findFirst();

        return newInducement.isPresent() ? newInducement.get() : null;
    }

    private void initLayout() {
        WebMarkupContainer tilesContainer = new WebMarkupContainer(ID_TILES_CONTAINER);
        tilesContainer.setOutputMarkupId(true);
        add(tilesContainer);

        ListView<Tile<ResourceObjectTypeWrapper>> list = new ListView<>(ID_TILES, tilesModel) {

            @Override
            protected void populateItem(ListItem<Tile<ResourceObjectTypeWrapper>> item) {
                item.add(createTilePanel(ID_TILE, item.getModel()));
            }
        };
        tilesContainer.add(list);
    }

    protected Component createTilePanel(String id, IModel<Tile<ResourceObjectTypeWrapper>> model) {
        return new TilePanel<>(id, model) {

            @Override
            protected void onInitialize() {
                super.onInitialize();
                get("title").add(AttributeAppender.replace("class", "mt-4 text-secondary text-center"));
                add(AttributeAppender.replace(
                        "class", () -> {
                            String active = "";
                            if (getModelObject().isSelected()) {
                                active = "active ";
                            }
                            return active +
                                    "catalog-tile-panel card mb-0 simple-tile selectable tile-panel "
                                    + "d-flex flex-column justify-content-center align-items-center rounded p-3";
                        }));
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                boolean oldState = getModelObject().isSelected();
                tilesModel.getObject().forEach(tile -> tile.setSelected(false));

                getModelObject().setSelected(!oldState);

                target.add(ConstructionResourceObjectTypeMembershipStepPanel.this.get(ID_TILES_CONTAINER));

                target.add(getNext());
            }

            @Override
            protected VisibleEnableBehaviour getDescriptionBehaviour() {
                return new VisibleBehaviour(() -> StringUtils.isNotBlank(getModelObject().getDescription()));
            }
        };
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    protected String getIcon() {
        return "fa fa-database";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRole.wizard.step.construction.membership");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRole.wizard.step.construction.membership.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.construction.membership.subText");
    }

    @Override
    public String appendCssToWizard() {
        return "mt-5 mx-auto col-10 col-sm-12";
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        performSelectedObjects();
        return super.onNextPerformed(target);
    }

    @Override
    public boolean onBackPerformed(AjaxRequestTarget target) {
        PrismContainerWrapper<AssignmentType> assignmentContainer =
                (PrismContainerWrapper<AssignmentType>) valueModel.getObject().getParent().getParent().getParent();

        try {
            PrismContainerValueWrapper<AssignmentType> oldInducement = getActualNewAssignment();
            if (oldInducement != null) {
                assignmentContainer.remove(oldInducement, getPageBase());
            }
        } catch (CommonException e) {
            LOGGER.error("Couldn't remove old inducement for membership");
        }

        return super.onBackPerformed(target);
    }

    private void performSelectedObjects() {
        Optional<Tile<ResourceObjectTypeWrapper>> selectedTile =
                tilesModel.getObject().stream().filter(tile -> tile.isSelected()).findFirst();

        if (selectedTile.isEmpty()) {
            return;
        }

        PrismContainerWrapper<AssignmentType> assignmentContainer =
                (PrismContainerWrapper<AssignmentType>) valueModel.getObject().getParent().getParent().getParent();

        try {
            PrismContainerValueWrapper<AssignmentType> oldInducement = getActualNewAssignment();
            if (oldInducement != null) {
                assignmentContainer.remove(oldInducement, getPageBase());
            }

            PrismObject<ResourceType> resource =
                    ProvisioningObjectsUtil.getConstructionResource(valueModel.getObject().getRealValue(), "load resource", getPageBase());

            PrismContainerValue<AssignmentType> newAssignment = new AssignmentType().asPrismContainerValue();
            newAssignment.asContainerable()
                    .construction(new ConstructionType()
                            .resourceRef(resource.getOid(), ResourceType.COMPLEX_TYPE)
                            .kind(selectedTile.get().getValue().kind)
                            .intent(selectedTile.get().getValue().intent))
                    .order(2)
                    .focusType(selectedTile.get().getValue().focusTypeName);

            PrismValueWrapper<AssignmentType> newWrapper =
                    WebPrismUtil.createNewValueWrapper(assignmentContainer, newAssignment, getPageBase());

            assignmentContainer.getValues().add((PrismContainerValueWrapper<AssignmentType>) newWrapper);

        } catch (CommonException e) {
            LOGGER.error("Couldn't create inducement for membership");
        }
    }

    class ResourceObjectTypeWrapper implements Serializable {

        private final ShadowKindType kind;
        private final String intent;
        private final QName focusTypeName;

        private ResourceObjectTypeWrapper(ResourceObjectTypeDefinition oc) {
            this.kind = oc.getKind();
            this.intent = oc.getIntent();
            this.focusTypeName = oc.getFocusTypeName();
        }
    }

    @Override
    public void onStepChanged(WizardStep newStep) {
        if (!ConstructionResourceObjectTypeMembershipStepPanel.this.equals(newStep)) {
            return;
        }

        ConstructionType construction = valueModel.getObject().getRealValue();
        if (construction == null) {
            return;
        }

        ObjectReferenceType resourceRef = construction.getResourceRef();
        String resourceOid = resourceRef != null ? resourceRef.getOid() : null;
        if (StringUtils.isNotEmpty(resourceOid)) {
            if (!resourceOid.equals(oldOidResource)) {
                tilesModel.reset();
            }
            oldOidResource = resourceOid;
        }

        ShadowKindType kind = construction.getKind();
        if (kind != null) {
            if (oldKind != kind) {
                tilesModel.reset();
            }
            oldKind = kind;
        }

        String intent = construction.getIntent();
        if (StringUtils.isNotEmpty(intent)) {
            if (!StringUtils.equals(oldIntent, intent)) {
                tilesModel.reset();
            }
            oldIntent = intent;
        }
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }
}
