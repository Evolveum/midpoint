/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.objectTemplate;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.ListMappingPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ObjectTemplateItemPanel;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Page;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.api.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.PageDescriptor;
import com.evolveum.midpoint.authentication.api.Url;
import com.evolveum.midpoint.web.component.ObjectBasicPanel;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

/**
 * @author skublik
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/objectTemplate", matchUrlForSecurity = "/admin/objectTemplate")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OBJECT_TEMPLATES_ALL_URL,
                        label = "PageObjectCollection.auth.objectTemplatesAll.label",
                        description = "PageObjectCollection.auth.objectTemplatesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OBJECT_TEMPLATE_URL,
                        label = "PageObjectCollection.auth.objectTemplate.label",
                        description = "PageObjectCollection.auth.objectTemplate.description")
        })
public class PageObjectTemplate extends PageAdminObjectDetails<ObjectTemplateType> {

    private static final Trace LOGGER = TraceManager.getTrace(PageObjectTemplate.class);

    public PageObjectTemplate() {
        initialize(null);
    }

    public PageObjectTemplate(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null);
    }

    public PageObjectTemplate(final PrismObject<ObjectTemplateType> userToEdit) {
        initialize(userToEdit);
    }

    public PageObjectTemplate(final PrismObject<ObjectTemplateType> unitToEdit, boolean isNewObject)  {
        initialize(unitToEdit, isNewObject);
    }

    public PageObjectTemplate(final PrismObject<ObjectTemplateType> unitToEdit, boolean isNewObject, boolean isReadonly)  {
        initialize(unitToEdit, isNewObject, isReadonly);
    }

    @Override
    public Class<ObjectTemplateType> getCompileTimeClass() {
        return ObjectTemplateType.class;
    }

    @Override
    public ObjectTemplateType createNewObject() {
        return new ObjectTemplateType(getPrismContext());
    }

    @Override
    protected ObjectSummaryPanel<ObjectTemplateType> createSummaryPanel(IModel<ObjectTemplateType> summaryModel) {
        return new ObjectTemplateSummaryPanel(ID_SUMMARY_PANEL, summaryModel, WebComponentUtil.getSummaryPanelSpecification(ObjectTemplateType.class, getCompiledGuiProfile()));
    }

    @Override
    protected Class<? extends Page> getRestartResponsePage() {
        return PageObjectTemplate.class;
    }

    private List<ITab> getTabs(){
        List<ITab> tabs = new ArrayList<>();
        tabs.add(new PanelTab(createStringResource("pageObjectTemplate.basic.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new ObjectBasicPanel<ObjectTemplateType>(panelId, getObjectModel()){
                    @Override
                    protected QName getType() {
                        return ObjectTemplateType.COMPLEX_TYPE;
                    }

                    @Override
                    protected ItemVisibility getBasicTabVisibility(ItemWrapper<?, ?> itemWrapper) {
                        if(itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ObjectTemplateType.F_SUBTYPE))
                                || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ObjectTemplateType.F_DIAGNOSTIC_INFORMATION))
                                || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ObjectTemplateType.F_LIFECYCLE_STATE))) {
                            return ItemVisibility.HIDDEN;
                        }
                        return ItemVisibility.AUTO;
                    }
                };
            }
        });

        tabs.add(new PanelTab(createStringResource("pageObjectTemplate.iterationSpecification.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel<IterationSpecificationType>(panelId, createModel(getObjectModel(), ObjectTemplateType.F_ITERATION_SPECIFICATION),
                        IterationSpecificationType.COMPLEX_TYPE);
            }
        });

        tabs.add(new PanelTab(createStringResource("pageObjectTemplate.item.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new ObjectTemplateItemPanel(panelId, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), ObjectTemplateType.F_ITEM));
            }
        });

        tabs.add(new PanelTab(createStringResource("pageObjectTemplate.mapping.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new ListMappingPanel(panelId, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), ObjectTemplateType.F_MAPPING));
            }
        });
        return tabs;
    }

    private <C extends Containerable> PrismContainerWrapperModel<ObjectTemplateType, C> createModel(IModel<PrismObjectWrapper<ObjectTemplateType>> model, ItemName itemName) {
        return PrismContainerWrapperModel.fromContainerWrapper(model, itemName);
    }

    @Override
    protected AbstractObjectMainPanel<ObjectTemplateType> createMainPanel(String id) {
        return new AbstractObjectMainPanel<ObjectTemplateType>(id, getObjectModel(), this) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<ITab> createTabs(PageAdminObjectDetails<ObjectTemplateType> parentPage) {
                return getTabs();
            }

        };
    }
}
