/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.objectdetails;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.assignment.GdprAssignmentPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public class FocusConsentTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F>{

    private static final long serialVersionUID = 1L;

    private static final String ID_ROLES = "roles";

//    private LoadableModel<List<AssignmentType>> consentsModel;

    public FocusConsentTabPanel(String id, Form<PrismObjectWrapper<F>> mainForm, LoadableModel<PrismObjectWrapper<F>> objectWrapperModel) {
        super(id, mainForm, objectWrapperModel);

        initLayout();
    }

    private void initLayout() {
        GdprAssignmentPanel consentRoles =  new GdprAssignmentPanel(ID_ROLES,
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ItemPath.create(FocusType.F_ASSIGNMENT)));
        add(consentRoles);
        consentRoles.setOutputMarkupId(true);

    }
}
