/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.repo.sqlbase.NativeOnlySupportedException;

import com.evolveum.midpoint.util.SingleLocalizableMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.box.SmallBox;
import com.evolveum.midpoint.gui.impl.component.box.SmallBoxData;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.widget.MetricWidgetPanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.model.api.util.DashboardUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.SelectorQualifiedGetOptionsUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgs;
import com.evolveum.midpoint.web.page.admin.reports.PageAuditLogViewer;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.shadows.PageShadows;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.Nullable;

/**
 * @author skublik
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/dashboard", matchUrlForSecurity = "/admin/dashboard")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminHome.AUTH_HOME_ALL_URI,
                        label = PageAdminHome.AUTH_HOME_ALL_LABEL,
                        description = PageAdminHome.AUTH_HOME_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
                        label = "PageDashboard.auth.dashboard.label",
                        description = "PageDashboard.auth.dashboard.description")
        })
public class PageDashboardConfigurable extends PageDashboard {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageDashboardConfigurable.class);
    private static final String DOT_CLASS = PageDashboardConfigurable.class.getName() + ".";
    private static final String OPERATION_COMPILE_DASHBOARD_COLLECTION = DOT_CLASS + "compileDashboardCollection";

    private static final Map<String, Class<? extends WebPage>> LINKS_REF_COLLECTIONS;
    private static final String NATIVE_ONLY_SUPPORTED_KEY = "PageDashboardConfigurable.widget.native.only";
    private static final String UNSUPPORTED_KEY = "PageDashboardConfigurable.widget.unsupported";

    static {
        Map<String, Class<? extends WebPage>> map = new HashMap<>();

        map.put(ResourceType.COMPLEX_TYPE.getLocalPart(), PageResources.class);
        map.put(AuditEventRecordType.COMPLEX_TYPE.getLocalPart(), PageAuditLogViewer.class);
        map.put(TaskType.COMPLEX_TYPE.getLocalPart(), PageTasks.class);
        map.put(UserType.COMPLEX_TYPE.getLocalPart(), PageUsers.class);
        map.put(RoleType.COMPLEX_TYPE.getLocalPart(), PageRoles.class);
        map.put(OrgType.COMPLEX_TYPE.getLocalPart(), PageOrgs.class);
        map.put(ServiceType.COMPLEX_TYPE.getLocalPart(), PageServices.class);

        LINKS_REF_COLLECTIONS = map;
    }

    private IModel<DashboardType> dashboardModel;

    private static final String ID_WIDGETS = "widgets";
    private static final String ID_WIDGET = "widget";

    @Override
    protected void onInitialize() {
        if (dashboardModel == null) {
            dashboardModel = initDashboardObject();
        }
        super.onInitialize();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<>(false) {

            @Override
            public String load() {
                DisplayType display = dashboardModel.getObject().getDisplay();
                if (display != null && display.getLabel() != null) {
                    return display.getLabel().getOrig();
                } else {
                    return dashboardModel.getObject().getName().getOrig();
                }
            }
        };
    }

    private IModel<DashboardType> initDashboardObject() {
        return new LoadableModel<>(false) {

            @Override
            public DashboardType load() {
                StringValue dashboardOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
                if (dashboardOid == null || StringUtils.isEmpty(dashboardOid.toString())) {
                    getSession().error(getString("PageDashboardConfigurable.message.oidNotDefined"));
                    throw new RestartResponseException(PageDashboardInfo.class);
                }
                Task task = createSimpleTask("Search dashboard");
                PrismObject<DashboardType> dashboard = WebModelServiceUtils.loadObject(DashboardType.class,
                        dashboardOid.toString(), PageDashboardConfigurable.this, task, task.getResult());
                if (dashboard == null) {
                    return null; //TODO what to do if null?
                }
                return dashboard.getRealValue();
            }
        };
    }

    protected void initLayout() {
        initInfoBoxes();
    }

    private void initInfoBoxes() {
        add(new ListView<DashboardWidgetType>(ID_WIDGETS, new PropertyModel<>(dashboardModel, "widget")) {

            @Override
            protected void populateItem(ListItem<DashboardWidgetType> item) {
                DashboardWidgetType dw = item.getModelObject();
                DashboardWidgetSourceTypeType sourceType = dw.getData() != null ? dw.getData().getSourceType() : null;
                if (DashboardWidgetSourceTypeType.METRIC == sourceType) {
                    item.add(populateMetricWidget(item.getModel()));
                } else {
                    item.add(populateDashboardWidget(item.getModel()));
                }
            }
        });
    }

    private Component populateMetricWidget(IModel<DashboardWidgetType> model) {
        return new MetricWidgetPanel(PageDashboardConfigurable.ID_WIDGET, model);
    }

    private Component populateDashboardWidget(IModel<DashboardWidgetType> model) {
        IModel<DashboardWidgetDto> widgetModel = loadWidgetData(model);

        SmallBox box = new SmallBox(PageDashboardConfigurable.ID_WIDGET, () -> {
            DashboardWidgetDto widget = widgetModel.getObject();

            SmallBoxData data = new SmallBoxData();
            data.setTitle(widget.getNumberLabel());
            data.setDescription(widget.getMessage());
            data.setIcon(widget.getIconCssClass());
            return data;
        }) {

            @Override
            protected boolean isLinkVisible() {
                return existLinkRef(model.getObject());
            }

            @Override
            protected void onClickLink(AjaxRequestTarget target) {
                navigateToPage(model.getObject());
            }
        };
        box.add(new VisibleBehaviour(() -> WebComponentUtil.getElementVisibility(model.getObject().getVisibility())));
        box.add(AttributeAppender.append("style", () -> StringUtils.join(
                widgetModel.getObject().getStyleColor(),
                " ",
                widgetModel.getObject().getStyleCssStyle())));

        return box;
    }

    private IModel<DashboardWidgetDto> loadWidgetData(IModel<DashboardWidgetType> model) {
        return new LoadableModel<>(false) {

            @Override
            protected DashboardWidgetDto load() {
                Task task = createSimpleTask("Get DashboardWidget");
                OperationResult result = task.getResult();
                try {
                    getPrismContext().adopt(model.getObject());

                    DashboardWidget dashboardWidget = getDashboardService().createWidgetData(model.getObject(), true, task, result);
                    result.computeStatusIfUnknown();

                    return new DashboardWidgetDto(dashboardWidget, PageDashboardConfigurable.this);
                } catch (Exception e) {
                    var ret = new DashboardWidgetDto(null, PageDashboardConfigurable.this);
                    var nativeOnlySupport = findNativeOnlyException(e);
                    if (nativeOnlySupport != null) {
                        // Here we can handle special case - that filter is only supported on native repository (and we are using generic)
                        LOGGER.warn("Couldn't get DashboardWidget with widget {}. Uses features supported only native repository.",
                                model.getObject().getIdentifier(), nativeOnlySupport.getMessage());
                        result.recordHandledError(nativeOnlySupport.getLocalizedUserFriendlyMessage(), e);
                        result.setUserFriendlyMessage(new SingleLocalizableMessage(
                                NATIVE_ONLY_SUPPORTED_KEY, new Object[]{model.getObject().getIdentifier()}));


                        return createUnsupportedWidget(model);
                    } else {
                        LOGGER.error("Couldn't get DashboardWidget with widget " + model.getObject().getIdentifier(), e);
                        result.recordFatalError("Couldn't get widget, reason: " + e.getMessage(), e);
                    }
                    result.computeStatusIfUnknown();
                    showResult(result);

                    return ret;
                }
            }
        };
    }

    private DashboardWidgetDto createUnsupportedWidget(IModel<DashboardWidgetType> model) {
        // Let's modify widget to warning
        var widget = model.getObject();
        widget.setData(null);
        var data = getDashboardService().createEmptyWidgetData(widget);
        var display = data.getDisplay();
        display.setColor("var(--warning)");
        display.setCssStyle("color: var(--navy) !important;");
        display.setIcon(new IconType().cssClass("fa fa-exclamation-triangle"));
        var unsupportedShort = getLocalizationService().translate(UNSUPPORTED_KEY, new Object[]{}, getLocale());
        data.setNumberMessage(unsupportedShort);
        var ret = new DashboardWidgetDto(data, PageDashboardConfigurable.this);
        var unsupportedLong = getLocalizationService().translate(NATIVE_ONLY_SUPPORTED_KEY, new Object[]{ret.getMessage()},getLocale());
        ret.setMessage(unsupportedLong);
        return ret;

    }

    @Nullable
    static final NativeOnlySupportedException findNativeOnlyException(Exception e) {
        Throwable next = e;
        while (next != null) {
            if (next instanceof NativeOnlySupportedException nativeOnly) {
                return nativeOnly;
            }
            next = e.getCause();
        }
        return null;
    }

    private boolean isCollectionLoadable(DashboardWidgetType widget) {
        Task task = createSimpleTask(OPERATION_COMPILE_DASHBOARD_COLLECTION);
        OperationResult result = new OperationResult(OPERATION_COMPILE_DASHBOARD_COLLECTION);
        try {
            getModelInteractionService().compileObjectCollectionView(getDashboardService()
                    .getCollectionRefSpecificationType(widget, task, result), null, task, result);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean existLinkRef(DashboardWidgetType widget) {
        if (widget == null) {
            return false;
        }
        DashboardWidgetSourceTypeType source = DashboardUtils.getSourceType(widget);
        if (source == null) {
            return false;
        }
        switch (source) {
            case OBJECT_COLLECTION -> {
                return linkRefForObjectCollectionExists(widget);
            }
            case AUDIT_SEARCH -> {
                return linkRefForAuditExists(widget);
            }
            case OBJECT -> {
                return linkRefForObjectExists(widget);
            }
        }
        return false;
    }

    private boolean linkRefForObjectCollectionExists(DashboardWidgetType widget) {
        ObjectCollectionType collection = getObjectCollectionType(widget);
        if (collection != null && collection.getType() != null && collection.getType().getLocalPart() != null) {
            if (QNameUtil.match(collection.getType(), ShadowType.COMPLEX_TYPE)) {
                String oid = getResourceOid(collection.getFilter());
                return StringUtils.isNotBlank(oid) || SelectorQualifiedGetOptionsUtil.hasRawOption(collection.getGetOptions());
            }
            return LINKS_REF_COLLECTIONS.containsKey(collection.getType().getLocalPart()) && isCollectionLoadable(widget);
        }
        return false;
    }

    private boolean linkRefForAuditExists(DashboardWidgetType widget) {
        Task task = createSimpleTask("Is audit collection");
        if (DashboardUtils.isAuditCollection(getObjectCollectionRef(widget), getModelService(), task, task.getResult())) {
            return LINKS_REF_COLLECTIONS.containsKey(AuditEventRecordType.COMPLEX_TYPE.getLocalPart());
        }
        return false;
    }

    private boolean linkRefForObjectExists(DashboardWidgetType widget) {
        ObjectType object = getObjectFromObjectRef(widget);
        if (object == null) {
            return false;
        }
        return DetailsPageUtil.hasDetailsPage(object.getClass());
    }

    private CollectionRefSpecificationType getObjectCollectionRef(DashboardWidgetType model) {
        if (DashboardUtils.isCollectionRefOfCollectionNull(model)) {
            return null;
        }
        return model.getData().getCollection();
    }

    private ObjectCollectionType getObjectCollectionType(DashboardWidgetType widget) {
        CollectionRefSpecificationType collectionRef = getObjectCollectionRef(widget);
        if (collectionRef == null) {
            return null;
        }
        ObjectReferenceType ref = collectionRef.getCollectionRef();
        Task task = createSimpleTask("Search collection");
        PrismObject<ObjectCollectionType> objectCollection = WebModelServiceUtils.loadObject(ref, this, task, task.getResult());
        if (objectCollection == null) {
            return null;
        }

        return objectCollection.asObjectable();
    }

    private boolean isDataNull(DashboardWidgetType dashboardWidgetType) {
        if (dashboardWidgetType.getData() == null) {
            LOGGER.error("Data is not found in widget " + dashboardWidgetType.getIdentifier());
            return true;
        }
        return false;
    }

    private String getResourceOid(SearchFilterType searchFilterType) {
        try {
            ObjectFilter filter = getPrismContext().getQueryConverter().createObjectFilter(ShadowType.class, searchFilterType);
            return ObjectQueryUtil.getResourceOidFromFilter(filter);
        } catch (SchemaException e) {
            LOGGER.error("Cannot convert filter: {}", e.getMessage(), e);
            return null;
        }
    }

    private <O extends ObjectType> O getObjectFromObjectRef(DashboardWidgetType model) {
        if (isDataNull(model)) {
            return null;
        }
        ObjectReferenceType ref = model.getData().getObjectRef();
        if (ref == null) {
            LOGGER.error("ObjectRef of data is not found in widget " + model.getIdentifier());
            return null;
        }
        Task task = createSimpleTask("Search domain collection");
        PrismObject<O> object = WebModelServiceUtils.loadObject(ref, this, task, task.getResult());
        if (object == null) {
            LOGGER.error("Object from ObjectRef " + ref + " is null in widget " + model.getIdentifier());
            return null;
        }
        return object.asObjectable();
    }

    private void navigateToPage(DashboardWidgetType widget) {
        if (widget == null) {
            return;
        }
        DashboardWidgetSourceTypeType source = DashboardUtils.getSourceType(widget);
        if (source == null) {
            return;
        }

        switch (source) {
            case OBJECT_COLLECTION, AUDIT_SEARCH -> navigateToObjectCollectionPage(widget);
            case OBJECT -> navigateToObjectPage(widget);
        }
    }

    private void navigateToObjectCollectionPage(DashboardWidgetType widget) {
        ObjectCollectionType collection = getObjectCollectionType(widget);
        if (collection != null && collection.getType() != null && collection.getType().getLocalPart() != null) {
            Class<? extends WebPage> pageType = LINKS_REF_COLLECTIONS.get(collection.getType().getLocalPart());
            PageParameters parameters = new PageParameters();
            if (QNameUtil.match(collection.getType(), ShadowType.COMPLEX_TYPE)) {
                pageType = PageShadows.class;
            }
            if (pageType == null) {
                return;
            }
            parameters.add(PageBase.PARAMETER_DASHBOARD_TYPE_OID, dashboardModel.getObject().getOid());
            parameters.add(PageBase.PARAMETER_DASHBOARD_WIDGET_NAME, widget.getIdentifier());

            navigateToNext(pageType, parameters);
        } else {
            LOGGER.error("CollectionType from collectionRef is null in widget " + widget.getIdentifier());
        }
    }

    private void navigateToObjectPage(DashboardWidgetType widget) {
        ObjectType object = getObjectFromObjectRef(widget);
        if (object == null) {
            return;
        }
        Class<? extends WebPage> pageType = DetailsPageUtil.getObjectDetailsPage(object.getClass());
        if (pageType == null) {
            return;
        }
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());

        navigateToNext(pageType, parameters);
    }
}
