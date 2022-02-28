/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

/**
 * TODO finish!
 */
public class CorrelationItemTarget {

    /**
     * The complete path related to the target object.
     */
    @NotNull private final ItemPath path;

    private CorrelationItemTarget(@NotNull ItemPath path) {
        this.path = path;
    }

    static CorrelationItemTarget createPrimary(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelationContext correlationContext) throws ConfigurationException {
        ItemPath path = getPrimaryTargetPath(itemBean);
        configCheck(!path.startsWithVariable(), "Variables are not supported in target paths: %s", path);
        return new CorrelationItemTarget(path);
    }

    private static ItemPath getPrimaryTargetPath(ItemCorrelationType itemBean) {
        if (itemBean.getPrimaryTargetPath() != null) {
            return itemBean.getPrimaryTargetPath().getItemPath();
        } else if (itemBean.getPath() != null) {
            return itemBean.getPath().getItemPath();
        } else {
            // TODO implement "ref" processing
            throw new UnsupportedOperationException("'ref' is not yet supported");
        }
    }

    public @NotNull ItemPath getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "CorrelationItemTarget{" +
                "path=" + path +
                '}';
    }
}
