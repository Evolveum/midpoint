/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.identities;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdentityItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRefinedDefinitionType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;

public class IdentityItemConfiguration implements Serializable {

    /** Beware, the name may be unqualified! */
    @NotNull private final QName name;

    /** Beware, the path segments may be unqualified! */
    @NotNull private final ItemPath path;

    private IdentityItemConfiguration(@NotNull QName name, @NotNull ItemPath path) {
        this.name = name;
        this.path = path;
    }

    @NotNull public static IdentityItemConfiguration of(
            @NotNull ItemRefinedDefinitionType itemDefBean,
            @NotNull IdentityItemDefinitionType identityDefBean) throws ConfigurationException {
        ItemPath path = MiscUtil.configNonNull(
                        itemDefBean.getRef(),
                        () -> "No 'ref' in " + itemDefBean)
                .getItemPath();
        QName explicitName = identityDefBean.getName();
        QName name = explicitName != null ? explicitName : deriveName(path, itemDefBean);
        return new IdentityItemConfiguration(name, path);
    }

    private static @NotNull QName deriveName(ItemPath path, ItemRefinedDefinitionType itemDefBean)
            throws ConfigurationException {
        return MiscUtil.configNonNull(
                        path.lastName(),
                        () -> "No name in path '" + path + "' in " + itemDefBean);
    }

    public @NotNull QName getName() {
        return name;
    }

    @SuppressWarnings("WeakerAccess")
    public @NotNull String getLocalName() {
        return name.getLocalPart();
    }

    public @NotNull ItemName getDefaultSearchItemName() {
        return new ItemName(SchemaConstants.NS_IDENTITY, getLocalName());
    }

    public @NotNull ItemPath getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "IdentityItemConfiguration{" +
                "name='" + name + '\'' +
                ", path=" + path +
                '}';
    }
}
