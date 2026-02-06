/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.panel;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.ObjectCollectionSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;

import java.io.Serial;

public class ObjectCollectionSearchItemPanel extends SingleSearchItemPanel<ObjectCollectionSearchItemWrapper> {
    @Serial private static final long serialVersionUID = 1L;

    public ObjectCollectionSearchItemPanel(String id, IModel<ObjectCollectionSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField(String id) {
        IModel<String> nameModel = super.createLabelModel();
        String oid = null;
        ObjectCollectionSearchItemWrapper item = getModelObject();
        if (item != null && item.getObjectCollectionView().getCollection() != null
                && item.getObjectCollectionView().getCollection().getCollectionRef() != null) {
            oid = item.getObjectCollectionView().getCollection().getCollectionRef().getOid();
        }
        String finalOid = oid;
        AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(id, nameModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                DetailsPageUtil.dispatchToObjectDetailsPage(ObjectCollectionType.class, finalOid, this, true);
            }

            @Override
            public boolean isEnabled() {
                return StringUtils.isNotEmpty(finalOid) && WebComponentUtil.isAuthorized(ObjectCollectionType.class);
            }
        };
        ajaxLinkPanel.setOutputMarkupId(true);
        return ajaxLinkPanel;
    }

    @Override
    protected IModel<String> createLabelModel() {
        return () -> "";
    }
}
