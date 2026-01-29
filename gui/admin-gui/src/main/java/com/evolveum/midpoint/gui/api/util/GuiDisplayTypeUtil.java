/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.page.admin.server.dto.ApprovalOutcomeIcon;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public class GuiDisplayTypeUtil {

    private static final Trace LOGGER = TraceManager.getTrace(GuiDisplayTypeUtil.class);

    public static DisplayType createDisplayType(String iconCssClass) {
        return createDisplayType(iconCssClass, "", "");
    }

    public static DisplayType createDisplayType(ApprovalOutcomeIcon caseIcon) {
        return createDisplayType(caseIcon.getIcon(), "", caseIcon.getTitle());
    }

    public static DisplayType createDisplayType(OperationResultStatusPresentationProperties OperationIcon) {
        return createDisplayType(OperationIcon.getIcon(), "", OperationIcon.getStatusLabelKey());
    }

    public static DisplayType createDisplayType(String iconCssClass, String iconColor, String tooltip) {
        return createDisplayType(iconCssClass, iconColor, WebComponentUtil.createPolyFromOrigString(tooltip));
    }

    public static DisplayType createDisplayType(String iconCssClass, String iconColor, String label, String tooltip) {
        return createDisplayType(iconCssClass, iconColor, WebComponentUtil.createPolyFromOrigString(label), WebComponentUtil.createPolyFromOrigString(tooltip));
    }

    public static DisplayType createDisplayType(String iconCssClass, PolyStringType tooltip) {
        return createDisplayType(iconCssClass, "", tooltip);
    }

    public static DisplayType createDisplayType(String iconCssClass, String iconColor, PolyStringType tooltip) {
        return createDisplayType(iconCssClass, iconColor, null, tooltip);
    }

    public static DisplayType createDisplayType(String iconCssClass, String iconColor, PolyStringType label, PolyStringType tooltip) {
        DisplayType displayType = new DisplayType();
        IconType icon = new IconType();
        icon.setCssClass(iconCssClass != null ? iconCssClass.trim() : iconCssClass);
        icon.setColor(iconColor);
        displayType.setIcon(icon);
        displayType.setLabel(label);

        displayType.setTooltip(tooltip);
        return displayType;
    }

    public static <O extends ObjectType> DisplayType getArchetypePolicyDisplayType(O object, PageAdminLTE pageBase) {
        if (object == null) {
            return null;
        }

        return getArchetypePolicyDisplayType(object.asPrismObject(), pageBase);
    }

    public static <O extends ObjectType> DisplayType getArchetypePolicyDisplayType(PrismObject<O> object, PageAdminLTE pageBase) {
        if (object != null) {
            try {
                ArchetypePolicyType archetypePolicy = WebComponentUtil.getArchetypeSpecification(object, pageBase);
                if (archetypePolicy != null) {
                    return archetypePolicy.getDisplay();
                }
            } catch (Exception e) {
                LOGGER.debug("Couldn't get archetype policy for " + object, e);
                return null;
            }
        }
        return null;
    }

    private static DisplayType createSimpleObjectRelationDisplayType(PageBase page, String key, String type, String relation) {
        if (type == null) {
            type = "";
        }
        if (relation == null) {
            relation = "";
        }
        String label = page.createStringResource(key, type, relation).getString();
        return createDisplayType("", "", label, label);
    }

    public static DisplayType getAssignmentObjectRelationDisplayType(PageBase pageBase, AssignmentObjectRelation assignmentTargetRelation,
            String defaultTitleKey) {
        if (assignmentTargetRelation == null) {
            return createSimpleObjectRelationDisplayType(pageBase, defaultTitleKey, null, null);
        }

        String typeTitle = "";
        String typeValue = "";
        if (CollectionUtils.isNotEmpty(assignmentTargetRelation.getArchetypeRefs())) {
            OperationResult result = new OperationResult(pageBase.getClass().getSimpleName() + "." + "loadArchetypeObject");
            try {
                ArchetypeType archetype = pageBase.getModelObjectResolver().resolve(assignmentTargetRelation.getArchetypeRefs().get(0), ArchetypeType.class,
                        null, null, pageBase.createSimpleTask(result.getOperation()), result);
                if (archetype != null) {
                    DisplayType archetypeDisplayType = archetype.getArchetypePolicy() != null ? archetype.getArchetypePolicy().getDisplay() : null;
                    String archetypeTooltip = archetypeDisplayType != null && archetypeDisplayType.getLabel() != null &&
                            StringUtils.isNotEmpty(archetypeDisplayType.getLabel().getOrig()) ?
                            archetypeDisplayType.getLabel().getOrig() :
                            (archetype.getName() != null && StringUtils.isNotEmpty(archetype.getName().getOrig()) ?
                                    archetype.getName().getOrig() : null);
                    typeValue = archetypeTooltip;
                    typeTitle = StringUtils.isNotEmpty(archetypeTooltip) ?
                            pageBase.createStringResource("abstractRoleMemberPanel.withType", archetypeTooltip).getString() : "";
                }
            } catch (Exception ex) {
                LOGGER.error("Couldn't load archetype object. " + ex.getLocalizedMessage());
            }
        } else if (CollectionUtils.isNotEmpty(assignmentTargetRelation.getObjectTypes())) {
            QName type = !CollectionUtils.isEmpty(assignmentTargetRelation.getObjectTypes()) ?
                    assignmentTargetRelation.getObjectTypes().get(0) : null;
            String typeName = type != null ? pageBase.createStringResource("ObjectType." + type.getLocalPart()).getString() : null;
            typeValue = typeName;
            typeTitle = StringUtils.isNotEmpty(typeName) ?
                    pageBase.createStringResource("abstractRoleMemberPanel.withType", typeName).getString() : "";
        }

        QName relation = !CollectionUtils.isEmpty(assignmentTargetRelation.getRelations()) ?
                assignmentTargetRelation.getRelations().get(0) : null;

        String relationValue = "";
        String relationTitle = "";
        if (relation != null) {
            RelationDefinitionType def = RelationUtil.getRelationDefinition(relation);
            if (def != null) {
                DisplayType displayType = null;
                if (def.getDisplay() == null) {
                    displayType = new DisplayType();
                } else {
                    displayType = createDisplayType(def.getDisplay().getCssClass());
                    if (def.getDisplay().getIcon() != null) {
                        displayType.setIcon(new IconType());
                        displayType.getIcon().setCssClass(def.getDisplay().getIcon().getCssClass());
                        displayType.getIcon().setColor(def.getDisplay().getIcon().getColor());
                    }
                }
                if (def.getDisplay().getLabel() != null) {
                    relationValue = LocalizationUtil.translatePolyString(def.getDisplay().getLabel());
                } else {
                    String relationKey = "RelationTypes." + RelationTypes.getRelationTypeByRelationValue(relation);
                    relationValue = pageBase.createStringResource(relationKey).getString();
                    if (StringUtils.isEmpty(relationValue) || relationKey.equals(relationValue)) {
                        relationValue = relation.getLocalPart();
                    }
                }
                displayType.setLabel(new PolyStringType(typeValue + " (" + relationValue + ")"));

                relationTitle = pageBase.createStringResource("abstractRoleMemberPanel.withRelation", relationValue).getString();

                if (displayType.getIcon() == null || StringUtils.isEmpty(displayType.getIcon().getCssClass())) {
                    displayType.setIcon(IconAndStylesUtil.createIconType(""));
                }
                displayType.setTooltip(WebComponentUtil.createPolyFromOrigString(pageBase.createStringResource(defaultTitleKey, typeTitle, relationTitle).getString()));
                return displayType;
            }
        }

        return createSimpleObjectRelationDisplayType(pageBase, defaultTitleKey, typeTitle, relationTitle);
    }

    public static DisplayType getNewObjectDisplayTypeFromCollectionView(CompiledObjectCollectionView view) {
        DisplayType displayType = view != null ? view.getDisplay() : null;
        if (displayType == null) {
            displayType = createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green", "");
        }

        if (displayType.getIcon() == null || displayType.getIcon().getCssClass() == null) {
            MiscSchemaUtil.mergeDisplay(displayType, createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green", ""));
        }

        if (!PolyStringUtils.isEmpty(displayType.getSingularLabel()) || !PolyStringUtils.isEmpty(displayType.getLabel())) {
            PolyStringType label = displayType.getSingularLabel() != null ?
                    displayType.getSingularLabel() : displayType.getLabel();

            String name = LocalizationUtil.translatePolyString(label);

            String tooltip = LocalizationUtil.translate("MainObjectListPanel.newObjectWithName", new Object[] { name });

            displayType.setTooltip(WebComponentUtil.createPolyFromOrigString(tooltip));
        }
        return view != null ? view.getDisplay() : null;
    }

    public static <O extends ObjectType> DisplayType getDisplayTypeForObject(PrismObject<O> obj, OperationResult result, PageBase pageBase) {
        if (obj == null) {
            return null;
        }

        return getDisplayTypeForObject(obj.asObjectable(), result, pageBase);
    }

    public static <O extends ObjectType> DisplayType getDisplayTypeForObject(O obj, OperationResult result, PageBase pageBase) {
        if (obj == null) {
            return null;
        }
        DisplayType displayType = getArchetypePolicyDisplayType(obj.asPrismObject(), pageBase);

        if (displayType == null) {
            displayType = createDisplayType(IconAndStylesUtil.createDefaultIcon(obj.asPrismObject()),
                    "", ColumnUtils.getIconColumnTitle(obj, result));
        }
        return displayType;
    }

    public static String getDisplayCssClass(DisplayType displayType) {
        if (displayType == null) {
            return "";
        }
        return displayType.getCssClass();
    }

    public static String getIconCssClass(DisplayType displayType) {
        if (displayType == null || displayType.getIcon() == null) {
            return "";
        }
        return displayType.getIcon().getCssClass();
    }

    public static PolyStringType getLabel(DisplayType displayType) {
        return displayType == null ? null : displayType.getLabel();
    }

    public static String getTranslatedLabel(DisplayType displayType) {
        if (displayType == null || displayType.getLabel() == null) {
            return "";
        }
        return LocalizationUtil.translatePolyString(displayType.getLabel());
    }

    public static String getIconColor(DisplayType displayType) {
        if (displayType == null || displayType.getIcon() == null) {
            return "";
        }
        return removeStringAfterSemicolon(displayType.getIcon().getColor());
    }

    public static String getHelp(DisplayType displayType) {
        if (displayType == null || displayType.getHelp() == null) {
            return "";
        }
        return LocalizationUtil.translatePolyString(displayType.getHelp());
    }

    public static String getTooltip(DisplayType displayType) {
        if (displayType == null || displayType.getTooltip() == null) {
            return "";
        }
        return LocalizationUtil.translatePolyString(displayType.getTooltip());
    }

    public static String getDisplayTypeTitle(DisplayType displayType) {
        if (displayType == null || displayType.getTooltip() == null) {
            return "";
        }
        return displayType.getTooltip().getOrig();
    }

    public static boolean existsIconDisplay(CompiledObjectCollectionView view) {
        if (view == null) {
            return false;
        }
        return existsIconDisplay(view.getDisplay());
    }

    private static boolean existsIconDisplay(DisplayType display) {
        if (display == null) {
            return false;
        }
        if (display.getIcon() == null) {
            return false;
        }
        return StringUtils.isNotBlank(display.getIcon().getCssClass());
    }

    public static boolean containsDifferentIcon(DisplayType display, String iconCss) {
        if (existsIconDisplay(display)) {
            return !display.getIcon().getCssClass().contains(iconCss);
        }
        return true;
    }

    public static DisplayType createDisplayTypeWithLabel(String labelKey) {
        return createDisplayTypeWithLabel(null, labelKey);
    }

    public static DisplayType createDisplayTypeWithLabel(String labelOrig, String labelKey) {
        DisplayType display = new DisplayType();
        display.setLabel(createPolyStringType(labelOrig, labelKey));
        return display;
    }

    public static String removeStringAfterSemicolon(String headerColor) {
        if (headerColor == null || !headerColor.contains(";")) {
            return headerColor;
        }
        return headerColor.substring(0, headerColor.indexOf(";"));
    }

    public static DisplayType getDisplayTypeForStrengthOfMapping(
            @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel,
            @Nullable String additionalCss) {
        PrismContainerValueWrapper<MappingType> mapping = rowModel.getObject();
        MappingType mappingBean = mapping.getRealValue();

        MappingStrengthType strength = mappingBean.getStrength();
        if (strength == null) {
            strength = MappingStrengthType.NORMAL;
        }

        String cssClass = "fa fa-circle-half-stroke";

        switch (strength) {
            case WEAK -> cssClass = "fa-regular fa-circle";
            case STRONG -> cssClass = "fa fa-circle";
        }

        if (additionalCss != null) {
            cssClass += cssClass + " " + additionalCss;
        }

        return new DisplayType()
                .tooltip(LocalizationUtil.translate(
                        "AbstractSpecificMappingTileTable.tile.help",
                        new Object[] { LocalizationUtil.translateEnum(strength) }))
                .beginIcon()
                .cssClass(cssClass)
                .end();
    }

    public static DisplayType combineDisplay(DisplayType display, DisplayType variationDisplay) {
        DisplayType combinedDisplay = new DisplayType();
        if (variationDisplay == null) {
            return display;
        }
        if (display == null) {
            return variationDisplay;
        }
        if (StringUtils.isBlank(variationDisplay.getColor())) {
            combinedDisplay.setColor(display.getColor());
        } else {
            combinedDisplay.setColor(variationDisplay.getColor());
        }
        if (StringUtils.isBlank(variationDisplay.getCssClass())) {
            combinedDisplay.setCssClass(display.getCssClass());
        } else {
            combinedDisplay.setCssClass(variationDisplay.getCssClass());
        }
        if (StringUtils.isBlank(variationDisplay.getCssStyle())) {
            combinedDisplay.setCssStyle(display.getCssStyle());
        } else {
            combinedDisplay.setCssStyle(variationDisplay.getCssStyle());
        }
        if (variationDisplay.getHelp() == null) {
            combinedDisplay.setHelp(display.getHelp());
        } else {
            combinedDisplay.setHelp(variationDisplay.getHelp());
        }
        if (variationDisplay.getLabel() == null) {
            combinedDisplay.setLabel(display.getLabel());
        } else {
            combinedDisplay.setLabel(variationDisplay.getLabel());
        }
        if (variationDisplay.getSingularLabel() == null) {
            combinedDisplay.setSingularLabel(display.getSingularLabel());
        } else {
            combinedDisplay.setSingularLabel(variationDisplay.getSingularLabel());
        }
        if (variationDisplay.getPluralLabel() == null) {
            combinedDisplay.setPluralLabel(display.getPluralLabel());
        } else {
            combinedDisplay.setPluralLabel(variationDisplay.getPluralLabel());
        }
        if (variationDisplay.getTooltip() == null) {
            combinedDisplay.setTooltip(display.getTooltip());
        } else {
            combinedDisplay.setTooltip(variationDisplay.getTooltip());
        }
        if (variationDisplay.getIcon() == null) {
            combinedDisplay.setIcon(display.getIcon());
        } else if (display.getIcon() != null) {
            IconType icon = new IconType();
            if (StringUtils.isBlank(variationDisplay.getIcon().getCssClass())) {
                icon.setCssClass(display.getIcon().getCssClass());
            } else {
                icon.setCssClass(variationDisplay.getIcon().getCssClass());
            }
            if (StringUtils.isBlank(variationDisplay.getIcon().getColor())) {
                icon.setColor(display.getIcon().getColor());
            } else {
                icon.setColor(variationDisplay.getIcon().getColor());
            }
            if (StringUtils.isBlank(variationDisplay.getIcon().getImageUrl())) {
                icon.setImageUrl(display.getIcon().getImageUrl());
            } else {
                icon.setImageUrl(variationDisplay.getIcon().getImageUrl());
            }
            combinedDisplay.setIcon(icon);
        }

        return combinedDisplay;
    }

    /**
     * TODO Unify with WebComponentUtil.createPolyFromOrigString
     * @param key
     * @return
     */
    public static @NotNull PolyStringType createPolyStringType(String key) {
        return createPolyStringType(null, key);
    }

    private static @NotNull PolyStringType createPolyStringType(String orig, String key) {
        PolyStringTranslationType translation = new PolyStringTranslationType();
        translation.setKey(key);
        translation.setFallback(key);
        PolyString poly = new PolyString(orig, null, translation);

        return new PolyStringType(poly);
    }
}
