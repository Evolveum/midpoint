/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.repo.sql.data.common.any.*;
import org.hibernate.transform.ResultTransformer;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
public class GetObjectResult implements Serializable {

    public static final Class[] EXT_COUNT_CLASSES = new Class[]{ROExtString.class, ROExtLong.class, ROExtDate.class,
            ROExtReference.class, ROExtPolyString.class, ROExtBoolean.class};

    public static final ResultStyle RESULT_STYLE = new ResultStyle() {

        @Override
        public ResultTransformer getResultTransformer() {
            return (tuple, aliases) -> new GetObjectResult((String) tuple[0], (byte[]) tuple[1]);
        }

        @Override
        public List<String> getIdentifiers(String rootAlias) {
            return Collections.singletonList(rootAlias + ".oid");
        }

        @Override
        public String getCountString(String rootAlias) {
            return rootAlias + ".oid";
        }

        @Override
        public List<String> getContentAttributes(String rootAlias) {
            return Collections.singletonList(rootAlias + ".fullObject");
        }
    };

    @NotNull private final String oid;
    @NotNull private final byte[] fullObject;

    public GetObjectResult(@NotNull String oid, @NotNull byte[] fullObject) {


        this.oid = oid;
        this.fullObject = fullObject;
    }

    @NotNull
    public String getOid() {
        return oid;
    }

    @NotNull
    public byte[] getFullObject() {
        return fullObject;
    }
}
