/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.box;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.model.api.util.DashboardUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.page.admin.server.PageTask;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.reports.PageAuditLogViewer;
import com.evolveum.midpoint.web.page.admin.resources.PageResource;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.services.PageService;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

/**
 * @author skublik
 */
public abstract class InfoBoxPanel extends BasePanel<DashboardWidgetType> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(InfoBoxPanel.class);

    private static final String ID_INFO_BOX = "infoBox";
    private static final String ID_ICON = "icon";
    private static final String ID_MESSAGE = "message";
    private static final String ID_NUMBER = "number";

    private static HashMap<String, Class<? extends WebPage>> linksRefCollections;
    private static HashMap<QName, Class<? extends WebPage>> linksRefObjects;

    private LoadableModel<DashboardWidgetDto> dasboardModel;

    static {
        linksRefCollections = new HashMap<String, Class<? extends WebPage>>() {
            private static final long serialVersionUID = 1L;

            {
                put(ResourceType.COMPLEX_TYPE.getLocalPart(), PageResources.class);
                put(AuditEventRecordType.COMPLEX_TYPE.getLocalPart(), PageAuditLogViewer.class);
                put(TaskType.COMPLEX_TYPE.getLocalPart(), PageTasks.class);
                put(UserType.COMPLEX_TYPE.getLocalPart(), PageUsers.class);
                put(RoleType.COMPLEX_TYPE.getLocalPart(), PageRoles.class);
                put(OrgType.COMPLEX_TYPE.getLocalPart(), PageOrgTree.class);
                put(ServiceType.COMPLEX_TYPE.getLocalPart(), PageServices.class);
            }
        };

        linksRefObjects = new HashMap<QName, Class<? extends WebPage>>() {
            private static final long serialVersionUID = 1L;

            {
                put(TaskType.COMPLEX_TYPE, PageTask.class);
                put(UserType.COMPLEX_TYPE, PageUser.class);
                put(RoleType.COMPLEX_TYPE, PageRole.class);
                put(OrgType.COMPLEX_TYPE, PageOrgUnit.class);
                put(ServiceType.COMPLEX_TYPE, PageService.class);
                put(ResourceType.COMPLEX_TYPE, PageResource.class);
            }
        };
    }

    public InfoBoxPanel(String id, IModel<DashboardWidgetType> model) {
        super(id, model);
        Validate.notNull(model, "Model must not be null.");
        Validate.notNull(model.getObject(), "Model object must not be null.");
        add(AttributeModifier.append("class", "dashboard-info-box"));

        dasboardModel = new LoadableModel<DashboardWidgetDto>(false) {
            @Override
            protected DashboardWidgetDto load() {
                Task task = getPageBase().createSimpleTask("Get DashboardWidget");
                try {
                    DashboardWidget dashboardWidget = getPageBase().getDashboardService().createWidgetData(getModelObject(), task, task.getResult());
                    return new DashboardWidgetDto(dashboardWidget, getPageBase());
                } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException
                        | ExpressionEvaluationException | ObjectNotFoundException e) {
                    LOGGER.error("Couldn't get DashboardWidget with widget " + getModelObject().getIdentifier(), e);
                }
                return null;
            }
        };

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();

    }

    private void initLayout() {
        WebMarkupContainer infoBox = new WebMarkupContainer(ID_INFO_BOX);
        add(infoBox);

        //TODO change to model
        Label number = new Label(ID_NUMBER, new PropertyModel<>(dasboardModel, DashboardWidgetDto.F_NUMBER_LABEL));
        infoBox.add(number);

        Label message = new Label(ID_MESSAGE, new PropertyModel<>(dasboardModel, DashboardWidgetDto.F_MESSAGE));
        infoBox.add(message);
        infoBox.add(AttributeModifier.append("style", new PropertyModel<>(dasboardModel, DashboardWidgetDto.F_STYLE_COLOR)));
        infoBox.add(AttributeModifier.append("style", new PropertyModel<>(dasboardModel, DashboardWidgetDto.F_STYLE_CSS_STYLE)));

        WebMarkupContainer infoBoxIcon = new WebMarkupContainer(ID_ICON);
        infoBox.add(infoBoxIcon);
        infoBoxIcon.add(AttributeModifier.append("class", new PropertyModel<>(dasboardModel, DashboardWidgetDto.D_ICON_CSS_CLASS)));

        customInitLayout(infoBox);
    }

    protected void customInitLayout(WebMarkupContainer infoBox) {

    }

    protected static HashMap<String, Class<? extends WebPage>> getLinksRefCollections() {
        return linksRefCollections;
    }

    protected static HashMap<QName, Class<? extends WebPage>> getLinksRefObjects() {
        return linksRefObjects;
    }

    protected void navigateToPage() {
        DashboardWidgetType dashboardWidget = getModelObject();
        if (dashboardWidget == null) {
            return;
        }
        DashboardWidgetSourceTypeType sourceType = DashboardUtils.getSourceType(dashboardWidget);
        if (sourceType == null) {
            return;
        }
        switch (sourceType) {
            case OBJECT_COLLECTION:
                navigateToObjectCollectionPage(dashboardWidget);
                break;
            case AUDIT_SEARCH:
                navigateToAuditPage(dashboardWidget);
                break;
            case OBJECT:
                navigateToObjectPage();
            }
    }

    private void navigateToObjectCollectionPage(DashboardWidgetType dashboardWidget) {
        ObjectCollectionType collection = getObjectCollectionType();
        if(collection != null && collection.getType() != null && collection.getType().getLocalPart() != null) {
            Class<? extends WebPage> pageType = getLinksRefCollections().get(collection.getType().getLocalPart());
            PageParameters parameters = new PageParameters();
            if (QNameUtil.match(collection.getType(), ShadowType.COMPLEX_TYPE)) {
                pageType = PageResource.class;
                String oid = getResourceOid(collection.getFilter().getFilterClauseXNode());
                if (oid != null) {
                    parameters.add(OnePageParameterEncoder.PARAMETER, oid);
                    Integer tab = getResourceTab(collection.getFilter().getFilterClauseXNode());
                    if (tab != null) {
                        parameters.add(PageResource.PARAMETER_SELECTED_TAB, tab);
                    } else {
                        parameters.add(PageResource.PARAMETER_SELECTED_TAB, 2);
                    }
                }
            }
            if(pageType == null) {
                return;
            }
            parameters.add(PageBase.PARAMETER_DASHBOARD_TYPE_OID, getDashboardOid());
            parameters.add(PageBase.PARAMETER_DASHBOARD_WIDGET_NAME, dashboardWidget.getIdentifier());
            getPageBase().navigateToNext(pageType, parameters);
        }  else {
            LOGGER.error("CollectionType from collectionRef is null in widget " + dashboardWidget.getIdentifier());
        }
    }

    private void navigateToAuditPage(DashboardWidgetType dashboardWidget) {
        Task task = getPageBase().createSimpleTask("Is audit collection");
        CollectionRefSpecificationType collectionRefSpecificationType = getObjectCollectionRef();
        if(DashboardUtils.isAuditCollection(collectionRefSpecificationType, getPageBase().getModelService(), task, task.getResult())) {
            PageParameters params = new PageParameters();
            params.add(PageAuditLogViewer.PARAM_DASHBOARD, getDashboardOid());
            params.add(PageAuditLogViewer.PARAM_DASHBOARD_WIDGET, getModelObject().getIdentifier());
            getPageBase().navigateToNext(PageAuditLogViewer.class, params);
        }  else {
            LOGGER.error("CollectionType from collectionRef is null in widget " + dashboardWidget.getIdentifier());
        }
    }

    private void navigateToObjectPage() {
        ObjectType object = getObjectFromObjectRef();
        if(object == null) {
            return;
        }
        QName typeName = WebComponentUtil.classToQName(getPageBase().getPrismContext(), object.getClass());
        Class<? extends WebPage> pageType = getLinksRefObjects().get(typeName);
        if(pageType == null) {
            return;
        }
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
        getPageBase().navigateToNext(pageType, parameters);
    }

    private Integer getResourceTab(MapXNode mapXNode) {
        for (QName name : mapXNode.keySet()) {
            XNode xNode = mapXNode.get(name);
            if (QNameUtil.match(name, new QName("equal"))) {
                List<MapXNode> listXNode = new ArrayList<>();
                if (xNode instanceof MapXNode) {
                    listXNode.add((MapXNode) xNode);
                } else if (xNode instanceof ListXNode) {
                    listXNode.addAll((Collection<? extends MapXNode>) ((ListXNode) xNode).asList());
                }
                for (MapXNode equalXNode : listXNode) {
                    if (equalXNode.get(new QName("path")) != null
                            && ((ItemPathType) ((PrimitiveXNode) equalXNode.get(new QName("path")))
                            .getValue()).getItemPath().equivalent(ItemPath.create("kind"))) {
                        XNode value = equalXNode.get(new QName("value"));
                        if (value instanceof PrimitiveXNode) {
                            ShadowKindType kind = ShadowKindType.fromValue(((PrimitiveXNode)value).getValueParser().getStringValue());
                            if (ShadowKindType.ACCOUNT.equals(kind)) {
                                return 2;
                            } else if (ShadowKindType.ENTITLEMENT.equals(kind)) {
                                return 3;
                            } else if (ShadowKindType.GENERIC.equals(kind)) {
                                return 4;
                            }
                            return null;
                        }
                    }
                }
            }
            if (xNode instanceof MapXNode) {
                Integer ret = getResourceTab((MapXNode) xNode);
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
    }

    private String getResourceOid(MapXNode mapXNode) {
        for (QName name : mapXNode.keySet()) {
            XNode xNode = mapXNode.get(name);
            if (QNameUtil.match(name, new QName("ref"))) {
                List<MapXNode> listXNode = new ArrayList<>();
                if (xNode instanceof MapXNode) {
                    listXNode.add((MapXNode) xNode);
                } else if (xNode instanceof ListXNode) {
                    listXNode.addAll((Collection<? extends MapXNode>) ((ListXNode) xNode).asList());
                }
                for (MapXNode equalXNode : listXNode) {
                    if (equalXNode.get(new QName("path")) != null
                            && ((ItemPathType) ((PrimitiveXNode) equalXNode.get(new QName("path")))
                            .getValue()).getItemPath().equivalent(ItemPath.create("resourceRef"))) {
                        XNode value = equalXNode.get(new QName("value"));
                        if (value instanceof MapXNode) {
                            PrimitiveXNode oid = ((PrimitiveXNode) ((MapXNode) value).get(new QName("oid")));
                            if (oid != null) {
                                return oid.getValueParser().getStringValue();
                            }
                        }
                    }
                }
            }
            if (xNode instanceof MapXNode) {
                return getResourceOid((MapXNode) xNode);
            }
        }
        return null;
    }

    protected boolean existLinkRef() {
        DashboardWidgetType dashboardWidgetType = getModelObject();
        if (dashboardWidgetType == null) {
            return false;
        }
        DashboardWidgetSourceTypeType sourceType = DashboardUtils.getSourceType(dashboardWidgetType);
        if (sourceType == null) {
            return false;
        }
        switch (sourceType) {
            case OBJECT_COLLECTION:
                ObjectCollectionType collection = getObjectCollectionType();
                if(collection != null && collection.getType() != null && collection.getType().getLocalPart() != null) {
                    if (QNameUtil.match(collection.getType(), ShadowType.COMPLEX_TYPE)) {
                        String oid = getResourceOid(collection.getFilter().getFilterClauseXNode());
                        return !StringUtils.isEmpty(oid);
                    }
                    return getLinksRefCollections().containsKey(collection.getType().getLocalPart());
                }  else {
                    return false;
                }
            case AUDIT_SEARCH:
                Task task = getPageBase().createSimpleTask("Is audit collection");
                if(DashboardUtils.isAuditCollection(getObjectCollectionRef(), getPageBase().getModelService(), task, task.getResult())) {
                    return getLinksRefCollections().containsKey(AuditEventRecordType.COMPLEX_TYPE.getLocalPart());
                }  else {
                    return false;
                }
            case OBJECT:
                ObjectType object = getObjectFromObjectRef();
                if(object == null) {
                    return false;
                }
                QName typeName = WebComponentUtil.classToQName(getPageBase().getPrismContext(), object.getClass());
                return getLinksRefObjects().containsKey(typeName);
            }
        return false;
    }

    private boolean isDataNull(DashboardWidgetType dashboardWidgetType) {
        if(dashboardWidgetType.getData() == null) {
            LOGGER.error("Data is not found in widget " + dashboardWidgetType.getIdentifier());
            return true;
        }
        return false;
    }

    private boolean isPresentationNull(IModel<DashboardWidgetType> model) {
        if(model.getObject().getPresentation() == null) {
            LOGGER.error("Presentation is not found in widget " + model.getObject().getIdentifier());
            return true;
        }
        return false;
    }

    private boolean isViewOfWidgetNull(IModel<DashboardWidgetType> model) {
        if(isPresentationNull(model)) {
            return true;
        }
        if(model.getObject().getPresentation().getView() == null) {
            LOGGER.error("View of presentation is not found in widget " + model.getObject().getIdentifier());
            return true;
        }
        return false;
    }

    private boolean isSourceTypeOfDataNull(DashboardWidgetType dashboardWidgetType) {
        if(isDataNull(dashboardWidgetType)) {
            return true;
        }
        if(dashboardWidgetType.getData().getSourceType() == null) {
            LOGGER.error("SourceType of data is not found in widget " + dashboardWidgetType.getIdentifier());
            return true;
        }
        return false;
    }

    private boolean isCollectionOfDataNull(DashboardWidgetType model) {
        if(isDataNull(model)) {
            return true;
        }
        if(model.getData().getCollection() == null) {
            LOGGER.error("Collection of data is not found in widget " + model.getIdentifier());
            return true;
        }
        return false;
    }

    private boolean isCollectionRefOfCollectionNull(DashboardWidgetType model) {
        if(isDataNull(model)) {
            return true;
        }
        if(isCollectionOfDataNull(model)) {
            return true;
        }
        ObjectReferenceType ref = model.getData().getCollection().getCollectionRef();
        if(ref == null) {
            LOGGER.error("CollectionRef of collection is not found in widget " + model.getIdentifier());
            return true;
        }
        return false;
    }

    private ObjectCollectionType getObjectCollectionType() {
        CollectionRefSpecificationType collectionRef = getObjectCollectionRef();
        if (collectionRef == null) {
            return null;
        }
        ObjectReferenceType ref = collectionRef.getCollectionRef();
        Task task = getPageBase().createSimpleTask("Search collection");
        PrismObject<ObjectCollectionType> objectCollection = WebModelServiceUtils.loadObject(ref, getPageBase(), task, task.getResult());
        if (objectCollection == null) {
            return null;
        }

        return objectCollection.asObjectable();
    }

    private CollectionRefSpecificationType getObjectCollectionRef() {
        DashboardWidgetType model = getModelObject();
        if(isCollectionRefOfCollectionNull(model)) {
            return null;
        }
        return model.getData().getCollection();
    }

    private <O extends ObjectType> O getObjectFromObjectRef() {
        DashboardWidgetType model = getModelObject();
        if(isDataNull(model)) {
            return null;
        }
        ObjectReferenceType ref = model.getData().getObjectRef();
        if(ref == null) {
            LOGGER.error("ObjectRef of data is not found in widget " + model.getIdentifier());
            return null;
        }
        Task task = getPageBase().createSimpleTask("Search domain collection");
        PrismObject<O> object = WebModelServiceUtils.loadObject(ref, getPageBase(), task, task.getResult());
        if(object == null) {
            LOGGER.error("Object from ObjectRef " + ref + " is null in widget " + model.getIdentifier());
            return null;
        }
        return object.asObjectable();
    }

    public abstract String getDashboardOid();
}
