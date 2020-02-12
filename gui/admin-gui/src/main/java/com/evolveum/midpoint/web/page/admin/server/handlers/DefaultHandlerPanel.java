/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.HandlerDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class DefaultHandlerPanel<D extends HandlerDto> extends BasePanel<D> {
    private static final long serialVersionUID = 1L;

    private static final String ID_OBJECT_REF_CONTAINER = "objectRefContainer";
    private static final String ID_OBJECT_REF = "objectRef";

    public DefaultHandlerPanel(String id, IModel<D> model, PageBase parentPage) {
        super(id, model);
        initLayout(parentPage);
        setOutputMarkupId(true);
    }

    private void initLayout(final PageBase parentPage) {
        WebMarkupContainer objectRefContainer = new WebMarkupContainer(ID_OBJECT_REF_CONTAINER);
//        objectRefContainer.add(new VisibleEnableBehaviour() {
//            @Override
//            public boolean isVisible() {
//                return getModelObject().getTaskDto().getObjectRef() != null;
//            }
//        });

        final LinkPanel objectRef = new LinkPanel(ID_OBJECT_REF, new PropertyModel<>(getModel(), HandlerDto.F_OBJECT_REF_NAME)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
//                ObjectReferenceType ref = getModelObject().getObjectRef();
//                if (ref != null) {
//                    WebComponentUtil.dispatchToObjectDetailsPage(ref, parentPage, false);
//                }
            }
//            @Override
//            public boolean isEnabled() {
//                return WebComponentUtil.hasDetailsPage(getModelObject().getObjectRef());
//            }
        };
        objectRefContainer.add(objectRef);
        add(objectRefContainer);
    }

}
