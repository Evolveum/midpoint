/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.box;

import java.util.HashMap;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.page.admin.server.PageTasks;
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
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.services.PageService;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetSourceTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

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
                put(TaskType.COMPLEX_TYPE, PageTaskEdit.class);
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
                if(pageType == null) {
                    return null;
                }
                return getPageBase().createWebPage(pageType, null);
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

    protected boolean existLinkRef() {
        IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
        DashboardWidgetSourceTypeType sourceType = getSourceType(model);
        switch (sourceType) {
        case OBJECT_COLLECTION:
            ObjectCollectionType collection = getObjectCollectionType();
            if(collection != null && collection.getType() != null && collection.getType().getLocalPart() != null) {
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
}
