package com.evolveum.midpoint.gui.api.component;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2MultiChoice;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.AttributeSettingPopupPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.web.component.prism.InputPanel;

public class ClusteringAttributeSelectorPanel extends InputPanel {
    private static final String ID_MULTISELECT = "multiselect";
    private static final String ID_SELECT_MANUALLY = "selectManually";

    protected List<ClusteringAttributeRuleType> objectToChooseFrom;
    protected IModel<List<ClusteringAttributeRuleType>> selectedObject = Model.ofList(new ArrayList<>());
    IModel<PrismPropertyValueWrapper<ClusteringAttributeSettingType>> model;
    RoleAnalysisProcessModeType processModeType;

    public ClusteringAttributeSelectorPanel(@NotNull String id,
            @NotNull IModel<PrismPropertyValueWrapper<ClusteringAttributeSettingType>> model,
            @NotNull RoleAnalysisProcessModeType processModeType) {
        super(id);
        this.model = model;
        this.processModeType = processModeType;
        this.objectToChooseFrom = createChoiceSet();
        initSelectedModel(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        initSelectionFragment();

        AjaxLink<?> configureAttributes = new AjaxLink<>(ID_SELECT_MANUALLY) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                AttributeSettingPopupPanel detailsPanel = new AttributeSettingPopupPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Configure attributes"), model);
                getPageBase().showMainPopup(detailsPanel, target);
            }
        };
        add(configureAttributes);
    }

    private void initSelectionFragment() {
        IModel<Collection<ClusteringAttributeRuleType>> multiselectModel = buildMultiSelectModel();

        ChoiceProvider<ClusteringAttributeRuleType> choiceProvider = buildChoiceProvider();

        Select2MultiChoice<ClusteringAttributeRuleType> multiselect = new Select2MultiChoice<>(ID_MULTISELECT, multiselectModel,
                choiceProvider);

        multiselect.getSettings()
                .setMinimumInputLength(0);
        multiselect.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Collection<ClusteringAttributeRuleType> refs = multiselect.getModel().getObject();
                updateSelected(refs);

            }
        });
        add(multiselect);

    }

    @NotNull
    private IModel<Collection<ClusteringAttributeRuleType>> buildMultiSelectModel() {
        return new IModel<>() {
            @Override
            public Collection<ClusteringAttributeRuleType> getObject() {
                return new ArrayList<>(getSelectedObject().getObject());
            }

            @Override
            public void setObject(Collection<ClusteringAttributeRuleType> object) {
                updateSelected(object);
            }
        };
    }

    private void initSelectedModel(@NotNull IModel<PrismPropertyValueWrapper<ClusteringAttributeSettingType>> model) {
        ClusteringAttributeSettingType realValue = model.getObject().getRealValue();

        if (realValue == null) {
            realValue = new ClusteringAttributeSettingType();
            model.getObject().setRealValue(realValue);
        }

        selectedObject = new LoadableModel<>(false) {

            @Override
            protected List<ClusteringAttributeRuleType> load() {
                ClusteringAttributeSettingType realValue = getModel().getObject().getRealValue();
                return new ArrayList<>(realValue.getClusteringAttributeRule());
            }
        };
    }


    @NotNull
    private ChoiceProvider<ClusteringAttributeRuleType> buildChoiceProvider() {
        return new ChoiceProvider<>() {
            @Override
            public String getDisplayValue(ClusteringAttributeRuleType roleAnalysisAttributeDef) {
                return roleAnalysisAttributeDef.getAttributeIdentifier();
            }

            @Override
            public String getIdValue(ClusteringAttributeRuleType roleAnalysisAttributeDef) {
                return roleAnalysisAttributeDef.getAttributeIdentifier();
            }

            @Override
            public void query(String inputString, int i, Response<ClusteringAttributeRuleType> response) {
                if (inputString == null || inputString.isEmpty()) {
                    response.addAll(getObjectToChooseFrom());
                    return;
                }

                response.addAll(performSearch(inputString));
            }

            @Override
            public Collection<ClusteringAttributeRuleType> toChoices(Collection<String> collection) {
                Collection<ClusteringAttributeRuleType> choices = new ArrayList<>();

                List<ClusteringAttributeRuleType> objectToChooseFrom = getObjectToChooseFrom();
                objectToChooseFrom.forEach(def -> {
                    if (collection.contains(def.getAttributeIdentifier())) {
                        choices.add(def);
                    }
                });

                return choices;
            }
        };
    }

    public void updateSelected(@NotNull Collection<ClusteringAttributeRuleType> poiRefs) {

        IModel<PrismPropertyValueWrapper<ClusteringAttributeSettingType>> model = getModel();
        ClusteringAttributeSettingType realValue = model.getObject().getRealValue();

        if (realValue == null) {
            realValue = new ClusteringAttributeSettingType();
            model.getObject().setRealValue(realValue);
        }

        List<ClusteringAttributeRuleType> clusteringAttributeRule = realValue.getClusteringAttributeRule();
        Set<String> identifiers = clusteringAttributeRule.stream().map(ClusteringAttributeRuleType::getAttributeIdentifier).collect(Collectors.toSet());
        for (ClusteringAttributeRuleType poiRef : poiRefs) {

            if (identifiers.contains(poiRef.getAttributeIdentifier())) {
                identifiers.remove(poiRef.getAttributeIdentifier());
                continue;
            }

            clusteringAttributeRule.add(poiRef.clone());
            identifiers.remove(poiRef.getAttributeIdentifier());
        }

        if (!identifiers.isEmpty()) {
            clusteringAttributeRule.removeIf(rule -> identifiers.contains(rule.getAttributeIdentifier()));
        }

        if (clusteringAttributeRule.size() == 1) {
            clusteringAttributeRule.get(0).setWeight(1.0);
        }

        getSelectedObject().setObject(new ArrayList<>(poiRefs));
    }

    private @NotNull List<ClusteringAttributeRuleType> createChoiceSet() {
        List<RoleAnalysisAttributeDef> attributesForUserAnalysis;
        if (processModeType.equals(RoleAnalysisProcessModeType.USER)) {
            attributesForUserAnalysis = RoleAnalysisAttributeDefUtils.getAttributesForUserAnalysis();
        } else {
            attributesForUserAnalysis = RoleAnalysisAttributeDefUtils.getAttributesForRoleAnalysis();
        }

        List<ClusteringAttributeRuleType> result = new ArrayList<>();
        for (RoleAnalysisAttributeDef def : attributesForUserAnalysis) {
            ClusteringAttributeRuleType rule = new ClusteringAttributeRuleType();
            rule.setAttributeIdentifier(def.getDisplayValue());
            rule.setSimilarity(100.0);
            rule.setWeight(1.0);
            rule.setIsMultiValue(def.isContainer());
            result.add(rule);
        }
        return result;
    }

    private @NotNull List<ClusteringAttributeRuleType> performSearch(String term) {
        List<ClusteringAttributeRuleType> results = new ArrayList<>();

        for (ClusteringAttributeRuleType def : getObjectToChooseFrom()) {
            if (def.getAttributeIdentifier().toLowerCase().contains(term.toLowerCase())) {
                results.add(def);
            }
        }
        return results;
    }

    public List<ClusteringAttributeRuleType> getObjectToChooseFrom() {
        return objectToChooseFrom;
    }

    public IModel<List<ClusteringAttributeRuleType>> getSelectedObject() {
        return this.selectedObject;
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return getFormC();
    }

    private Select2MultiChoice<?> getFormC() {
        return (Select2MultiChoice<?>) get(getPageBase().createComponentPath(ID_MULTISELECT));
    }

    public IModel<PrismPropertyValueWrapper<ClusteringAttributeSettingType>> getModel() {
        return model;
    }
}
