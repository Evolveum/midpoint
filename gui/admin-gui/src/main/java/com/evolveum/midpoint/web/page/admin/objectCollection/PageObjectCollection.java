/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.objectCollection;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.ObjectBasicPanel;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.progress.ProgressPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author skublik
 */
@PageDescriptor(
        url  = "/admin/objectCollection",
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                        label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                        description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OBJECT_COLLECTIONS_ALL_URL,
                        label = "PageObjectCollection.auth.objectCollectionsAll.label",
                        description = "PageObjectCollection.auth.objectCollectionsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OBJECT_COLLECTION_URL,
                        label = "PageObjectCollection.auth.objectCollection.label",
                        description = "PageObjectCollection.auth.objectCollection.description")
        })
public class PageObjectCollection extends PageAdminObjectDetails<ObjectCollectionType> {

    private static final Trace LOGGER = TraceManager.getTrace(PageObjectCollection.class);

    public PageObjectCollection() {
        initialize(null);
    }

    public PageObjectCollection(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null);
    }

    public PageObjectCollection(final PrismObject<ObjectCollectionType> userToEdit) {
        initialize(userToEdit);
    }

    public PageObjectCollection(final PrismObject<ObjectCollectionType> unitToEdit, boolean isNewObject)  {
        initialize(unitToEdit, isNewObject);
    }

    public PageObjectCollection(final PrismObject<ObjectCollectionType> unitToEdit, boolean isNewObject, boolean isReadonly)  {
        initialize(unitToEdit, isNewObject, isReadonly);
    }

    @Override
    public Class<ObjectCollectionType> getCompileTimeClass() {
        return ObjectCollectionType.class;
    }

    @Override
    protected ObjectCollectionType createNewObject() {
        return new ObjectCollectionType(getPrismContext());
    }

    @Override
    protected ObjectSummaryPanel<ObjectCollectionType> createSummaryPanel(IModel<ObjectCollectionType> summaryModel) {
        return new ObjectCollectionSummaryPanel(ID_SUMMARY_PANEL, summaryModel, this);
    }

    @Override
    protected Class<? extends Page> getRestartResponsePage() {
        return PageObjectCollection.class;
    }

    private List<ITab> getTabs(){
        List<ITab> tabs = new ArrayList<>();
        tabs.add(new PanelTab(createStringResource("pageObjectCollection.basic.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new ObjectBasicPanel<ObjectCollectionType>(panelId, getObjectModel()){
                    @Override
                    protected QName getType() {
                        return ObjectCollectionType.COMPLEX_TYPE;
                    }

                    @Override
                    protected ItemVisibility getBasicTabVisibility(ItemWrapper<?, ?> itemWrapper) {
                        if(itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ObjectCollectionType.F_SUBTYPE))
                                || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ObjectCollectionType.F_DIAGNOSTIC_INFORMATION))
                                || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ObjectCollectionType.F_LIFECYCLE_STATE))) {
                            return ItemVisibility.HIDDEN;
                        }
                        return ItemVisibility.AUTO;
                    }
                };
            }
        });

        tabs.add(new PanelTab(createStringResource("pageObjectCollection.baseCollection.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel<CollectionRefSpecificationType>(panelId, createModel(getObjectModel(), ObjectCollectionType.F_BASE_COLLECTION),
                        CollectionRefSpecificationType.COMPLEX_TYPE) {
                    @Override
                    protected ItemVisibility getVisibility(ItemPath itemPath) {
                        if (ItemPath.create(ObjectCollectionType.F_BASE_COLLECTION, CollectionRefSpecificationType.F_BASE_COLLECTION_REF)
                                .isSuperPathOrEquivalent(itemPath)) {
                            return ItemVisibility.HIDDEN;
                        }
                        return ItemVisibility.AUTO;
                    }
                };
            }
        });

        tabs.add(new PanelTab(createStringResource("pageObjectCollection.defaultView.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createContainerPanel(panelId, getObjectModel(), ObjectCollectionType.F_DEFAULT_VIEW, GuiObjectListViewType.COMPLEX_TYPE);
            }
        });

        tabs.add(new PanelTab(createStringResource("pageObjectCollection.domain.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createContainerPanel(panelId, getObjectModel(), ObjectCollectionType.F_DOMAIN, CollectionRefSpecificationType.COMPLEX_TYPE);
            }
        });

        tabs.add(new PanelTab(createStringResource("pageObjectCollection.option.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createContainerPanel(panelId, getObjectModel(), ObjectCollectionType.F_GET_OPTIONS, SelectorQualifiedGetOptionsType.COMPLEX_TYPE);
            }
        });
        return tabs;
    }

    private <C extends Containerable> SingleContainerPanel<C> createContainerPanel(String panelId, IModel<PrismObjectWrapper<ObjectCollectionType>> objectModel, ItemName propertyName, QName propertyType) {
        return new SingleContainerPanel<C>(panelId, createModel(objectModel, propertyName), propertyType);
    }

    private <C extends Containerable> PrismContainerWrapperModel<ObjectCollectionType, C> createModel(IModel<PrismObjectWrapper<ObjectCollectionType>> model, ItemName itemName) {
        return PrismContainerWrapperModel.fromContainerWrapper(model, itemName);
    }

//    @Override
//    public void finishProcessing(AjaxRequestTarget target, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, boolean returningFromAsync, OperationResult result) {
//        if (!isKeepDisplayingResults()) {
//            showResult(result);
//            redirectBack();
//        }
//    }

    @Override
    protected AbstractObjectMainPanel<ObjectCollectionType> createMainPanel(String id) {
        return new AbstractObjectMainPanel<ObjectCollectionType>(id, getObjectModel(), this) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<ITab> createTabs(PageAdminObjectDetails<ObjectCollectionType> parentPage) {
                return getTabs();
            }

        };
    }
}
