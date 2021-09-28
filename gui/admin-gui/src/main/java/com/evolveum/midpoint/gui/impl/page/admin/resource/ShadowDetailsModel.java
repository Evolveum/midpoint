/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ShadowDetailsModel extends ObjectDetailsModels<ShadowType> {

    public ShadowDetailsModel(LoadableModel<PrismObject<ShadowType>> prismObjectModel, ModelServiceLocator serviceLocator) {
        super(prismObjectModel, serviceLocator);
    }

    protected GuiObjectDetailsPageType loadDetailsPageConfiguration(PrismObject<ShadowType> prismObject) {
        return getModelServiceLocator().getCompiledGuiProfile().findShadowDetailsConfiguration(createResourceShadowDiscriminator(prismObject.asObjectable()));
    }

    private ResourceShadowDiscriminator createResourceShadowDiscriminator(ShadowType shadow) {
        return new ResourceShadowDiscriminator(shadow.getResourceRef().getOid(), shadow.getKind(), shadow.getIntent(), null, false);
    }

}
