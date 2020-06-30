/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.SearchFilterConfigurationPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectListViewType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

@Component
public class SearchFilterPanelFactory extends AbstractGuiComponentFactory<SearchFilterType> {

    private static final transient Trace LOGGER = TraceManager.getTrace(SearchFilterPanelFactory.class);

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return SearchFilterType.COMPLEX_TYPE.equals(wrapper.getTypeName());
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<SearchFilterType> panelCtx) {
        PrismPropertyWrapper<SearchFilterType> searchFilterItemWrapper = panelCtx.unwrapWrapperModel();
        PrismContainerValueWrapper containerWrapper = searchFilterItemWrapper.getParent();
        //todo do we want to use search filter configuration component all over the gui?
        if (containerWrapper != null){
            QName filterType = null;
            if (containerWrapper.getRealValue() instanceof ObjectCollectionType){
                ObjectCollectionType collectionObj = (ObjectCollectionType) containerWrapper.getRealValue();
                filterType = collectionObj.getType() != null ? collectionObj.getType() : ObjectType.COMPLEX_TYPE;
            } else if (containerWrapper.getRealValue() instanceof SearchItemType){
                try {
                    PrismObjectWrapper ow = containerWrapper.getParent().findObjectWrapper();
                    ItemPath searchItemPath = containerWrapper.getPath();
                    ItemPath objectCollectionItemPath = searchItemPath.subPath(0, 3);
                    Long containerId = Long.parseLong(searchItemPath.subPath(3, 4).toString());
                    PrismContainerWrapper pcw = ow.findContainer(objectCollectionItemPath);
                    List<PrismContainerValueWrapper> values = pcw.getValues();
                    for (PrismContainerValueWrapper val : values) {
                        if (val.getPath().last() instanceof Long && Long.parseLong(val.getPath().last().toString()) == containerId) {
                            Object obj = val.getRealValue();
                            if (obj instanceof GuiObjectListViewType) {
                                filterType = ((GuiObjectListViewType) obj).getType();
                            } else if (obj instanceof ObjectCollectionType) {
                                filterType = ((ObjectCollectionType) obj).getType();
                            }
                            break;
                        }
                    }
                } catch (SchemaException ex){
                    LOGGER.warn("Unable to find search filter type");
                }
            }
            if (filterType != null) {
                return new SearchFilterConfigurationPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel(),
                        WebComponentUtil.qnameToClass(panelCtx.getPageBase().getPrismContext(), filterType == null ? ObjectType.COMPLEX_TYPE : filterType));
            }
        }
        return new AceEditorPanel(panelCtx.getComponentId(), null, new SearchFilterTypeModel(panelCtx.getRealValueModel(), panelCtx.getPageBase()), 10);
    }

}
