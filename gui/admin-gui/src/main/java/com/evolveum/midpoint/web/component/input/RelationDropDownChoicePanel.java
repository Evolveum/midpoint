/*
 * Copyright (C) 2010-2020 Evolveum and contributors
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
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

/**
 * Created by honchar
 *
 *  todo change name? this is not "choice panel", it contains whole "form-group" with label and everything
 *  not very reusable, contains whole form-group html with label, sizing and other html stuff. Even though main "functionality"
 *  added is to create specific combobox populated with relations.
 *  better reusable alternative was written as {@link RelationDropDownChoice}
 */
@Deprecated
public class RelationDropDownChoicePanel extends BasePanel<QName> {
    private static final long serialVersionUID = 1L;

    private static final String ID_INPUT = "input";

    private final List<QName> supportedRelations;
    private final boolean allowNull;
    private QName defaultRelation;

    public RelationDropDownChoicePanel(String id, QName defaultRelation, List<QName> supportedRelations, boolean allowNull) {
        super(id);
        this.supportedRelations = supportedRelations;
        this.allowNull = allowNull;
        this.defaultRelation = defaultRelation;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        ListModel supportedRelationsModel = new ListModel<>(supportedRelations);
        if (!allowNull && defaultRelation == null) {
            defaultRelation = supportedRelations.size() > 0 ? supportedRelations.get(0) : PrismConstants.Q_ANY;
        }
        DropDownFormGroup<QName> input = new DropDownFormGroup<>(ID_INPUT, createValueModel(defaultRelation), supportedRelationsModel, getRenderer(),
                getRelationLabelModel(), createStringResource("relationDropDownChoicePanel.tooltip.relation"), null, null, !allowNull) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getNullValidDisplayValue() {
                return RelationDropDownChoicePanel.this.getNullValidDisplayValue();
            }
        };
        input.getInput().add(new EnableBehaviour(() -> isRelationDropDownEnabled()));
        input.getInput().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        input.getInput().add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                RelationDropDownChoicePanel.this.onValueChanged(target);
            }
        });
        add(input);

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
    }

    protected IModel<QName> createValueModel(QName defaultRelation) {
        return Model.of(defaultRelation);
    }

    protected IChoiceRenderer<QName> getRenderer() {
        return WebComponentUtil.getRelationChoicesRenderer();
    }

    protected boolean isRelationDropDownEnabled() {
        return true;
    }

    protected IModel<String> getRelationLabelModel() {
        return createStringResource("relationDropDownChoicePanel.relation");
    }

    protected void onValueChanged(AjaxRequestTarget target) {
    }

    public @NotNull QName getRelationValue() {
        QName relationValue = ((DropDownFormGroup<QName>) get(ID_INPUT)).getModelObject();
        return Objects.requireNonNullElse(relationValue, PrismConstants.Q_ANY);
    }

    protected String getNullValidDisplayValue() {
        return getString("DropDownChoicePanel.empty");
    }
}
