/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

public abstract class PageAdminAbstractRole<T extends AbstractRoleType> extends PageAdminFocus<T> {
    private static final long serialVersionUID = 1L;

    public PageAdminAbstractRole() {
        super();
    }

    public PageAdminAbstractRole(PageParameters parameters) {
        super(parameters);
    }

    public PageAdminAbstractRole(final PrismObject<T> abstractRole) {
        super(abstractRole);
    }

    public PageAdminAbstractRole(final PrismObject<T> userToEdit, boolean isNewObject) {
        super(userToEdit, isNewObject);
    }


    public PageAdminAbstractRole(final PrismObject<T> abstractRole, boolean isNewObject, boolean isReadonly) {
        super(abstractRole, isNewObject, isReadonly);
    }

    @Override
    protected void prepareObjectForAdd(PrismObject<T> focus) throws SchemaException {
        super.prepareObjectForAdd(focus);
    }

    @Override
    protected void initializeModel(final PrismObject<T> objectToEdit, boolean isNewObject, boolean isReadonly) {
        super.initializeModel(objectToEdit, isNewObject, isReadonly);
    }
}
