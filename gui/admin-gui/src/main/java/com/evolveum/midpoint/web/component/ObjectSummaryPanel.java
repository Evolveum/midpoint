/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.model.api.authentication.CompiledUserProfile;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

import org.apache.wicket.model.IModel;

public abstract class ObjectSummaryPanel<O extends ObjectType> extends AbstractSummaryPanel<O> {
    private static final long serialVersionUID = -3755521482914447912L;

    public ObjectSummaryPanel(String id, Class<O> type, final IModel<O> model, ModelServiceLocator serviceLocator) {
        super(id, model, determineConfig(type, serviceLocator.getCompiledUserProfile()));
    }

    private static <O extends ObjectType> SummaryPanelSpecificationType determineConfig(Class<O> type, CompiledUserProfile compiledUserProfile) {
        GuiObjectDetailsPageType guiObjectDetailsType = compiledUserProfile.findObjectDetailsConfiguration(type);
        if (guiObjectDetailsType == null) {
            return null;
        }
        return guiObjectDetailsType.getSummaryPanel();
    }
}
