/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.search.ReferenceValueSearchPanel;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;

public class ReferenceSearchItemPanel extends PropertySearchItemPanel<ReferenceSearchItemWrapper> {

    private static final long serialVersionUID = 1L;

    public ReferenceSearchItemPanel(String id, IModel<ReferenceSearchItemWrapper> searchItem) {
        super(id, searchItem);
    }

    @Override
    protected Component initSearchItemField(String id) {
        return new ReferenceValueSearchPanel(id,
                new PropertyModel<>(getModel(), ReferenceSearchItemWrapper.F_VALUE), getModelObject().getDef()){

            private static final long serialVersionUID = 1L;

            @Override
            public Boolean isItemPanelEnabled() {
                return true;// item.isEnabled();
            }

            @Override
            protected boolean isAllowedNotFoundObjectRef() {
                return getSearchType().equals(AuditEventRecordType.class);
            }

            @Override
            protected List<QName> getAllowedRelations() {
                if (getSearchType().equals(AuditEventRecordType.class)) {
                    return Collections.emptyList();
                }
                return super.getAllowedRelations();
            }

            @Override
            protected void referenceValueUpdated(ObjectReferenceType ort, AjaxRequestTarget target) {
                ((SearchValue<ObjectReferenceType>)ReferenceSearchItemPanel.this.getModelObject().getValue()).setValue(ort);
                ReferenceSearchItemPanel.this.updateSearchPanel(ort, target);
            }

            private Class<? extends Containerable> getSearchType() {
                return ReferenceSearchItemPanel.this.getModelObject().getSearchType();
            }
        };
    }

    private void updateSearchPanel(ObjectReferenceType ort, AjaxRequestTarget target) {
        target.add(findParent(SearchPanel.class));
    }
}
