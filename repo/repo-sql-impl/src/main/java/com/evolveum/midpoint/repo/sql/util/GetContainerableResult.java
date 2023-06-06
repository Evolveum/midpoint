/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.transform.ResultTransformer;
import org.jetbrains.annotations.NotNull;

/**
 * @author lazyman
 */
public final class GetContainerableResult implements Serializable {

    public static final ResultStyle RESULT_STYLE = new ResultStyle() {
        @Override
        public ResultTransformer getResultTransformer() {
            return new ResultTransformer() {
                @Override
                public Object transformTuple(Object[] tuple, String[] aliases) {
                    return new GetContainerableResult((String) tuple[0], (byte[]) tuple[2]);
                }
            };
        }

        @Override
        public List<String> getIdentifiers(String rootAlias) {
            return Arrays.asList(rootAlias + ".ownerOid", rootAlias + ".id");
        }

        @Override
        public String getCountString(String basePath) {
            return "*";
        }

        @Override
        public List<String> getContentAttributes(String rootAlias) {
            return Collections.singletonList(rootAlias + ".fullObject");
        }
    };

    private final String ownerOid;
    private final byte[] fullObject;

    private GetContainerableResult(@NotNull String ownerOid, @NotNull byte[] fullObject) {
        this.fullObject = fullObject;
        this.ownerOid = ownerOid;
    }

    public byte[] getFullObject() {
        return fullObject;
    }

    public String getOwnerOid() {
        return ownerOid;
    }
}
