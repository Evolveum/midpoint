/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

    public ObjectTypeSelectPanel(String id, IModel<QName> model, Class<O> superclass) {
        super(id, model);
        initLayout(model, superclass);
    }

    private void initLayout(IModel<QName> model, final Class<O> superclass) {
        select = new DropDownChoice<>(ID_SELECT, model,
                new IModel<List<QName>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public List<QName> getObject() {
                        if (superclass == null || superclass == ObjectType.class) {
                            return ObjectTypeListUtil.createObjectTypeList();
                        }
                        if (superclass == FocusType.class) {
                            return ObjectTypeListUtil.createFocusTypeList();
                        }
                        if (superclass == AbstractRoleType.class) {
                            return ObjectTypeListUtil.createAbstractRoleTypeList();
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
