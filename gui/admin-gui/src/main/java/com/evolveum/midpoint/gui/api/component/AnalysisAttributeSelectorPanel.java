package com.evolveum.midpoint.gui.api.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2MultiChoice;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AnalysisAttributeRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AnalysisAttributeSettingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class AnalysisAttributeSelectorPanel extends InputPanel {
    private static final String ID_MULTISELECT = "multiselect";

    protected List<AnalysisAttributeRuleType> objectToChooseFrom;
    protected IModel<List<AnalysisAttributeRuleType>> selectedObject;
    protected IModel<PrismPropertyValueWrapper<AnalysisAttributeSettingType>> model;
    protected RoleAnalysisProcessModeType processModeType;

    public AnalysisAttributeSelectorPanel(@NotNull String id,
            @NotNull IModel<PrismPropertyValueWrapper<AnalysisAttributeSettingType>> model,
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
    }

    private void initSelectionFragment() {
        IModel<Collection<AnalysisAttributeRuleType>> multiselectModel = buildMultiSelectModel();

        ChoiceProvider<AnalysisAttributeRuleType> choiceProvider = buildChoiceProvider();

        Select2MultiChoice<AnalysisAttributeRuleType> multiselect = new Select2MultiChoice<>(ID_MULTISELECT, multiselectModel,
                choiceProvider);

        multiselect.getSettings()
                .setMinimumInputLength(0);
        multiselect.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Collection<AnalysisAttributeRuleType> refs = multiselect.getModel().getObject();
                updateSelected(refs);

            }
        });
        add(multiselect);

    }

    @NotNull
    private IModel<Collection<AnalysisAttributeRuleType>> buildMultiSelectModel() {
        return new IModel<>() {
            @Override
            public Collection<AnalysisAttributeRuleType> getObject() {
                return new ArrayList<>(getSelectedObject().getObject());
            }

            @Override
            public void setObject(Collection<AnalysisAttributeRuleType> object) {
                updateSelected(object);
            }
        };
    }

    private void initSelectedModel(@NotNull IModel<PrismPropertyValueWrapper<AnalysisAttributeSettingType>> model) {
        AnalysisAttributeSettingType realValue = model.getObject().getRealValue();

        if (realValue == null) {
            realValue = new AnalysisAttributeSettingType();
            model.getObject().setRealValue(realValue);
        }

        selectedObject = new LoadableModel<>(false) {

            @Override
            protected List<AnalysisAttributeRuleType> load() {
                AnalysisAttributeSettingType realValue = getModel().getObject().getRealValue();
                return new ArrayList<>(realValue.getAnalysisAttributeRule());
            }
        };
    }

    @NotNull
    private ChoiceProvider<AnalysisAttributeRuleType> buildChoiceProvider() {
        return new ChoiceProvider<>() {
            @Override
            public String getDisplayValue(AnalysisAttributeRuleType roleAnalysisAttributeDef) {
                String prefix = "";
                if (roleAnalysisAttributeDef.getPropertyType().equals(UserType.COMPLEX_TYPE)) {
                    prefix = "(User) ";
                } else {
                    prefix = "(Role) ";
                }
                return prefix + roleAnalysisAttributeDef.getAttributeIdentifier();
            }

            @Override
            public String getIdValue(AnalysisAttributeRuleType roleAnalysisAttributeDef) {
                String prefix = "";
                if (roleAnalysisAttributeDef.getPropertyType().equals(UserType.COMPLEX_TYPE)) {
                    prefix = "(User) ";
                } else {
                    prefix = "(Role) ";
                }

                return prefix + roleAnalysisAttributeDef.getAttributeIdentifier();
            }

            @Override
            public void query(String inputString, int i, Response<AnalysisAttributeRuleType> response) {
                if (inputString == null || inputString.isEmpty()) {
                    response.addAll(getObjectToChooseFrom());
                    return;
                }

                response.addAll(performSearch(inputString));
            }

            @Override
            public Collection<AnalysisAttributeRuleType> toChoices(Collection<String> collection) {
                Collection<AnalysisAttributeRuleType> choices = new ArrayList<>();

                List<AnalysisAttributeRuleType> objectToChooseFrom = getObjectToChooseFrom();
                objectToChooseFrom.forEach(def -> {

                    String prefix;
                    if (def.getPropertyType().equals(UserType.COMPLEX_TYPE)) {
                        prefix = "(User) ";
                    } else {
                        prefix = "(Role) ";
                    }

                    String value = prefix + def.getAttributeIdentifier();

                    if (collection.contains(value)) {
                        choices.add(def);
                    }
                });

                return choices;
            }
        };
    }

    public void updateSelected(@NotNull Collection<AnalysisAttributeRuleType> poiRefs) {

        IModel<PrismPropertyValueWrapper<AnalysisAttributeSettingType>> model = getModel();
        AnalysisAttributeSettingType realValue = model.getObject().getRealValue();

        if (realValue == null) {
            realValue = new AnalysisAttributeSettingType();
            model.getObject().setRealValue(realValue);
        }

        List<AnalysisAttributeRuleType> clusteringAttributeRule = realValue.getAnalysisAttributeRule();
        Set<String> identifiers = clusteringAttributeRule.stream()
                .map(AnalysisAttributeRuleType::getAttributeIdentifier).collect(Collectors.toSet());

        for (AnalysisAttributeRuleType poiRef : poiRefs) {

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

        getSelectedObject().setObject(new ArrayList<>(poiRefs));
    }

    private @NotNull List<AnalysisAttributeRuleType> createChoiceSet() {
        List<RoleAnalysisAttributeDef> attributesForUserAnalysis = new ArrayList<>(
                RoleAnalysisAttributeDefUtils.getAttributesForUserAnalysis());

        attributesForUserAnalysis.addAll(RoleAnalysisAttributeDefUtils.getAttributesForRoleAnalysis());

        List<AnalysisAttributeRuleType> result = new ArrayList<>();
        for (RoleAnalysisAttributeDef def : attributesForUserAnalysis) {
            AnalysisAttributeRuleType rule = new AnalysisAttributeRuleType();
            rule.setAttributeIdentifier(def.getDisplayValue());
            rule.setPropertyType(def.getComplexType());
            result.add(rule);
        }
        return result;
    }

    private @NotNull List<AnalysisAttributeRuleType> performSearch(String term) {
        List<AnalysisAttributeRuleType> results = new ArrayList<>();

        for (AnalysisAttributeRuleType def : getObjectToChooseFrom()) {
            if (def.getAttributeIdentifier().toLowerCase().contains(term.toLowerCase())) {
                results.add(def);
            }
        }
        return results;
    }

    public List<AnalysisAttributeRuleType> getObjectToChooseFrom() {
        return objectToChooseFrom;
    }

    public IModel<List<AnalysisAttributeRuleType>> getSelectedObject() {
        return this.selectedObject;
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return getFormC();
    }

    private Select2MultiChoice<?> getFormC() {
        return (Select2MultiChoice<?>) get(getPageBase().createComponentPath(ID_MULTISELECT));
    }

    public IModel<PrismPropertyValueWrapper<AnalysisAttributeSettingType>> getModel() {
        return model;
    }
}
