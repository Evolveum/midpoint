/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.gui.api.page.PageAdminLTE.createStringResourceStatic;

/**
 * Util class for creating different list types, e.g. list of assignable object,
 * list of searchable objects, list of object types, etc.
 */
public class ObjectTypeListUtil {

    //TODO what about reference search?
    public static List<QName> createSearchableTypeList() {
        var supportedObjectTypeList = new ArrayList<>(ObjectTypeListUtil.createObjectTypeList());
        supportedObjectTypeList.add(AssignmentType.COMPLEX_TYPE);
        supportedObjectTypeList.add(CaseWorkItemType.COMPLEX_TYPE);
        supportedObjectTypeList.add(AccessCertificationCaseType.COMPLEX_TYPE);
        supportedObjectTypeList.add(AccessCertificationWorkItemType.COMPLEX_TYPE);
        supportedObjectTypeList.add(OperationExecutionType.COMPLEX_TYPE);
        supportedObjectTypeList.add(SimulationResultProcessedObjectType.COMPLEX_TYPE);
        return supportedObjectTypeList;
    }

    // TODO: move to schema component
    public static List<QName> createObjectTypeList() {
        return createObjectTypesList().stream().map(ObjectTypes::getTypeQName).collect(Collectors.toList());

    }

    public static List<ObjectTypes> createObjectTypesList() {
        List<ObjectTypes> types = Arrays.asList(ObjectTypes.values());

        return types.stream().sorted((type1, type2) -> {
            Validate.notNull(type1);
            Validate.notNull(type2);

            ObjectTypeGuiDescriptor decs1 = ObjectTypeGuiDescriptor.getDescriptor(type1);
            ObjectTypeGuiDescriptor desc2 = ObjectTypeGuiDescriptor.getDescriptor(type2);

            String localizedType1 = translate(decs1);
            String localizedType2 = translate(desc2);

            Collator collator = Collator.getInstance(WebComponentUtil.getCurrentLocale());
            collator.setStrength(Collator.PRIMARY);

            return collator.compare(localizedType1, localizedType2);

        }).collect(Collectors.toList());
    }

    private static @NotNull String translate(ObjectTypeGuiDescriptor descriptor) {
        MidPointApplication app = MidPointApplication.get();
        String translatedValue = app.getLocalizationService().translate(descriptor.getLocalizationKey(), null, WebComponentUtil.getCurrentLocale());
        return translatedValue != null ? translatedValue : descriptor.getLocalizationKey();
    }

    public static List<QName> createContainerableTypesQnameList() {
        List<QName> qnameList = createObjectTypeList();
        //todo create enum for containerable types?
        qnameList.add(AuditEventRecordType.COMPLEX_TYPE);
        qnameList.add(AccessCertificationCaseType.COMPLEX_TYPE);
        qnameList.add(CaseWorkItemType.COMPLEX_TYPE);
        return qnameList.stream().sorted((type1, type2) -> {
            Validate.notNull(type1);
            Validate.notNull(type2);

            String key1 = "ObjectType." + type1.getLocalPart();
            String localizedType1 = createStringResourceStatic(key1).getString();
            if (StringUtils.isEmpty(localizedType1) || localizedType1.equals(key1)) {
                localizedType1 = type1.getLocalPart();
            }
            String key2 = "ObjectType." + type2.getLocalPart();
            String localizedType2 = createStringResourceStatic(key2).getString();
            if (StringUtils.isEmpty(localizedType2) || localizedType1.equals(key2)) {
                localizedType2 = type2.getLocalPart();
            }

            Collator collator = Collator.getInstance(WebComponentUtil.getCurrentLocale());
            collator.setStrength(Collator.PRIMARY);

            return collator.compare(localizedType1, localizedType2);

        }).collect(Collectors.toList());
    }

    public static List<QName> createAssignmentHolderTypeQnamesList() {
        List<ObjectTypes> objectTypes = createAssignmentHolderTypesList();
        return objectTypes.stream().map(ObjectTypes::getTypeQName).collect(Collectors.toList());
    }

    public static List<ObjectTypes> createAssignmentHolderTypesList() {
        return createObjectTypesList().stream().filter(type -> AssignmentHolderType.class.isAssignableFrom(type.getClassDefinition())).collect(Collectors.toList());
    }

    // TODO: move to schema component
    public static List<QName> createFocusTypeList() {
        return createFocusTypeList(false);
    }

    public static List<QName> createFocusTypeList(boolean includeAbstractType) {
        List<QName> focusTypeList = new ArrayList<>();

        focusTypeList.add(UserType.COMPLEX_TYPE);
        focusTypeList.add(OrgType.COMPLEX_TYPE);
        focusTypeList.add(RoleType.COMPLEX_TYPE);
        focusTypeList.add(ServiceType.COMPLEX_TYPE);

        if (includeAbstractType) {
            focusTypeList.add(FocusType.COMPLEX_TYPE);
        }

        return focusTypeList;
    }

    // TODO: move to schema component
    public static List<QName> createAbstractRoleTypeList() {
        List<QName> focusTypeList = new ArrayList<>();

        focusTypeList.add(AbstractRoleType.COMPLEX_TYPE);
        focusTypeList.add(OrgType.COMPLEX_TYPE);
        focusTypeList.add(RoleType.COMPLEX_TYPE);
        focusTypeList.add(ServiceType.COMPLEX_TYPE);

        return focusTypeList;
    }

    public static List<ObjectTypes> createAssignableTypesList() {
        List<ObjectTypes> focusTypeList = new ArrayList<>();

        focusTypeList.add(ObjectTypes.RESOURCE);
        focusTypeList.add(ObjectTypes.ORG);
        focusTypeList.add(ObjectTypes.ROLE);
        focusTypeList.add(ObjectTypes.SERVICE);

        return focusTypeList;
    }
}
