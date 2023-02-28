package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.PrismObject;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@PanelType(name = "roleWizard-construction-objectType")
@PanelInstance(identifier = "roleWizard-construction-objectType",
        applicableForType = AbstractRoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRole.wizard.step.construction.objectType", icon = "fa fa-database"),
        containerPath = "empty")
public class ConstructionResourceObjectTypeStepPanel<AR extends AbstractRoleType> extends AbstractWizardStepPanel<FocusDetailsModels<AR>> {

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionResourceObjectTypeStepPanel.class);

    public static final String PANEL_TYPE = "roleWizard-construction-objectType";

    private static final String ID_TILES_CONTAINER = "tilesContainer";
    private static final String ID_TILES = "tiles";
    private static final String ID_TILE = "tile";

    private final IModel<PrismContainerValueWrapper<ConstructionType>> valueModel;
    private IModel<List<Tile<ResourceObjectTypeWrapper>>> tilesModel;

    public ConstructionResourceObjectTypeStepPanel(
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
                            WebComponentUtil.getConstructionResource(construction, "load resource", getPageBase());
                    if (resource == null) {
                        return list;
                    }

                    try {
                        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
                        ResourceObjectTypeDefinition actualOc =
                                (ResourceObjectTypeDefinition) WebComponentUtil.getResourceObjectDefinition(construction, getPageBase());
                        schema.getObjectTypeDefinitions().forEach(oc -> {
                            String icon = WebComponentUtil.createShadowIcon(oc.getKind());

                            String description = oc.getDescription();

                            String name;
                            if (oc.isDefaultForKind()) {
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
                            t.setSelected(matchResourceObjectTypes(actualOc, oc));
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

    private boolean matchResourceObjectTypes(ResourceObjectTypeDefinition actualOc, ResourceObjectTypeDefinition oc) {
        if (actualOc == null) {
            return false;
        }

        if (actualOc.isDefaultForKind()) {
            return oc.isDefaultForKind() && actualOc.matchesKind(oc.getKind());
        }
        return actualOc.matches(oc.getKind(), oc.getIntent());
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
                                    + "d-flex flex-column align-items-center rounded p-3";
                        }));
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                boolean oldState = getModelObject().isSelected();
                tilesModel.getObject().forEach(tile -> tile.setSelected(false));

                getModelObject().setSelected(!oldState);

                target.add(ConstructionResourceObjectTypeStepPanel.this.get(ID_TILES_CONTAINER));
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
        return createStringResource("PageRole.wizard.step.construction.objectType");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRole.wizard.step.construction.objectType.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.construction.objectType.subText");
    }

    @Override
    public String appendCssToWizard() {
        return "mt-5 mx-auto col-10 col-sm-12";
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        if (isValid(target)) {
            performSelectedObjects();
            return super.onNextPerformed(target);
        }
        return false;
    }

    private void performSelectedObjects() {
        Optional<Tile<ResourceObjectTypeWrapper>> selectedTile =
                tilesModel.getObject().stream().filter(tile -> tile.isSelected()).findFirst();
        try {
            PrismPropertyWrapper<ShadowKindType> kind = valueModel.getObject().findProperty(ConstructionType.F_KIND);
            kind.getValue().setRealValue(selectedTile.get().getValue().kind);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find kind property in construction value");
        }

        try {
            PrismPropertyWrapper<String> kind = valueModel.getObject().findProperty(ConstructionType.F_INTENT);
            kind.getValue().setRealValue(selectedTile.get().getValue().intent);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find kind property in construction value");
        }
    }

    private boolean isValid(AjaxRequestTarget target) {
        if (isNotSelected()) {
            String key = "ConstructionResourceObjectTypeStepPanel.isMandatory";
            new Toast()
                    .error()
                    .title(PageBase.createStringResourceStatic(key).getString())
                    .icon("fas fa-circle-exclamation")
                    .autohide(true)
                    .delay(5_000)
                    .body(PageBase.createStringResourceStatic(key + ".text").getString())
                    .show(target);
            return false;
        }
        return true;
    }

    private boolean isNotSelected() {
        Optional<Tile<ResourceObjectTypeWrapper>> selectedTile =
                tilesModel.getObject().stream().filter(tile -> tile.isSelected()).findFirst();
        return selectedTile.isEmpty();
    }

    class ResourceObjectTypeWrapper implements Serializable {

        private final ShadowKindType kind;
        private final String intent;

        private ResourceObjectTypeWrapper(ResourceObjectTypeDefinition oc) {
            this.kind = oc.getKind();
            this.intent = oc.getIntent();
        }
    }
}