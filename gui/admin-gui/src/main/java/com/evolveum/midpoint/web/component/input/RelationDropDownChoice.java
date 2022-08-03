/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RelationDropDownChoice extends DropDownChoice<QName> {

    public RelationDropDownChoice(String id, QName defaultRelation, List<QName> supportedRelations, boolean allowNull) {
        super(id);

        if (!allowNull && defaultRelation == null) {
            defaultRelation = supportedRelations.size() > 0 ? supportedRelations.get(0) : PrismConstants.Q_ANY;
        }

        setModel(createValueModel(defaultRelation));
        setChoices(new ListModel<>(supportedRelations));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setChoiceRenderer(WebComponentUtil.getRelationChoicesRenderer());

        add(new EnableBehaviour(() -> isRelationDropDownEnabled()));
        add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                onValueChanged(target);
            }
        });
    }

    protected boolean isRelationDropDownEnabled() {
        return true;
    }

    protected void onValueChanged(AjaxRequestTarget target) {
    }

    protected IModel<QName> createValueModel(QName defaultRelation) {
        return Model.of(defaultRelation);
    }

    @Override
    protected String getNullValidDisplayValue() {
        return getString("DropDownChoicePanel.empty");
    }

    public @NotNull QName getRelationValue() {
        QName relationValue = getModelObject();
        return Objects.requireNonNullElse(relationValue, PrismConstants.Q_ANY);
    }
}
