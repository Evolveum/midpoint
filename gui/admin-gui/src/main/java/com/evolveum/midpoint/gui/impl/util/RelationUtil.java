/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.util;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RelationUtil {

    private static final Trace LOGGER = TraceManager.getTrace(RelationUtil.class);

    private static final String DEFAULT_RELATION_ICON = "fa-solid fa-user";


    /**
     * To be used only for tests when there's no MidpointApplication.
     * (Quite a hack. Replace eventually by a more serious solution.)
     */
    private static RelationRegistry staticallyProvidedRelationRegistry;


    public static QName normalizeRelation(QName relation) {
        return getRelationRegistry().normalizeRelation(relation);
    }

    // can this implementation be made more efficient? [pm]
    @SuppressWarnings("WeakerAccess")
    public static boolean isOfKind(QName relation, RelationKindType kind) {
        return getRelationRegistry().isOfKind(relation, kind);
    }

    public static RelationRegistry getRelationRegistry() {
        if (staticallyProvidedRelationRegistry != null) {
            return staticallyProvidedRelationRegistry;
        } else {
            return MidPointApplication.get().getRelationRegistry();
        }
    }

    public static boolean isManagerRelation(QName relation) {
        return isOfKind(relation, RelationKindType.MANAGER);
    }

    public static boolean isDefaultRelation(QName relation) {
        return getRelationRegistry().isDefault(relation);
    }

    public static String getRelationLabelValue(PrismContainerValueWrapper<AssignmentType> assignmentWrapper, PageBase pageBase) {
        QName relation;
        try {
            relation = getRelation(assignmentWrapper);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Problem while getting relation for {}", e, assignmentWrapper.getRealValue());
            return null;
        }

        String relationDisplayName = getRelationHeaderLabelKeyIfKnown(relation);
        return StringUtils.isNotEmpty(relationDisplayName) ?
                pageBase.createStringResource(relationDisplayName).getString() :
                pageBase.createStringResource(relation.getLocalPart()).getString();
    }

    public static String getRelationLabelValue(AssignmentType assignment, PageBase pageBase) {
        String relationDisplayName;
        QName relation;
        if (assignment == null || assignment.getTargetRef() == null) {
            return null;
        }
        relation = assignment.getTargetRef().getRelation();
        relationDisplayName = getRelationHeaderLabelKeyIfKnown(relation);
        return StringUtils.isNotEmpty(relationDisplayName) ?
                pageBase.createStringResource(relationDisplayName).getString() :
                pageBase.createStringResource(relation.getLocalPart()).getString();
    }

    public static String getRelationLabelValue(PrismReferenceValue referenceValue, PageBase pageBase) {
        if (referenceValue == null) {
            return "";
        }
        QName relation = referenceValue.getRelation();
        String relationDisplayName = getRelationHeaderLabelKeyIfKnown(relation);
        return StringUtils.isNotEmpty(relationDisplayName) ?
                pageBase.createStringResource(relationDisplayName).getString() :
                pageBase.createStringResource(relation.getLocalPart()).getString();
    }

    private static QName getRelation(PrismContainerValueWrapper<AssignmentType> assignmentWrapper) throws SchemaException {
        if (assignmentWrapper == null) {
            return null;
        }

        PrismReferenceWrapper<ObjectReferenceType> targetRef = assignmentWrapper.findReference(AssignmentType.F_TARGET_REF);
        if (targetRef == null) {
            return null;
        }

        PrismReferenceValueWrapperImpl<ObjectReferenceType> refValue = targetRef.getValue();
        if (refValue == null) {
            return null;
        }

        Referencable ref = refValue.getRealValue();
        if (ref == null) {
            return null;
        }
        return ref.getRelation();
    }

    @SuppressWarnings("WeakerAccess")
    public static QName getDefaultRelation() {
        return getRelationRegistry().getDefaultRelation();
    }

    @NotNull
    public static QName getDefaultRelationOrFail() {
        QName relation = getDefaultRelation();
        if (relation != null) {
            return relation;
        } else {
            throw new IllegalStateException("No default relation is defined");
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    public static QName getDefaultRelationFor(RelationKindType kind) {
        return getRelationRegistry().getDefaultRelationFor(kind);
    }

    @NotNull
    public static QName getDefaultRelationOrFail(RelationKindType kind) {
        QName relation = getDefaultRelationFor(kind);
        if (relation != null) {
            return relation;
        } else {
            throw new IllegalStateException("No default relation for kind " + kind);
        }
    }

    @NotNull
    public static String getRelationHeaderLabelKey(QName relation) {
        String label = getRelationHeaderLabelKeyIfKnown(relation);
        if (label != null) {
            return label;
        } else {
            return relation != null ? relation.getLocalPart() : "default";
        }
    }

    @Nullable
    public static String getRelationHeaderLabelKeyIfKnown(QName relation) {
        RelationDefinitionType definition = getRelationRegistry().getRelationDefinition(relation);

        PolyStringType label = getRelationLabel(definition);
        if (label == null) {
            return null;
        }

        PolyStringTranslationType translation = label.getTranslation();
        if (translation == null) {
            return label.getOrig();
        }

        return translation.getKey();

    }

    @Nullable
    private static PolyStringType getRelationLabel(RelationDefinitionType definition) {
        if (definition == null) {
            return null;
        }

        DisplayType displayType = definition.getDisplay();
        if (displayType == null) {
            return null;
        }

        return displayType.getLabel();
    }

    public static RelationDefinitionType getRelationDefinition(QName relation, List<RelationDefinitionType> relations) {
        if (relations == null) {
            return null;
        }

        for (RelationDefinitionType rel : relations) {
            if (relation.equals(rel.getRef())) {
                return rel;
            }
        }

        return null;
    }

    public static String getRelationIcon(QName relation, List<RelationDefinitionType> relations) {
        final String defaultIcon = getDefaultRelationIcon(relation);

        RelationDefinitionType rel = getRelationDefinition(relation, relations);
        if (rel == null || rel.getDisplay() == null) {
            return defaultIcon;
        }

        DisplayType display = rel.getDisplay();

        IconType it = display.getIcon();
        if (it == null || it.getCssClass() == null) {
            return defaultIcon;
        }

        return it.getCssClass();
    }

    public static PolyString getRelationLabel(QName relation, List<RelationDefinitionType> relations) {
        final PolyString defaultLabel = new PolyString(relation.getLocalPart());

        RelationDefinitionType rel = getRelationDefinition(relation, relations);
        if (rel == null) {
            return defaultLabel;
        }

        DisplayType display = rel.getDisplay();
        if (display == null || display.getLabel() == null) {
            return defaultLabel;
        }

        return display.getLabel().toPolyString();
    }

    public static String getDefaultRelationIcon(QName name) {
        if (SchemaConstants.ORG_DEFAULT.equals(name)) {
            return "fa-solid fa-user";
        } else if (SchemaConstants.ORG_MANAGER.equals(name)) {
            return "fa-solid fa-user-tie";
        } else if (SchemaConstants.ORG_APPROVER.equals(name)) {
            return "fa-solid fa-clipboard-check";
        } else if (SchemaConstants.ORG_OWNER.equals(name)) {
            return "fa-solid fa-crown";
        }

        return DEFAULT_RELATION_ICON;
    }

    public static List<QName> getCategoryRelationChoices(AreaCategoryType category, List<RelationDefinitionType> defList) {
        List<QName> relationsList = new ArrayList<>();
        defList.sort((rD1, rD2) -> {
            if (rD1 == null || rD2 == null) {
                return 0;
            }
            RelationKindType rK1 = rD1.getDefaultFor() != null ? rD1.getDefaultFor() : getHighestRelationKind(rD1.getKind());
            RelationKindType rK2 = rD2.getDefaultFor() != null ? rD2.getDefaultFor() : getHighestRelationKind(rD2.getKind());
            int int1 = rK1 != null ? rK1.ordinal() : 100;
            int int2 = rK2 != null ? rK2.ordinal() : 100;
            int compare = Integer.compare(int1, int2);
            if (compare == 0) {
                if (rD1.getDisplay() == null || rD1.getDisplay().getLabel() == null
                        || rD2.getDisplay() == null || rD2.getDisplay().getLabel() == null) {
                    return compare;
                }
                String display1 = LocalizationUtil.translatePolyString(rD1.getDisplay().getLabel());
                String display2 = LocalizationUtil.translatePolyString(rD2.getDisplay().getLabel());
                return String.CASE_INSENSITIVE_ORDER.compare(display1, display2);
            }
            return compare;
        });
        defList.forEach(def -> {
            if (def.getCategory() != null && def.getCategory().contains(category)) {
                relationsList.add(def.getRef());
            }
        });
        return relationsList;
    }

    public static List<QName> getCategoryRelationChoices(AreaCategoryType category, ModelServiceLocator pageBase) {
        return getCategoryRelationChoices(category, getRelationDefinitions(pageBase));
    }

    private static RelationKindType getHighestRelationKind(List<RelationKindType> kinds) {
        RelationKindType ret = null;
        for (RelationKindType kind : kinds) {
            if (ret == null || ret.ordinal() < kind.ordinal()) {
                ret = kind;
            }
        }
        return ret;
    }

    public static List<QName> getAllRelations(ModelServiceLocator pageBase) {
        List<RelationDefinitionType> allRelationDefinitions = getRelationDefinitions(pageBase);
        List<QName> allRelationsQName = new ArrayList<>(allRelationDefinitions.size());
        allRelationDefinitions.forEach(relation -> allRelationsQName.add(relation.getRef()));
        return allRelationsQName;
    }

    @NotNull
    public static List<RelationDefinitionType> getRelationDefinitions(ModelServiceLocator pageBase) {
        return pageBase.getModelInteractionService().getRelationDefinitions();
    }

    public static RelationDefinitionType getRelationDefinition(QName relation) {
        return getRelationRegistry().getRelationDefinition(relation);
    }

    @SuppressWarnings("unused")
    public static RelationRegistry getStaticallyProvidedRelationRegistry() {
        return staticallyProvidedRelationRegistry;
    }

    public static IChoiceRenderer<QName> getRelationChoicesRenderer() {
        return new IChoiceRenderer<>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public QName getObject(String id, IModel<? extends List<? extends QName>> choices) {
                if (StringUtils.isBlank(id)) {
                    return null;
                }
                return choices.getObject().get(Integer.parseInt(id));
            }

            @Override
            public Object getDisplayValue(QName object) {
                RelationDefinitionType def = getRelationDefinition(object);
                if (def != null) {
                    String translatedLabel = GuiDisplayTypeUtil.getTranslatedLabel(def.getDisplay());
                    if (StringUtils.isNotEmpty(translatedLabel)) {
                        return translatedLabel;
                    }
                }
                return object.getLocalPart();
            }
        };
    }
}
