/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.abstractrole;

import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

public class AbstractRoleDetailsModel<AR extends AbstractRoleType> extends FocusDetailsModels<AR> {

    private BusinessRoleApplicationDto patternDeltas;

    public AbstractRoleDetailsModel(LoadableDetachableModel<PrismObject<AR>> prismObjectModel, PageBase serviceLocator) {
        super(prismObjectModel, serviceLocator);
    }

    public AbstractRoleDetailsModel(LoadableDetachableModel<PrismObject<AR>> prismObjectModel, boolean history, PageBase serviceLocator) {
        super(prismObjectModel, history, serviceLocator);
    }

    public void setPatternDeltas(BusinessRoleApplicationDto patternDeltas) {
        this.patternDeltas = patternDeltas;
    }


    public BusinessRoleApplicationDto getPatternDeltas() {
        return patternDeltas;
    }

}
