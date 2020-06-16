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

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.page.admin.server.PageTask;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
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
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditSearchDto;
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
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordItemType;

import org.jetbrains.annotations.NotNull;

/**
 * @author skublik
 */
public abstract class InfoBoxPanel extends Panel{
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(InfoBoxPanel.class);

    private static final String ID_INFO_BOX = "infoBox";
    private static final String ID_ICON = "icon";
    private static final String ID_MESSAGE = "message";
    private static final String ID_NUMBER = "number";

    private static final String DEFAULT_BACKGROUND_COLOR = "background-color:#00a65a;";
    private static final String DEFAULT_COLOR = "color: #fff !important;";
    private static final String DEFAULT_ICON = "fa fa-question";

    private static final String NUMBER_MESSAGE_UNKNOWN = "InfoBoxPanel.message.unknown";

    private static HashMap<String, Class<? extends WebPage>> linksRefCollections;
    private static HashMap<QName, Class<? extends WebPage>> linksRefObjects;

    static {
        linksRefCollections = new HashMap<String, Class<? extends WebPage>>() {
            private static final long serialVersionUID = 1L;

            {
                put(ResourceType.COMPLEX_TYPE.getLocalPart(), PageResources.class);
                put(AuditEventRecordItemType.COMPLEX_TYPE.getLocalPart(), PageAuditLogViewer.class);
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

    private static PageBase pageBase;
    private DisplayType display;

    public InfoBoxPanel(String id, IModel<DashboardWidgetType> model, PageBase pageBase) {
        super(id, model);
        Validate.notNull(model, "Model must not be null.");
        Validate.notNull(model.getObject(), "Model object must not be null.");
        add(AttributeModifier.append("class", "dashboard-info-box"));
        this.pageBase = pageBase;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();

    }

    private void initLayout() {
        IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
        IModel<DashboardWidget> data = new IModel<DashboardWidget>() {
            private static final long serialVersionUID = 1L;

            @Override
            public DashboardWidget getObject() {
                Task task = getPageBase().createSimpleTask("Get DashboardWidget");
                try {
                    DashboardWidget ret = getPageBase().getDashboardService().createWidgetData(model.getObject(), task, task.getResult());
                    setDisplay(ret.getDisplay());
                    return ret;
                } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException
                        | ExpressionEvaluationException | ObjectNotFoundException e) {
                    LOGGER.error("Couldn't get DashboardWidget with widget " + model.getObject().getIdentifier(), e);
                }
                return null;
            }
        };

        this.display = model.getObject().getDisplay();

        WebMarkupContainer infoBox = new WebMarkupContainer(ID_INFO_BOX);
        add(infoBox);

        Label number = new Label(ID_NUMBER,
                data.getObject().getNumberMessage() == null ?
                        getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN) :
                            getStringModel(data.getObject().getNumberMessage())); //number message have to add before icon because is needed evaluate variation
        infoBox.add(number);

        IModel<DisplayType> displayModel = new IModel<DisplayType>() {
            private static final long serialVersionUID = 1L;

            @Override
            public DisplayType getObject() {
                return display;
            }
        };

        Label message = null;
        if(displayModel.getObject() != null && displayModel.getObject().getLabel() != null) {
            message = new Label(ID_MESSAGE, new PropertyModel<String>(displayModel, "label"));
        } else {
            message = new Label(ID_MESSAGE, new PropertyModel<String>(model, "identifier"));
        }
        infoBox.add(message);

        if(displayModel.getObject() != null && StringUtils.isNoneBlank(displayModel.getObject().getColor())) {
            String color = displayModel.getObject().getColor();
            infoBox.add(AttributeModifier.append("style", getStringModel("background-color:" + color + ";")));
        } else {
            infoBox.add(AttributeModifier.append("style", getStringModel(DEFAULT_BACKGROUND_COLOR)));
        }

        if(displayModel.getObject() != null && StringUtils.isNoneBlank(displayModel.getObject().getCssStyle())) {
            String style = displayModel.getObject().getCssStyle();
            infoBox.add(AttributeModifier.append("style", style));
            if(!style.toLowerCase().contains(" color:") && !style.toLowerCase().startsWith("color:")) {
                infoBox.add(AttributeModifier.append("style", getStringModel(DEFAULT_COLOR)));
            }
        } else {
            infoBox.add(AttributeModifier.append("style", getStringModel(DEFAULT_COLOR)));
        }

        WebMarkupContainer infoBoxIcon = new WebMarkupContainer(ID_ICON);
        infoBox.add(infoBoxIcon);
        if(displayModel.getObject() != null && displayModel.getObject().getIcon() != null
                && StringUtils.isNoneBlank(displayModel.getObject().getIcon().getCssClass())) {
            infoBoxIcon.add(AttributeModifier.append("class", new PropertyModel<String>(displayModel, "icon.cssClass")));
        } else {
            infoBoxIcon.add(AttributeModifier.append("class", getStringModel(DEFAULT_ICON)));
        }

        customInitLayout(infoBox);
    }

    public void setDisplay(DisplayType display) {
        this.display = display;
    }

    private DashboardWidgetSourceTypeType getSourceType(IModel<DashboardWidgetType> model) {
        if(isSourceTypeOfDataNull(model)) {
            return null;
        }
        return model.getObject().getData().getSourceType();
    }

    protected void customInitLayout(WebMarkupContainer infoBox) {

    }

    private IModel<String> getStringModel(String value){
        return new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return value;
            }
        };
    }

    protected static HashMap<String, Class<? extends WebPage>> getLinksRefCollections() {
        return linksRefCollections;
    }

    protected static HashMap<QName, Class<? extends WebPage>> getLinksRefObjects() {
        return linksRefObjects;
    }

    protected static PageBase getPageBase() {
        return pageBase;
    }

    protected WebPage getLinkRef() {
        IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
        DashboardWidgetSourceTypeType sourceType = getSourceType(model);
        switch (sourceType) {
        case OBJECT_COLLECTION:
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
                    return null;
                }
                parameters.add(PageBase.PARAMETER_DASHBOARD_TYPE_OID, getDashboardOid());
                parameters.add(PageBase.PARAMETER_DASHBOARD_WIDGET_NAME, model.getObject().getIdentifier());
                return getPageBase().createWebPage(pageType, parameters);
            }  else {
                LOGGER.error("CollectionType from collectionRef is null in widget " + model.getObject().getIdentifier());
            }
            break;
        case AUDIT_SEARCH:
            collection = getObjectCollectionType();
            if(collection != null && collection.getAuditSearch() != null && collection.getAuditSearch().getRecordQuery() != null) {
                Class<? extends WebPage> pageType = getLinksRefCollections().get(AuditEventRecordItemType.COMPLEX_TYPE.getLocalPart());
                if(pageType == null) {
                    return null;
                }
                AuditSearchDto searchDto = new AuditSearchDto();
                searchDto.setCollection(collection);
                getPageBase().getSessionStorage().getAuditLog().setSearchDto(searchDto);
                return getPageBase().createWebPage(pageType, null);
            }  else {
                LOGGER.error("CollectionType from collectionRef is null in widget " + model.getObject().getIdentifier());
            }
            break;
        case OBJECT:
            ObjectType object = getObjectFromObjectRef();
            if(object == null) {
                return null;
            }
            QName typeName = WebComponentUtil.classToQName(getPageBase().getPrismContext(), object.getClass());
            Class<? extends WebPage> pageType = getLinksRefObjects().get(typeName);
            if(pageType == null) {
                return null;
            }
            PageParameters parameters = new PageParameters();
            parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
            return getPageBase().createWebPage(pageType, parameters);
        }
    return null;
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
                        if (value != null && value instanceof PrimitiveXNode) {
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
                        if (value != null && value instanceof MapXNode) {
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
        IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
        DashboardWidgetSourceTypeType sourceType = getSourceType(model);
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
            collection = getObjectCollectionType();
            if(collection != null && collection.getAuditSearch() != null && collection.getAuditSearch().getRecordQuery() != null) {
                return getLinksRefCollections().containsKey(AuditEventRecordItemType.COMPLEX_TYPE.getLocalPart());
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

    private boolean isDataNull(IModel<DashboardWidgetType> model) {
        if(model.getObject().getData() == null) {
            LOGGER.error("Data is not found in widget " + model.getObject().getIdentifier());
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

    private boolean isSourceTypeOfDataNull(IModel<DashboardWidgetType> model) {
        if(isDataNull(model)) {
            return true;
        }
        if(model.getObject().getData().getSourceType() == null) {
            LOGGER.error("SourceType of data is not found in widget " + model.getObject().getIdentifier());
            return true;
        }
        return false;
    }

    private boolean isCollectionOfDataNull(IModel<DashboardWidgetType> model) {
        if(isDataNull(model)) {
            return true;
        }
        if(model.getObject().getData().getCollection() == null) {
            LOGGER.error("Collection of data is not found in widget " + model.getObject().getIdentifier());
            return true;
        }
        return false;
    }

    private boolean isCollectionRefOfCollectionNull(IModel<DashboardWidgetType> model) {
        if(isDataNull(model)) {
            return true;
        }
        if(isCollectionOfDataNull(model)) {
            return true;
        }
        ObjectReferenceType ref = model.getObject().getData().getCollection().getCollectionRef();
        if(ref == null) {
            LOGGER.error("CollectionRef of collection is not found in widget " + model.getObject().getIdentifier());
            return true;
        }
        return false;
    }

    private ObjectCollectionType getObjectCollectionType() {
        IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
        if(isCollectionRefOfCollectionNull(model)) {
            return null;
        }
        ObjectReferenceType ref = model.getObject().getData().getCollection().getCollectionRef();
        Task task = getPageBase().createSimpleTask("Search collection");
        ObjectCollectionType collection = (ObjectCollectionType)WebModelServiceUtils.loadObject(ref,
                getPageBase(), task, task.getResult()).getRealValue();
        return collection;
    }

    private ObjectType getObjectFromObjectRef() {
        IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
        if(isDataNull(model)) {
            return null;
        }
        ObjectReferenceType ref = model.getObject().getData().getObjectRef();
        if(ref == null) {
            LOGGER.error("ObjectRef of data is not found in widget " + model.getObject().getIdentifier());
            return null;
        }
        Task task = getPageBase().createSimpleTask("Search domain collection");
        ObjectType object = WebModelServiceUtils.loadObject(ref,
                getPageBase(), task, task.getResult()).getRealValue();
        if(object == null) {
            LOGGER.error("Object from ObjectRef " + ref + " is null in widget " + model.getObject().getIdentifier());
        }
        return object;
    }

    public abstract String getDashboardOid();
}
