/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.DisplayableValueImpl;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/objects",
                        matchUrlForSecurity = "/admin/simulations/result/?*/objects"),
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/tag/${TAG_OID}/objects",
                        matchUrlForSecurity = "/admin/simulations/result/?*/tag/?*/objects")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATIONS_ALL_URL,
                        label = "PageSimulationResults.auth.simulationsAll.label",
                        description = "PageSimulationResults.auth.simulationsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATION_PROCESSED_OBJECTS_URL,
                        label = "PageSimulationResultObjects.auth.simulationProcessedObjects.label",
                        description = "PageSimulationResultObjects.auth.simulationProcessedObjects.description")
        }
)
public class PageSimulationResultObjects extends PageAdmin implements SimulationPage {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = SimulationPage.class.getName() + ".";

    private static final String OPERATION_LOAD_RESULT = DOT_CLASS + "loadResult";
    private static final String OPERATION_LOAD_TAG = DOT_CLASS + "loadTag";

    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_TABLE = "table";

    private IModel<SimulationResultType> resultModel;

    private IModel<Search<SimulationResultProcessedObjectType>> searchModel;

    public PageSimulationResultObjects() {
        this(new PageParameters());
    }

    public PageSimulationResultObjects(PageParameters parameters) {
        super(parameters);

        initModels();
        initLayout();
    }

    private void initModels() {
        resultModel = new LoadableDetachableModel<>() {

            @Override
            protected SimulationResultType load() {
                return loadSimulationResult(PageSimulationResultObjects.this);
            }
        };

//        searchModel = new LoadableDetachableModel<>() {
//            @Override
//            protected Search<SimulationResultProcessedObjectType> load() {
//                GenericPageStorage storage = getSessionStorage().getSimulation();
//                Search search = storage.getSearch();
//
//                if (search == null || search.isForceReload()) {
//                    SearchContext searchCtx = new SearchContext();
//                    searchCtx.setPanelType(CollectionPanelType.SIMULATION_PROCESSED_OBJECTS);
//
//                    SearchBuilder searchBuilder = new SearchBuilder(SimulationResultProcessedObjectType.class)
//                            .additionalSearchContext(searchCtx)
//                            .modelServiceLocator(PageSimulationResultObjects.this);
//                    search = searchBuilder.build();
//
//                    AvailableTagSearchItemWrapper eventTagRefItem = (AvailableTagSearchItemWrapper)
//                            search.findPropertySearchItem(SimulationResultProcessedObjectType.F_EVENT_TAG_REF);
//                    if (eventTagRefItem != null) {
//                        List<PrismObject<TagType>> tags = WebModelServiceUtils.searchObjects(
//                                TagType.class, null, getPageTask().getResult(), PageSimulationResultObjects.this);
//
//                        List<DisplayableValue<String>> values = tags.stream()
//                                .map(o -> new DisplayableValueImpl<>(
//                                        o.getOid(),
//                                        WebComponentUtil.getDisplayNameOrName(o),
//                                        o.asObjectable().getDescription()))
//                                .collect(Collectors.toList());
//                        eventTagRefItem.getAvailableValues().addAll(values);
//                    }
//
//                    storage.setSearch(search);
//                }
//                return search;
//            }
//        };
    }

    private void initLayout() {
        NavigationPanel navigation = new NavigationPanel(ID_NAVIGATION) {

            @Override
            protected @NotNull VisibleEnableBehaviour getNextVisibilityBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected IModel<String> createTitleModel() {
                return () -> WebComponentUtil.getDisplayNameOrName(resultModel.getObject().asPrismObject());
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                PageSimulationResultObjects.this.onBackPerformed(target);
            }
        };
        add(navigation);

        ProcessedObjectsPanel table = new ProcessedObjectsPanel(ID_TABLE) {

            @Override
            protected @NotNull String getSimulationResultOid() {
                String oid = getPageParameterResultOid();
                if (!Utils.isPrismObjectOidValid(oid)) {
                    throw new RestartResponseException(PageError404.class);
                }

                return oid;
            }

            @Override
            protected String getTagOid() {
                String oid = getPageParameterTagOid();
                if (oid != null && !Utils.isPrismObjectOidValid(oid)) {
                    throw new RestartResponseException(PageError404.class);
                }

                return oid;
            }

            @Override
            protected Search createSearch() {
                Search search = super.createSearch();

                AvailableTagSearchItemWrapper eventTagRefItem = (AvailableTagSearchItemWrapper)
                        search.findPropertySearchItem(SimulationResultProcessedObjectType.F_EVENT_TAG_REF);
                if (eventTagRefItem != null) {
                    List<String> tagOids = resultModel.getObject().getMetric().stream()
                            .map(m -> m.getRef() != null ? m.getRef().getEventTagRef() : null)
                            .filter(ref -> ref != null)
                            .map(ref -> ref.getOid())
                            .filter(oid -> Utils.isPrismObjectOidValid(oid))
                            .distinct().collect(Collectors.toList());

                    ObjectQuery query = getPrismContext().queryFor(TagType.class).id(tagOids.toArray(new String[tagOids.size()])).build();

                    List<PrismObject<TagType>> tags = WebModelServiceUtils.searchObjects(
                            TagType.class, query, getPageTask().getResult(), PageSimulationResultObjects.this);

                    List<DisplayableValue<String>> values = tags.stream()
                            .map(o -> new DisplayableValueImpl<>(
                                    o.getOid(),
                                    WebComponentUtil.getDisplayNameOrName(o),
                                    o.asObjectable().getDescription()))
                            .sorted(Comparator.comparing(d -> d.getLabel(), Comparator.naturalOrder()))
                            .collect(Collectors.toList());
                    eventTagRefItem.getAvailableValues().addAll(values);
                }

                return search;
            }
        };
        add(table);
    }

    private void onBackPerformed(AjaxRequestTarget target) {
        redirectBack();
    }
}
