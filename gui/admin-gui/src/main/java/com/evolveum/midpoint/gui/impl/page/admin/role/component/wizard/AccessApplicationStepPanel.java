package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.gui.impl.component.tile.FocusTilePanel;
import com.evolveum.midpoint.gui.impl.component.tile.MemberTilePanel;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization.ActionStepPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.IResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@PanelType(name = "roleWizard-access-application")
@PanelInstance(identifier = "roleWizard-access-application",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageRole.wizard.step.access.application", icon = "fa fa-list"),
        containerPath = "empty")
public class AccessApplicationStepPanel extends AbstractWizardStepPanel<RoleType, FocusDetailsModels<RoleType>> {

    public static final String PANEL_TYPE = "rw-synchronization-reaction-action";

    private static final Trace LOGGER = TraceManager.getTrace(ActionStepPanel.class);

    private static final String ID_TABLE = "table";

    private final IModel<PrismContainerValueWrapper<AssignmentType>> applicationModel;

    private IModel<Search<ServiceType>> searchModel;

    public AccessApplicationStepPanel(FocusDetailsModels<RoleType> model, IModel<PrismContainerValueWrapper<AssignmentType>> applicationModel) {
        super(model);
        this.applicationModel = applicationModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initSearchModel();
        initLayout();
    }

    private void initSearchModel() {
        searchModel = new LoadableModel<>(false) {
            @Override
            protected Search<ServiceType> load() {
                return SearchFactory.createSearch(ServiceType.class, getPageBase());
            }
        };
    }

    private void initLayout() {
        SelectableBeanObjectDataProvider<ServiceType> provider = new SelectableBeanObjectDataProvider<>(
                getPageBase(), searchModel, null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return PrismContext.get().queryFor(ServiceType.class)
                        .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(SystemObjectsType.ARCHETYPE_APPLICATIONS.value())
                        .build();
            }

            @Override
            public void detach() {
                preprocessSelectedDataInternal();
                super.detach();
            }
        };
        provider.setCompiledObjectCollectionView(getCompiledCollectionViewFromPanelConfiguration());
        provider.setOptions(getSearchOptions());

        TileTablePanel<TemplateTile<SelectableBean<ServiceType>>, SelectableBean<ServiceType>> tilesTable =
                new TileTablePanel<>(
                        ID_TABLE,
                        provider,
                        Collections.emptyList(),
                        Model.of(ViewToggle.TILE),
                        UserProfileStorage.TableId.PANEL_ACCESS_WIZARD_STEP) {

                    @Override
                    protected TemplateTile<SelectableBean<ServiceType>> createTileObject(SelectableBean<ServiceType> object) {
                        TemplateTile<SelectableBean<ServiceType>> t = TemplateTile.createTileFromObject(object, getPageBase());
                        return t;
                    }

                    @Override
                    protected Component createTile(String id, IModel<TemplateTile<SelectableBean<ServiceType>>> model) {

                        return new FocusTilePanel<>(id, model) {

                            @Override
                            protected void initLayout() {
                                super.initLayout();

                                add(new AjaxEventBehavior("click") {
                                    @Override
                                    protected void onEvent(AjaxRequestTarget target) {
                                        onClick(target);
                                    }
                                });

                                add(AttributeAppender.append("class", "card catalog-tile-panel d-flex flex-column align-items-center bordered p-3 h-100 mb-0"));
                                add(AttributeAppender.append("class", () -> getModelObject().isSelected() ? "active selectable" : null));
                            }

                            @Override
                            protected void onClick(AjaxRequestTarget target) {
                                super.onClick(target);
                                provider.clearSelectedObjects();
                                target.add(getTable());
                                getModelObject().getValue().setSelected(getModelObject().isSelected());
                            }

                            @Override
                            protected Behavior createDetailsBehaviour() {
                                return VisibleBehaviour.ALWAYS_INVISIBLE;
                            }

                            @Override
                            protected IModel<IResource> createPreferredImage(IModel<TemplateTile<SelectableBean<ServiceType>>> model) {
                                return new LoadableModel<>(false) {
                                    @Override
                                    protected IResource load() {
                                        ServiceType object = model.getObject().getValue().getValue();
                                        return WebComponentUtil.createJpegPhotoResource(object);
                                    }
                                };
                            }
                        };
                    }

                    @Override
                    protected String getTileCssClasses() {
                        return "col-xs-4 col-sm-4 col-md-4 col-lg-3 col-xl-2 col-xxl-2 px-4 mb-3";
                    }

                    @Override
                    protected IModel<Search<? extends ObjectType>> createSearchModel() {
                        return (IModel) searchModel;
                    }
                };
        add(tilesTable);
    }

    private Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return getPageBase().getOperationOptionsBuilder()
                .item(FocusType.F_JPEG_PHOTO).retrieve()
                .build();
    }

    private CompiledObjectCollectionView getCompiledCollectionViewFromPanelConfiguration() {
        ContainerPanelConfigurationType panelConfig = getContainerConfiguration(PANEL_TYPE);

        if (panelConfig == null) {
            return null;
        }
        if (panelConfig.getListView() == null) {
            return null;
        }
        CollectionRefSpecificationType collectionRefSpecificationType = panelConfig.getListView().getCollection();

        CompiledObjectCollectionView compiledCollectionViewFromPanelConfiguration = null;
        if (collectionRefSpecificationType == null) {
            compiledCollectionViewFromPanelConfiguration = new CompiledObjectCollectionView();
            getPageBase().getModelInteractionService().applyView(compiledCollectionViewFromPanelConfiguration, panelConfig.getListView());
            return compiledCollectionViewFromPanelConfiguration;
        }
        Task task = getPageBase().createSimpleTask("Compile collection");
        OperationResult result = task.getResult();
        try {
            compiledCollectionViewFromPanelConfiguration = getPageBase().getModelInteractionService().compileObjectCollectionView(
                    collectionRefSpecificationType, ServiceType.class, task, result);
        } catch (Throwable e) {
            LOGGER.error("Cannot compile object collection view for panel configuration {}. Reason: {}", panelConfig, e.getMessage(), e);
            result.recordFatalError("Cannot compile object collection view for panel configuration " + panelConfig + ". Reason: " + e.getMessage(), e);
            getPageBase().showResult(result);
        }
        return compiledCollectionViewFromPanelConfiguration;

    }

    private Component getTable() {
        return get(ID_TABLE);
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    protected String getIcon() {
        return "fa fa-list";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRole.wizard.step.access.application");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRole.wizard.step.access.application.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRole.wizard.step.access.application.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    public String appendCssToWizard() {
        return "mt-5 mx-auto col-11";
    }

}
