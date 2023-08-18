/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.abstractrole;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.BusinessRoleApplicationDto;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

import org.apache.wicket.model.LoadableDetachableModel;

import java.util.ArrayList;
import java.util.List;

public class AbstractRoleDetailsModel<AR extends AbstractRoleType> extends FocusDetailsModels<AR> {

    private List<BusinessRoleApplicationDto> patternDeltas = new ArrayList<>();

    public AbstractRoleDetailsModel(LoadableDetachableModel<PrismObject<AR>> prismObjectModel, PageBase serviceLocator) {
        super(prismObjectModel, serviceLocator);
    }

    public AbstractRoleDetailsModel(LoadableDetachableModel<PrismObject<AR>> prismObjectModel, boolean history, PageBase serviceLocator) {
        super(prismObjectModel, history, serviceLocator);
    }

    public void setPatternDeltas(List<BusinessRoleApplicationDto> patternDeltas) {
        this.patternDeltas = patternDeltas;
    }

    public void addPatternDeltas(List<BusinessRoleApplicationDto> patternDeltas) {
        this.patternDeltas.addAll(patternDeltas);
    }

    public List<BusinessRoleApplicationDto> getPatternDeltas() {
        return patternDeltas;
    }

}
