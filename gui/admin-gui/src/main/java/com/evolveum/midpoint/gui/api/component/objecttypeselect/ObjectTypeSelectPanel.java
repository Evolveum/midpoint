/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.objecttypeselect;

import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class ObjectTypeSelectPanel<O extends ObjectType> extends BasePanel<QName> {
    private static final long serialVersionUID = 1L;

    private static final String ID_SELECT = "select";

    private DropDownChoice<QName> select;
    private final Class<O> superClass;

    public ObjectTypeSelectPanel(String id, IModel<QName> model, Class<O> superClass) {
        super(id, model);
        this.superClass = superClass;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout(getModel(), superClass);
    }

    private void initLayout(IModel<QName> model, final Class<O> superclass) {
        select = new DropDownChoice<>(ID_SELECT, model,
                new IModel<List<QName>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public List<QName> getObject() {
                        if (superclass == null || superclass == ObjectType.class) {
                            return ObjectTypeListUtil.createObjectTypeList(getPageBase());
                        }
                        if (superclass == FocusType.class) {
                            return ObjectTypeListUtil.createFocusTypeList(getPageBase());
                        }
                        if (superclass == AbstractRoleType.class) {
                            return ObjectTypeListUtil.createAbstractRoleTypeList(getPageBase());
                        }
                        throw new IllegalArgumentException("Unknown superclass "+superclass);
                    }
            }, new QNameObjectTypeChoiceRenderer());
        select.setNullValid(true);

        add(select);
    }

    public void addInput(Behavior behavior) {
        select.add(behavior);
    }

}
