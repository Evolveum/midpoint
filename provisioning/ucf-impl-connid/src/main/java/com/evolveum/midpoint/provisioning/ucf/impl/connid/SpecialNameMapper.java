/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.Uid;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Maps between special ConnId attribute names like `__UID__` and their midPoint representation like `icfs:uid`.
 */
class SpecialItemNameMapper {

    /** Maps ConnId names to their standard midPoint representation, like `__UID__` to `icfs:uid`, and so on.. */
    private static final Map<String, ItemName> SPECIAL_ATTRIBUTE_MAP_ICF = new HashMap<>();

    /** Inversion of {@link #SPECIAL_ATTRIBUTE_MAP_ICF}. */
    private static final Map<QName, String> SPECIAL_ATTRIBUTE_MAP_MP = new HashMap<>();

    private static void initialize() {
        addSpecialAttributeMapping(Name.NAME, SchemaConstants.ICFS_NAME);
        addSpecialAttributeMapping(Uid.NAME, SchemaConstants.ICFS_UID);

        addOperationalAttributeMapping(OperationalAttributeInfos.CURRENT_PASSWORD);
        addOperationalAttributeMapping(OperationalAttributeInfos.DISABLE_DATE);
        addOperationalAttributeMapping(OperationalAttributeInfos.ENABLE);
        addOperationalAttributeMapping(OperationalAttributeInfos.ENABLE_DATE);
        addOperationalAttributeMapping(OperationalAttributeInfos.LOCK_OUT);
        addOperationalAttributeMapping(OperationalAttributeInfos.PASSWORD);
        addOperationalAttributeMapping(OperationalAttributeInfos.PASSWORD_EXPIRATION_DATE);
        addOperationalAttributeMapping(OperationalAttributeInfos.PASSWORD_EXPIRED);

        addOperationalAttributeMapping(SecretIcfOperationalAttributes.DESCRIPTION);
        addOperationalAttributeMapping(SecretIcfOperationalAttributes.GROUPS);
        addOperationalAttributeMapping(SecretIcfOperationalAttributes.LAST_LOGIN_DATE);
    }

    private static void addSpecialAttributeMapping(String icfName, ItemName qname) {
        SPECIAL_ATTRIBUTE_MAP_ICF.put(icfName, qname);
        SPECIAL_ATTRIBUTE_MAP_MP.put(qname, icfName);
    }

    private static void addOperationalAttributeMapping(SecretIcfOperationalAttributes opAttr) {
        addOperationalAttributeMapping(opAttr.getName());
    }

    private static void addOperationalAttributeMapping(AttributeInfo attrInfo) {
        addOperationalAttributeMapping(attrInfo.getName());
    }

    private static void addOperationalAttributeMapping(String icfName) {
        addSpecialAttributeMapping(icfName, convertUnderscoreAttributeNameToQName(icfName));
    }

    /** Converts e.g. __ENABLE_DATE__ to icfs:enableDate, using a simple algorithm. */
    private static ItemName convertUnderscoreAttributeNameToQName(String icfAttrName) {

        argCheck(icfAttrName.startsWith("__") && icfAttrName.endsWith("__"),
                "Not a ConnId special name: %s", icfAttrName);

        // Strip leading and trailing underscores
        String inside = icfAttrName.substring(2, icfAttrName.length() - 2);

        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;
        while (true) {
            int nextIndex = inside.indexOf("_", lastIndex);
            if (nextIndex < 0) {
                String upcase = inside.substring(lastIndex);
                sb.append(toCamelCase(upcase, lastIndex == 0));
                break;
            }
            String upcase = inside.substring(lastIndex, nextIndex);
            sb.append(toCamelCase(upcase, lastIndex == 0));
            lastIndex = nextIndex + 1;
        }

        return new ItemName(SchemaConstants.NS_ICF_SCHEMA, sb.toString());
    }

    private static String toCamelCase(String upcase, boolean lowCase) {
        if (lowCase) {
            return StringUtils.lowerCase(upcase);
        } else {
            return StringUtils.capitalize(StringUtils.lowerCase(upcase));
        }
    }

    static {
        initialize();
    }

    static boolean isConnIdNameSpecial(String connIdAttrName) {
        return SPECIAL_ATTRIBUTE_MAP_ICF.containsKey(connIdAttrName);
    }

    static ItemName toUcfFormat(String connIdAttrName) {
        return SPECIAL_ATTRIBUTE_MAP_ICF.get(connIdAttrName);
    }

    static boolean isUcfNameSpecial(QName ucfAttrName) {
        return SPECIAL_ATTRIBUTE_MAP_MP.containsKey(ucfAttrName);
    }

    static String toConnIdFormat(QName ucfAttrName) {
        return SPECIAL_ATTRIBUTE_MAP_MP.get(ucfAttrName);
    }
}
