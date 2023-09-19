/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
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
                displayType.setLabel(new PolyStringType(typeValue + " " + relationValue));

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

    public static DisplayType getNewObjectDisplayTypeFromCollectionView(CompiledObjectCollectionView view, PageBase pageBase) {
        DisplayType displayType = view != null ? view.getDisplay() : null;
        if (displayType == null) {
            displayType = createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green", "");
        }

        if (displayType.getIcon() == null || displayType.getIcon().getCssClass() == null){
            MiscSchemaUtil.mergeDisplay(displayType, createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green", ""));
        }

        if (PolyStringUtils.isEmpty(displayType.getTooltip()) && !PolyStringUtils.isEmpty(displayType.getLabel())) {
            String sb = pageBase.createStringResource("MainObjectListPanel.newObject").getString()
                    + " "
                    + displayType.getLabel().getOrig().toLowerCase();
            displayType.setTooltip(WebComponentUtil.createPolyFromOrigString(sb));
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
        return displayType.getIcon().getColor();
    }

    public static String getHelp(DisplayType displayType) {
        if (displayType == null || displayType.getHelp() == null) {
            return "";
        }
        return LocalizationUtil.translatePolyString(displayType.getHelp());
    }

    public static String getDisplayTypeTitle(DisplayType displayType) {
        if (displayType == null || displayType.getTooltip() == null) {
            return "";
        }
        return displayType.getTooltip().getOrig();
    }

    public static boolean existsIconDisplay(CompiledObjectCollectionView view) {
        if (view == null){
            return false;
        }
        return existsIconDisplay(view.getDisplay());
    }

    private static boolean existsIconDisplay(DisplayType display) {
        if (display == null){
            return false;
        }
        if (display.getIcon() == null){
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

    public static DisplayType createDisplayTypeWith(String labelOrg, String labelKey, String helpKey) {
        DisplayType display = new DisplayType();

        PolyStringType label = new PolyStringType(labelOrg);
        PolyStringTranslationType translationLabel = new PolyStringTranslationType();
        translationLabel.setKey(labelKey);
        label.setTranslation(translationLabel);
        display.setLabel(label);

        PolyStringType help = new PolyStringType("");
        PolyStringTranslationType translationHelp = new PolyStringTranslationType();
        translationHelp.setKey(helpKey);
        help.setTranslation(translationHelp);
        display.setHelp(help);
        return display;
    }
}
