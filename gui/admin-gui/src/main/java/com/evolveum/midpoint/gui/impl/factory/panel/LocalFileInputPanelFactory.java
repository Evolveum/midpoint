/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.LocalFileInputPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Currently this factory is used only for CSV connector, filePath configuration property.
 */
@Component
public class LocalFileInputPanelFactory<T extends Serializable> extends AbstractInputGuiComponentFactory<T> {

    private static final Trace LOGGER = TraceManager.getTrace(LocalFileInputPanelFactory.class);

    private static final ItemName FILE_PATH_ITEM = ItemName.WithoutPrefix.from(
            SchemaConstants.ICF_FRAMEWORK_URI
                    + "/bundle/com.evolveum.polygon.connector-csv/com.evolveum.polygon.connector.csv.CsvConnector",
            "filePath");

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        PrismObjectWrapper<?> objectWrapper = wrapper.findObjectWrapper();
        if (objectWrapper == null) {
            return false;
        }

        PrismObject<?> object;
        try {
            object = objectWrapper.getObjectApplyDelta();
        } catch (CommonException e) {
            LOGGER.trace("Couldn't get object from object wrapper and apply delta", e);
            return false;
        }

        if (!ResourceType.class.equals(object.getCompileTimeClass())) {
            return false;
        }

        return FILE_PATH_ITEM.matches(wrapper.getItemName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<T> panelCtx) {
        // noinspection unchecked
        return new LocalFileInputPanel(panelCtx.getComponentId(), (IModel<String>) panelCtx.getRealValueModel());

    }

    @Override
    public Integer getOrder() {
        return super.getOrder() - 100;
    }
}
