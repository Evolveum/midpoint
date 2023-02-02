/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.List;
import java.util.Objects;
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
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/objects",
                        matchUrlForSecurity = "/admin/simulations/result/?*/objects"),
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}/mark/${MARK_OID}/objects",
                        matchUrlForSecurity = "/admin/simulations/result/?*/mark/?*/objects")
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

    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_TABLE = "table";

    private IModel<SimulationResultType> resultModel;

    private IModel<List<MarkType>> availableMarksModel;

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

        availableMarksModel = new LoadableDetachableModel<>() {

            @Override
            protected List<MarkType> load() {
                String[] markOids = resultModel.getObject().getMetric().stream()
                        .map(m -> m.getRef() != null ? m.getRef().getEventMarkRef() : null)
                        .filter(Objects::nonNull)
                        .map(AbstractReferencable::getOid)
                        .filter(Utils::isPrismObjectOidValid)
                        .distinct().toArray(String[]::new);

                ObjectQuery query = getPrismContext()
                        .queryFor(MarkType.class)
                        .id(markOids).build();

                List<PrismObject<MarkType>> marks = WebModelServiceUtils.searchObjects(
                        MarkType.class, query, getPageTask().getResult(), PageSimulationResultObjects.this);

                return marks.stream()
                        .map(o -> o.asObjectable())
                        .collect(Collectors.toUnmodifiableList());
            }
        };
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> null;
    }

    private IModel<String> createTitleModel() {
        return () -> getString("PageSimulationResultObjects.title");
    }

    @Override
    protected void createBreadcrumb() {
        addBreadcrumb(new Breadcrumb(PageSimulationResultObjects.super.createPageTitleModel(), this.getClass(), getPageParameters()));
    }

    private void initLayout() {
        NavigationPanel navigation = new NavigationPanel(ID_NAVIGATION) {

            @Override
            protected @NotNull VisibleEnableBehaviour getNextVisibilityBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected IModel<String> createTitleModel() {
                return PageSimulationResultObjects.this.createTitleModel();
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                PageSimulationResultObjects.this.onBackPerformed();
            }
        };
        add(navigation);

        ProcessedObjectsPanel table = new ProcessedObjectsPanel(ID_TABLE, availableMarksModel) {

            @Override
            protected @NotNull String getSimulationResultOid() {
                String oid = getPageParameterResultOid();
                if (!Utils.isPrismObjectOidValid(oid)) {
                    throw new RestartResponseException(PageError404.class);
                }

                return oid;
            }

            @Override
            protected String getMarkOid() {
                String oid = getPageParameterMarkOid();
                if (oid != null && !Utils.isPrismObjectOidValid(oid)) {
                    throw new RestartResponseException(PageError404.class);
                }

                return oid;
            }
        };
        add(table);
    }

    private void onBackPerformed() {
        redirectBack();
    }
}
