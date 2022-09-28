/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.wicket.model.LoadableDetachableModel;

public class ShadowDetailsModel extends ObjectDetailsModels<ShadowType> {

    public ShadowDetailsModel(LoadableDetachableModel<PrismObject<ShadowType>> prismObjectModel, ModelServiceLocator serviceLocator) {
        super(prismObjectModel, serviceLocator);
    }

    @Override
    protected GuiObjectDetailsPageType loadDetailsPageConfiguration() {
        return getModelServiceLocator().getCompiledGuiProfile().findShadowDetailsConfiguration(createResourceShadowCoordinates(getPrismObject().asObjectable()));
    }

    private ResourceShadowCoordinates createResourceShadowCoordinates(ShadowType shadow) {
        return new ResourceShadowCoordinates(shadow.getResourceRef().getOid(), shadow.getKind(), shadow.getIntent());
    }

}
