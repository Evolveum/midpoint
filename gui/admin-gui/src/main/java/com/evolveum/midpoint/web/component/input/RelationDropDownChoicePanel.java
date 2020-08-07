/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Created by honchar
 */
public class RelationDropDownChoicePanel extends BasePanel<QName> {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = RelationDropDownChoicePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_RELATION_DEFINITIONS = DOT_CLASS + "loadRelationDefinitions";
    private static final Trace LOGGER = TraceManager.getTrace(RelationDropDownChoicePanel.class);

    private static final String ID_INPUT = "input";

    private List<QName> supportedRelations;
    private boolean allowNull;
    private QName defaultRelation;

//    public RelationDropDownChoicePanel(String id, IModel<QName> model, AreaCategoryType category) {
//        this(id, model, category, false);
//    }

    public RelationDropDownChoicePanel(String id, QName defaultRelation, List<QName> supportedRelations, boolean allowNull) {
        super(id);
        this.supportedRelations = supportedRelations;
        this.allowNull = allowNull;
        this.defaultRelation = defaultRelation;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();

        ListModel supportedRelationsModel = new ListModel<>(supportedRelations);
        if (!allowNull && defaultRelation == null) {
            if (CollectionUtils.isNotEmpty(supportedRelations)){
                List<QName> sortedRelations  = WebComponentUtil.sortDropDownChoices(supportedRelationsModel, getRenderer());
                defaultRelation = sortedRelations.get(0);
            } else {
                defaultRelation = PrismConstants.Q_ANY;
            }
            defaultRelation = supportedRelations.size() > 0 ? supportedRelations.get(0) : PrismConstants.Q_ANY;
        }
        DropDownFormGroup<QName> input = new DropDownFormGroup<QName>(ID_INPUT, Model.of(defaultRelation), supportedRelationsModel, getRenderer(),
                getRelationLabelModel(), "relationDropDownChoicePanel.tooltip.relation", true, "col-md-4",
                getRelationLabelModel() == null || StringUtils.isEmpty(getRelationLabelModel().getObject()) ? "" : "col-md-8", !allowNull){
            private static final long serialVersionUID = 1L;

            @Override
            protected String getNullValidDisplayValue(){
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

    protected IChoiceRenderer<QName> getRenderer(){
        return new IChoiceRenderer<QName>() {

            private static final long serialVersionUID = 1L;

            @Override
            public QName getObject(String id, IModel choices) {
                if (StringUtils.isBlank(id)) {
                    return null;
                }
                return ((List<QName>)choices.getObject()).get(Integer.parseInt(id));
            }

            @Override
            public Object getDisplayValue(QName object) {
                RelationDefinitionType def = WebComponentUtil.getRelationDefinition(object);
                if (def != null){
                    DisplayType display = def.getDisplay();
                    if (display != null){
                        PolyStringType label = display.getLabel();
                        if (PolyStringUtils.isNotEmpty(label)){
                            return getPageBase().createStringResource(label).getString();
                        }
                    }
                }
                return object.getLocalPart();
            }

            @Override
            public String getIdValue(QName object, int index) {
                return Integer.toString(index);
            }
        };
    }

    protected boolean isRelationDropDownEnabled(){
        return true;
    }

    protected IModel<String> getRelationLabelModel(){
        return createStringResource("relationDropDownChoicePanel.relation");
    }

   protected void onValueChanged(AjaxRequestTarget target){
    }

    public QName getRelationValue() {
        QName relationValue = ((DropDownFormGroup<QName>) get(ID_INPUT)).getModelObject();
        if (relationValue == null){
            return PrismConstants.Q_ANY;
        } else {
            return relationValue;
        }
    }

    protected String getNullValidDisplayValue(){
        return getString("DropDownChoicePanel.empty");
    }
}
