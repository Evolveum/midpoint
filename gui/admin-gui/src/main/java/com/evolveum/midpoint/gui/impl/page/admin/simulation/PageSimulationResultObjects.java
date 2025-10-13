/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.SimulationMetricValuesTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectProcessingStateType;
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

    public static final String PAGE_QUERY_PARAMETER = "state";

    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_FORM = "form";
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

    private ObjectProcessingStateType getStateQueryParameter() {
        PageParameters params = getPageParameters();
        String state = params.get(PAGE_QUERY_PARAMETER).toString();
        if (StringUtils.isEmpty(state)) {
            return null;
        }

        try {
            return ObjectProcessingStateType.fromValue(state);
        } catch (Exception ex) {
            return null;
        }
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

    private IModel<List<MarkType>> createNonEmptyMarksModel() {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<MarkType> load() {
                List<MarkType> all = availableMarksModel.getObject();

                Set<String> nonEmptyMarkOids = resultModel.getObject().getMetric().stream()
                        .filter(m -> m.getRef() != null && m.getRef().getEventMarkRef() != null)
                        .filter(m -> !Objects.equals(BigDecimal.ZERO, SimulationMetricValuesTypeUtil.getValue(m)))
                        .map(m -> m.getRef().getEventMarkRef().getOid())
                        .collect(Collectors.toUnmodifiableSet());

                // filter only marks that occur in simulation result (their respective metric count > 0)

                return all.stream()
                        .filter(m -> nonEmptyMarkOids.contains(m.getOid()))
                        .collect(Collectors.toUnmodifiableList());
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
                return PageSimulationResultObjects.this.createTitleModel();
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                PageSimulationResultObjects.this.onBackPerformed();
            }
        };
        add(navigation);

        MidpointForm<?> form = new MidpointForm<>(ID_FORM);
        add(form);

        IModel<List<MarkType>> nonEmptyMarksModel = createNonEmptyMarksModel();

        ProcessedObjectsPanel table = new ProcessedObjectsPanel(ID_TABLE, nonEmptyMarksModel) {

            @Override
            protected ObjectProcessingStateType getPredefinedProcessingState() {
                return getStateQueryParameter();
            }

            @Override
            protected SearchContext createAdditionalSearchContext() {
                SearchContext ctx = super.createAdditionalSearchContext();
                ctx.setObjectProcessingState(getStateQueryParameter());

                return ctx;
            }

            @Override
            protected @NotNull String getSimulationResultOid() {
                String oid = getPageParameterResultOid();
                if (!Utils.isPrismObjectOidValid(oid)) {
                    throw new RestartResponseException(PageError404.class);
                }

                return oid;
            }

            @Override
            protected String getPredefinedMarkOid() {
                String oid = getPageParameterMarkOid();
                if (oid != null && !Utils.isPrismObjectOidValid(oid)) {
                    throw new RestartResponseException(PageError404.class);
                }

                return oid;
            }
        };
        form.add(table);
    }

    private void onBackPerformed() {
        if (canRedirectBack()) {
            redirectBack();
            return;
        }

        clearBreadcrumbs();

        PageParameters params = new PageParameters();
        params.set(SimulationPage.PAGE_PARAMETER_RESULT_OID, resultModel.getObject().getOid());

        navigateToNext(PageSimulationResult.class, params);
    }
}
