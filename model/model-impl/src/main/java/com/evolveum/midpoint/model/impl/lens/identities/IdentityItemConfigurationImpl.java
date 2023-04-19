/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.identities;

import com.evolveum.midpoint.model.api.identities.IdentityItemConfiguration;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ItemRefinedDefinitionTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdentityItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRefinedDefinitionType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;

import static com.evolveum.midpoint.schema.error.ConfigErrorReporter.describe;

public class IdentityItemConfigurationImpl implements Serializable, IdentityItemConfiguration {

    /** Beware, the name may be unqualified! */
    @NotNull private final QName name;

    /** Beware, the path segments may be unqualified! */
    @NotNull private final ItemPath path;

    private IdentityItemConfigurationImpl(@NotNull QName name, @NotNull ItemPath path) {
        this.name = name;
        this.path = path;
    }

    @NotNull public static IdentityItemConfigurationImpl of(
            @NotNull ItemRefinedDefinitionType itemDefBean,
            @NotNull IdentityItemDefinitionType identityDefBean) throws ConfigurationException {
        ItemPath path = ItemRefinedDefinitionTypeUtil.getRef(itemDefBean);
        QName explicitName = identityDefBean.getName();
        QName name = explicitName != null ? explicitName : deriveName(path, itemDefBean);
        return new IdentityItemConfigurationImpl(name, path);
    }

    private static @NotNull QName deriveName(ItemPath path, ItemRefinedDefinitionType itemDefBean)
            throws ConfigurationException {
        return MiscUtil.configNonNull(
                        path.lastName(),
                        () -> "No name in path '" + path + "' in " + describe(itemDefBean));
    }

    @Override
    public @NotNull QName getName() {
        return name;
    }

    @Override
    @SuppressWarnings("WeakerAccess")
    public @NotNull String getLocalName() {
        return name.getLocalPart();
    }

    @Override
    public @NotNull ItemName getDefaultSearchItemName() {
        return new ItemName(SchemaConstants.NS_NORMALIZED_DATA, getLocalName());
    }

    @Override
    public @NotNull ItemPath getPath() {
        return path;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "name='" + name + '\'' +
                ", path=" + path +
                '}';
    }
}
