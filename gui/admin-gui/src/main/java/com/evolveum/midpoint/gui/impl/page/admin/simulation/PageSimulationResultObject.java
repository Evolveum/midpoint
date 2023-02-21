/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.data.CountToolbar;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.prism.show.ChangesPanel;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.prism.show.VisualizationPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/object/${CONTAINER_ID}",
                        matchUrlForSecurity = "/admin/simulations/result/?*/object/?*"),
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/mark/${MARK_OID}/object/${CONTAINER_ID}",
                        matchUrlForSecurity = "/admin/simulations/result/?*/mark/?*/object/?*")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATIONS_ALL_URL,
                        label = "PageSimulationResults.auth.simulationsAll.label",
                        description = "PageSimulationResults.auth.simulationsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATION_PROCESSED_OBJECT_URL,
                        label = "PageSimulationResultObject.auth.simulationProcessedObject.label",
                        description = "PageSimulationResultObject.auth.simulationProcessedObject.description")
        }
)
public class PageSimulationResultObject extends PageAdmin implements SimulationPage {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSimulationResultObject.class);

    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_DETAILS = "details";
    private static final String ID_RELATED_OBJECTS = "relatedObjects";
    private static final String ID_CHANGES_NEW = "changesNew";
    private static final String ID_CHANGES = "changes";
    private static final String ID_PAGING = "paging";
    private static final String ID_FOOTER = "footer";
    private static final String ID_COUNT = "count";

    private IModel<SimulationResultType> resultModel;

    private IModel<SimulationResultProcessedObjectType> objectModel;

    private IModel<List<DetailsTableItem>> detailsModel;

    private IModel<VisualizationDto> changesModel;

    public PageSimulationResultObject() {
        this(new PageParameters());
    }

    public PageSimulationResultObject(PageParameters parameters) {
        super(parameters);

        initModels();
        initLayout();
    }

    private void initModels() {
        resultModel = new LoadableDetachableModel<>() {

            @Override
            protected SimulationResultType load() {
                return loadSimulationResult(PageSimulationResultObject.this);
            }
        };

        objectModel = new LoadableDetachableModel<>() {

            @Override
            protected SimulationResultProcessedObjectType load() {
                Task task = getPageTask();

                Long id = getPageParameterContainerId();

                if (id == null) {
                    throw new RestartResponseException(PageError404.class);
                }

                ObjectQuery query = getPrismContext().queryFor(SimulationResultProcessedObjectType.class)
                        .ownedBy(SimulationResultType.class, SimulationResultType.F_PROCESSED_OBJECT)
                        .ownerId(resultModel.getObject().getOid())
                        .and()
                        .id(id)
                        .build();

                List<SimulationResultProcessedObjectType> result = WebModelServiceUtils.searchContainers(SimulationResultProcessedObjectType.class,
                        query, null, task.getResult(), PageSimulationResultObject.this);

                if (result.isEmpty()) {
                    throw new RestartResponseException(PageError404.class);
                }

                return result.get(0);
            }
        };

        detailsModel = new LoadableDetachableModel<>() {

            @Override
            protected List<DetailsTableItem> load() {
                List<DetailsTableItem> items = new ArrayList<>();

                items.add(new DetailsTableItem(createStringResource("PageSimulationResultObject.type"), () -> SimulationsGuiUtil.getProcessedObjectType(objectModel)));

                IModel<String> resourceCoordinatesModel = new LoadableDetachableModel<>() {

                    @Override
                    protected String load() {
                        SimulationResultProcessedObjectType object = objectModel.getObject();
                        ShadowDiscriminatorType discriminator = object.getResourceObjectCoordinates();
                        if (discriminator == null) {
                            return null;
                        }

                        ObjectReferenceType resourceRef = discriminator.getResourceRef();
                        if (resourceRef == null) {
                            return null;
                        }

                        PrismObject<ResourceType> resourceObject = WebModelServiceUtils.loadObject(resourceRef, PageSimulationResultObject.this);
                        if (resourceObject == null) {
                            return null;
                        }

                        ResourceType resource = resourceObject.asObjectable();
                        SchemaHandlingType handling = resource.getSchemaHandling();
                        if (handling == null) {
                            return null;
                        }

                        ResourceObjectTypeDefinitionType found = null;
                        for (ResourceObjectTypeDefinitionType objectType : handling.getObjectType()) {
                            if (Objects.equals(objectType.getKind(), discriminator.getKind()) && Objects.equals(objectType.getIntent(), discriminator.getIntent())) {
                                found = objectType;
                                break;
                            }
                        }

                        if (found == null) {
                            return null;
                        }

                        String displayName = found.getDisplayName();
                        if (displayName == null) {
                            displayName = getString("PageSimulationResultObject.unknownResourceObject");
                        }

                        return getString("PageSimulationResultObject.resourceCoordinatesValue", displayName, WebComponentUtil.getName(resource));
                    }
                };
                items.add(new DetailsTableItem(createStringResource("PageSimulationResultObject.resourceCoordinates"), resourceCoordinatesModel) {

                    @Override
                    public VisibleBehaviour isVisible() {
                        return new VisibleBehaviour(() -> StringUtils.isNotEmpty(resourceCoordinatesModel.getObject()));
                    }
                });

                items.add(new DetailsTableItem(createStringResource("PageSimulationResultObject.state"), null) {

                    @Override
                    public Component createValueComponent(String id) {
                        return SimulationsGuiUtil.createProcessedObjectStateLabel(id, objectModel);
                    }
                });

                items.add(new DetailsTableItem(createStringResource("PageSimulationResultObject.structuralArchetype"),
                        new LoadableDetachableModel<>() {
                            @Override
                            protected String load() {
                                SimulationResultProcessedObjectType object = objectModel.getObject();
                                if (object.getStructuralArchetypeRef() == null) {
                                    return null;
                                }

                                return WebModelServiceUtils.resolveReferenceName(object.getStructuralArchetypeRef(), PageSimulationResultObject.this);
                            }
                        }) {

                    @Override
                    public VisibleBehaviour isVisible() {
                        return new VisibleBehaviour(() -> objectModel.getObject().getStructuralArchetypeRef() != null);
                    }
                });

                items.add(new DetailsTableItem(createStringResource("PageSimulationResultObject.marks"), null) {

                    @Override
                    public Component createValueComponent(String id) {
                        IModel<String> model = new LoadableDetachableModel<>() {

                            @Override
                            protected String load() {
                                SimulationResultProcessedObjectType object = objectModel.getObject();

                                Object[] names = object.getEventMarkRef().stream()
                                        .map(ref -> WebModelServiceUtils.resolveReferenceName(ref, PageSimulationResultObject.this))
                                        .filter(Objects::nonNull)
                                        .sorted()
                                        .toArray();

                                return StringUtils.joinWith("\n", names);
                            }
                        };

                        MultiLineLabel label = new MultiLineLabel(id, model);
                        label.setRenderBodyOnly(true);

                        return label;
                    }
                });

                items.add(new DetailsTableItem(createStringResource("PageSimulationResultObject.projectionCount"),
                        () -> Integer.toString(Objects.requireNonNullElse(objectModel.getObject().getProjectionRecords(), 0))));

                return items;
            }
        };

        changesModel = new LoadableDetachableModel<>() {

            @Override
            protected VisualizationDto load() {
                ObjectDeltaType objectDelta = objectModel.getObject().getDelta();
                return SimulationsGuiUtil.createVisualizationDto(objectDelta, PageSimulationResultObject.this);
            }
        };
    }

    private void initLayout() {
        NavigationPanel navigation = new NavigationPanel(ID_NAVIGATION) {

            @Override
            protected @NotNull VisibleEnableBehaviour getNextVisibilityBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected IModel<String> createTitleModel() {
                return PageSimulationResultObject.this.createTitleModel();
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                PageSimulationResultObject.this.onBackPerformed();
            }
        };
        add(navigation);

        IModel<Search<SimulationResultProcessedObjectType>> searchModel = new LoadableDetachableModel<>() {

            @Override
            protected Search<SimulationResultProcessedObjectType> load() {
                return new SearchBuilder<>(SimulationResultProcessedObjectType.class)
                        .modelServiceLocator(PageSimulationResultObject.this)
                        .build();
            }
        };

        CombinedRelatedObjectsProvider provider = new CombinedRelatedObjectsProvider(this, searchModel, objectModel) {

            @Override
            protected @NotNull String getSimulationResultOid() {
                return PageSimulationResultObject.this.getPageParameterResultOid();
            }

            @Override
            protected @NotNull Long getProcessedObjectId() {
                return PageSimulationResultObject.this.getPageParameterContainerId();
            }
        };

        List<IColumn<SelectableBean<SimulationResultProcessedObjectType>, String>> columns = createColumns();

        DataTable<SelectableBean<SimulationResultProcessedObjectType>, String> relatedObjects =
                new SelectableDataTable<>(ID_RELATED_OBJECTS, columns, provider, 10) {

                    @Override
                    protected Item<IColumn<SelectableBean<SimulationResultProcessedObjectType>, String>> newCellItem(String id, int index, IModel<IColumn<SelectableBean<SimulationResultProcessedObjectType>, String>> model) {
                        Item<IColumn<SelectableBean<SimulationResultProcessedObjectType>, String>> item = super.newCellItem(id, index, model);
                        item.add(AttributeAppender.append("class", "align-middle"));

                        return item;
                    }

                };
        relatedObjects.add(new VisibleBehaviour(() -> relatedObjects.getRowCount() > 1));
        add(relatedObjects);

        final WebMarkupContainer footer = new WebMarkupContainer(ID_FOOTER);
        footer.add(new VisibleBehaviour(() -> relatedObjects.getPageCount() > 1));
        add(footer);

        final Label count = new Label(ID_COUNT, () -> CountToolbar.createCountString(relatedObjects));
        footer.add(count);

        final NavigatorPanel paging = new NavigatorPanel(ID_PAGING, relatedObjects, true) {

            @Override
            protected String getPaginationCssClass() {
                return null;
            }
        };
        footer.add(paging);

        DetailsTablePanel details = new DetailsTablePanel(ID_DETAILS,
                () -> "fa-solid fa-circle-question",
                createStringResource("PageSimulationResultObject.details"),
                detailsModel);
        add(details);

        ChangesPanel changesNew = new ChangesPanel(ID_CHANGES_NEW, () -> Arrays.asList(objectModel.getObject().getDelta()), null);
        changesNew.add(new VisibleBehaviour(() -> WebComponentUtil.isEnabledExperimentalFeatures()));
        add(changesNew);

        VisualizationPanel changes = new VisualizationPanel(ID_CHANGES, changesModel);
        changes.add(new VisibleBehaviour(() -> changesModel.getObject() != null && !WebComponentUtil.isEnabledExperimentalFeatures()));
        add(changes);
    }

    private List<IColumn<SelectableBean<SimulationResultProcessedObjectType>, String>> createColumns() {
        List<IColumn<SelectableBean<SimulationResultProcessedObjectType>, String>> columns = new ArrayList<>();
        columns.add(SimulationsGuiUtil.createProcessedObjectIconColumn());
        columns.add(new AjaxLinkColumn<>(createStringResource("ProcessedObjectsPanel.nameColumn")) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
                onRelatedObjectClicked(rowModel.getObject());
            }

            @Override
            protected IModel<String> createLinkModel(IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
                return () -> {
                    SimulationResultProcessedObjectType obj = rowModel.getObject().getValue();
                    if (obj == null || obj.getName() == null) {
                        return getString("ProcessedObjectsPanel.unnamed");
                    }

                    return WebComponentUtil.getTranslatedPolyString(obj.getName());
                };
            }
        });
        columns.add(new LambdaColumn<>(null, row -> SimulationsGuiUtil.getProcessedObjectType(row::getValue)));

        return columns;
    }

    private void onRelatedObjectClicked(SelectableBean<SimulationResultProcessedObjectType> bean) {
        SimulationResultProcessedObjectType object = bean.getValue();
        if (object == null) {
            return;
        }

        PageParameters params = new PageParameters();
        params.set(PageSimulationResultObject.PAGE_PARAMETER_RESULT_OID, getPageParameterResultOid());
        String markOid = getPageParameterMarkOid();
        if (markOid != null) {
            params.set(PageSimulationResultObject.PAGE_PARAMETER_MARK_OID, markOid);
        }
        params.set(PageSimulationResultObject.PAGE_PARAMETER_CONTAINER_ID, object.getId());

        navigateToNext(PageSimulationResultObject.class, params);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> null;
    }

    private void onBackPerformed() {
        redirectBack();
    }

    private IModel<String> createTitleModel() {
        return () ->
                WebComponentUtil.getOrigStringFromPoly(objectModel.getObject().getName())
                        + " (" + WebComponentUtil.getDisplayNameOrName(resultModel.getObject().asPrismObject()) + ")";
    }

    @Override
    protected void createBreadcrumb() {
        addBreadcrumb(new Breadcrumb(createTitleModel(), this.getClass(), getPageParameters()));
    }
}
