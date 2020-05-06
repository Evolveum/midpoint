/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismReferencePanel;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.application.IComponentInitializationListener;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.Response;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.PrismHeaderPanel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SchrodingerComponentInitListener implements IComponentInitializationListener, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(SchrodingerComponentInitListener.class);

    private static final String ATTR_DATA_PREFIX = "data-s-";

    private static final String ATTR_ID = "id";
    private static final String ATTR_RESOURCE_KEY = "resource-key";
    private static final String ATTR_QNAME = "qname";

    @Override
    public void onInitialize(Component component) {
        try {
            handleId(component);
            handleLocalization(component);
        } catch (Exception ex) {
            LOGGER.error("Schrodinger component initializer failed", ex);
        }
    }

    private void handleId(Component component) {
        writeDataAttribute(component, ATTR_ID, component.getId());
    }

    private void writeDataAttribute(Component component, String key, String value) {
        if (!component.getRenderBodyOnly()) {
            component.add(AttributeModifier.append(ATTR_DATA_PREFIX + key, value));
            return;
        }

        if ("title".equals(component.getId()) && component.getParent() instanceof Page) {
            // we don't want to alter <title> element
            return;
        }

        component.add(new Behavior() {

            @Override
            public void afterRender(Component component) {
                Response resp = component.getResponse();
                resp.write("<schrodinger " + ATTR_DATA_PREFIX + key + "=\"" + value + "\"></schrodinger>");
            }
        });
    }

    private void handleLocalization(Component component) {
        if (component instanceof PrismPropertyPanel || component instanceof PrismReferencePanel) {
            ItemPanel ppp = (ItemPanel) component;
            ItemWrapper iw = (ItemWrapper) ppp.getModel().getObject();
            String key = iw.getDisplayName();

            QName qname = iw.getItemName();

            writeDataAttribute(component, ATTR_RESOURCE_KEY, key);
            writeDataAttribute(component, ATTR_QNAME, qnameToString(qname));
            return;
        }

        if (component instanceof PrismHeaderPanel) {
            PrismHeaderPanel php = (PrismHeaderPanel) component;
            String key = php.getLabel();

            writeDataAttribute(component, ATTR_RESOURCE_KEY, key);
            return;
        }

        StringResourceModel model = null;
        if (component.getDefaultModel() instanceof StringResourceModel) {
            model = (StringResourceModel) component.getDefaultModel();
        } else if (component.getInnermostModel() instanceof StringResourceModel) {
            model = (StringResourceModel) component.getInnermostModel();
        }

        if (model == null) {
            return;
        }

        try {
            String key = (String) FieldUtils.readField(model, "resourceKey", true);
            if (key != null) {
                writeDataAttribute(component, ATTR_RESOURCE_KEY, key);
            }
        } catch (Exception ex) {
            // we don't care, should be all right, unless selenium tests starts failing
        }
    }

    private String qnameToString(QName qname) {
        if (qname == null) {
            return null;
        }

        return StringUtils.join(new Object[]{qname.getNamespaceURI(), qname.getLocalPart()}, "#");
    }
}
