/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.button;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.web.component.AjaxIconButton;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.io.Serial;

public abstract class AbstractStatisticsButton<T> extends BasePanel<T> {

    @Serial private static final long serialVersionUID = 1L;

    protected static final String ID_PROCESS_STATISTICS_BUTTON = "processStatisticsButton";

    protected AbstractStatisticsButton(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(createButton());
    }

    private AjaxIconButton createButton() {
        AjaxIconButton button = new AjaxIconButton(
                ID_PROCESS_STATISTICS_BUTTON,
                getIconModel(),
                getMainButtonLabel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onButtonClick(target);
            }
        };
        button.showTitleAsLabel(true);
        button.setOutputMarkupId(true);
        button.add(AttributeModifier.append("class", getButtonCssClass()));
        return button;
    }

    protected String getButtonCssClass() {
        return isRegenerateMode() ? "btn btn-outline-primary" : "btn btn-sm btn-light border";
    }

    protected IModel<String> getIconModel() {
        return () -> isRegenerateMode() ? "fa fa-sync" : "fa-solid fa-chart-bar";
    }

    protected boolean forceRegeneration() {
        return false;
    }

    protected abstract IModel<String> getMainButtonLabel();

    protected abstract void onButtonClick(AjaxRequestTarget target);

    protected boolean isRegenerateMode() {
        return false;
    }

}
