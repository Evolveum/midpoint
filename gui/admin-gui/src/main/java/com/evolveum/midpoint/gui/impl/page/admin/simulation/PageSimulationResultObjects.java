/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.search.Search;

import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugList;
import com.evolveum.midpoint.web.session.GenericPageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;

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
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenu;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

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

        searchModel = new LoadableDetachableModel<>() {
            @Override
            protected Search<SimulationResultProcessedObjectType> load() {
                GenericPageStorage storage = getSessionStorage().getConfiguration();
                Search search = storage.getSearch();

//                if (search == null || search.isForceReload()) {
//                    Class<? extends ObjectType> type = getType();
//
//                    SearchBuilder searchBuilder = new SearchBuilder(type)
//                            .additionalSearchContext(createAdditionalSearchContext())
//                            .modelServiceLocator(PageDebugList.this);
//
//                    search = createSearch(type);
//
//
//                    storage.setSearch(search);
//                }
                return search;
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
        };
        add(table);
    }

    private void onBackPerformed(AjaxRequestTarget target) {
        //todo implement
        setResponsePage(PageSimulationResults.class);
    }
}
