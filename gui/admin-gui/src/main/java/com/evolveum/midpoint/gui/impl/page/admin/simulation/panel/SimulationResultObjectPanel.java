/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResultObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
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
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.*;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.data.CountToolbar;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkWithBadgesColumn;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.prism.show.ChangesPanel;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

public abstract class SimulationResultObjectPanel extends BasePanel<SimulationResultType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_DETAILS = "details";
    private static final String ID_RELATED_OBJECTS = "relatedObjects";
    private static final String ID_CHANGES = "changes";
    private static final String ID_PAGING = "paging";
    private static final String ID_FOOTER = "footer";
    private static final String ID_COUNT = "count";

    private final IModel<SimulationResultProcessedObjectType> objectModel;
    private IModel<ProcessedObject<?>> processedObjectModel;
    private IModel<List<DetailsTableItem>> detailsModel;

    public SimulationResultObjectPanel(String id, IModel<SimulationResultType> model, IModel<SimulationResultProcessedObjectType> objectModel) {
        super(id, model);
        this.objectModel = objectModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    protected IModel<SimulationResultProcessedObjectType> getObjectModel() {
        return objectModel;
    }

    private void initModels() {
        processedObjectModel = new LoadableDetachableModel<>() {

            @Override
            protected ProcessedObject<?> load() {
                SimulationResultProcessedObjectType object = objectModel.getObject();
                if (object == null) {
                    return null;
                }

                return SimulationsGuiUtil.parseProcessedObject(object, getPageBase());
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

                        PrismObject<ResourceType> resourceObject = WebModelServiceUtils.loadObject(resourceRef, getPageBase());
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
                                ObjectReferenceType archetypeRef = object.getStructuralArchetypeRef();
                                if (archetypeRef == null) {
                                    return null;
                                }

                                PrismObject<ArchetypeType> archetype = WebModelServiceUtils.loadObject(archetypeRef, getPageBase());
                                if (archetype == null) {
                                    return WebComponentUtil.getName(archetypeRef);
                                }

                                return WebComponentUtil.getDisplayNameOrName(archetype);
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
                                        .map(ref -> WebModelServiceUtils.resolveReferenceName(ref, getPageBase()))
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
    }

    private void initLayout() {
        CombinedRelatedObjectsProvider provider = getCombinedRelatedObjectsProvider();

        List<IColumn<SelectableBean<SimulationResultProcessedObjectType>, String>> columns = createColumns();

        DataTable<SelectableBean<SimulationResultProcessedObjectType>, String> relatedObjects =
                new SelectableDataTable<>(ID_RELATED_OBJECTS, columns, provider, 10) {

                    @Override
                    protected @NotNull Item<IColumn<SelectableBean<SimulationResultProcessedObjectType>, String>> newCellItem(
                            String id, int index, IModel<IColumn<SelectableBean<SimulationResultProcessedObjectType>, String>> model) {
                        Item<IColumn<SelectableBean<SimulationResultProcessedObjectType>, String>> item = super.newCellItem(id, index, model);
                        if (index == 1) {
                            item.add(AttributeAppender.append("style", "word-break:break-all"));
                        }
                        item.add(AttributeAppender.append("class", "align-middle"));

                        return item;
                    }

                    @Override
                    protected boolean isBreakTextBehaviourEnabled(int index) {
                        if (index == 1) {
                            return false;
                        }
                        return super.isBreakTextBehaviourEnabled(index);
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

            @Contract(pure = true)
            @Override
            protected @Nullable String getPaginationCssClass() {
                return null;
            }
        };
        footer.add(paging);

        DisplayType displayType = new DisplayType()
                .label(createStringResource("PageSimulationResultObject.details").getString())
                .icon(new IconType().cssClass("nav-icon fa-solid fa-flask"));
        DetailsTablePanel details = new DetailsTablePanel(ID_DETAILS,
                Model.of(displayType),
                detailsModel);
        add(details);

        ChangesPanel changesNew = buildChangePanel();
        add(changesNew);
    }

    private @NotNull CombinedRelatedObjectsProvider getCombinedRelatedObjectsProvider() {
        IModel<Search<SimulationResultProcessedObjectType>> searchModel = new LoadableDetachableModel<>() {

            @Override
            protected Search<SimulationResultProcessedObjectType> load() {
                return new SearchBuilder<>(SimulationResultProcessedObjectType.class)
                        .modelServiceLocator(getPageBase())
                        .build();
            }
        };

        return new CombinedRelatedObjectsProvider(SimulationResultObjectPanel.this, searchModel, objectModel) {

            @Override
            protected @NotNull String getSimulationResultOid() {
                return SimulationResultObjectPanel.this.getModelObject().getOid();
            }

            @Override
            protected @NotNull Long getProcessedObjectId() {
                return SimulationResultObjectPanel.this.getObjectModel().getObject().getId();
            }
        };
    }

    private @NotNull ChangesPanel buildChangePanel() {
        IModel<List<VisualizationDto>> visualizations = new LoadableDetachableModel<>() {

            @Override
            protected List<VisualizationDto> load() {
                try {
                    ProcessedObject<?> object = processedObjectModel.getObject();
                    if (object == null || object.getDelta() == null) {
                        return Collections.emptyList();
                    }

                    object.fixEstimatedOldValuesInDelta();

                    Visualization visualization = SimulationsGuiUtil.createVisualization(object.getDelta(), getPageBase());
                    if (visualization == null) {
                        return Collections.emptyList();
                    }

                    return List.of(new VisualizationDto(visualization));
                } catch (Exception ex) {
                    // intentionally empty
                }

                return Collections.emptyList();
            }
        };

        ChangesPanel changesNew = new ChangesPanel(ID_CHANGES, visualizations);
        changesNew.setShowOperationalItems(true);
        return changesNew;
    }

    private List<IColumn<SelectableBean<SimulationResultProcessedObjectType>, String>> createColumns() {
        List<IColumn<SelectableBean<SimulationResultProcessedObjectType>, String>> columns = new ArrayList<>();
        columns.add(SimulationsGuiUtil.createProcessedObjectIconColumn(getPageBase()));
        columns.add(new AjaxLinkWithBadgesColumn<>(createStringResource("ProcessedObjectsPanel.nameColumn")) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
                onRelatedObjectClicked(rowModel.getObject(), target);
            }

            @Override
            protected IModel<String> createLinkModel(IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
                return () -> {
                    SelectableBean<SimulationResultProcessedObjectType> bean = rowModel.getObject();
                    SimulationResultProcessedObjectType object = bean.getValue();
                    if (object == null) {
                        return null;
                    }

                    ProcessedObject<?> obj = SimulationsGuiUtil.parseProcessedObject(object, getPageBase());

                    return SimulationsGuiUtil.getProcessedObjectName(obj, getPageBase());
                };
            }

            @Override
            protected IModel<List<Badge>> createBadgesModel(IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
                return SimulationResultObjectPanel.this.createBadgesModel(rowModel);
            }
        });
        columns.add(new LambdaColumn<>(null, row -> SimulationsGuiUtil.getProcessedObjectType(row::getValue)));

        return columns;
    }

    @Contract(value = "_ -> new", pure = true)
    private @NotNull IModel<List<Badge>> createBadgesModel(IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<Badge> load() {
                SelectableBean<SimulationResultProcessedObjectType> bean = rowModel.getObject();
                SimulationResultProcessedObjectType object = bean.getValue();
                if (object == null || object.getState() == null) {
                    return Collections.emptyList();
                }

                ObjectProcessingStateType state = object.getState();
                if (state != ObjectProcessingStateType.UNMODIFIED) {
                    return Collections.emptyList();
                }

                String style = SimulationsGuiUtil.getObjectProcessingStateBadgeCss(state);

                return List.of(new Badge(style, LocalizationUtil.translateEnum(state)));
            }
        };
    }

    private void onRelatedObjectClicked(@NotNull SelectableBean<SimulationResultProcessedObjectType> bean, AjaxRequestTarget target) {
        SimulationResultProcessedObjectType object = bean.getValue();
        if (object == null) {
            return;
        }

        String markOid = getMarkOid();
        String simulationResultOid = getModelObject().getOid();
        navigateToSimulationResultObject(simulationResultOid, markOid, object, target);
    }

    protected abstract String getMarkOid();

    protected void navigateToSimulationResultObject(
            @NotNull String simulationResultOid,
            @Nullable String markOid,
            @NotNull SimulationResultProcessedObjectType object,
            @NotNull AjaxRequestTarget target) {
        PageParameters params = new PageParameters();
        params.set(PageSimulationResultObject.PAGE_PARAMETER_RESULT_OID, simulationResultOid);
        if (markOid != null) {
            params.set(PageSimulationResultObject.PAGE_PARAMETER_MARK_OID, markOid);
        }
        params.set(PageSimulationResultObject.PAGE_PARAMETER_CONTAINER_ID, object.getId());
        getPageBase().navigateToNext(PageSimulationResultObject.class, params);
    }

}
