/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.transform.BasicTransformerAdapter;
import org.hibernate.transform.ResultTransformer;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 * @author mederly
 */
public final class GetContainerableIdOnlyResult implements Serializable {

    public static final ResultStyle RESULT_STYLE = new ResultStyle() {
        @Override
        public ResultTransformer getResultTransformer() {
            return new BasicTransformerAdapter() {
                @Override
                public Object transformTuple(Object[] tuple, String[] aliases) {
                    return new GetContainerableIdOnlyResult((String) tuple[0], (Integer) tuple[1]);
                }
            };
        }

        @Override
        public List<String> getIdentifiers(String rootAlias) {
            return Arrays.asList(rootAlias + ".ownerOid", rootAlias + ".id");
        }

        @Override
        public String getCountString(String basePath) {
            return "*";     // TODO ok?
        }

        @Override
        public List<String> getContentAttributes(String rootAlias) {
            return Collections.emptyList();
        }
    };

    private final String ownerOid;
    private final Integer id;

    private GetContainerableIdOnlyResult(@NotNull String ownerOid, @NotNull Integer id) {
        this.ownerOid = ownerOid;
        this.id = id;
    }

    public String getOwnerOid() {
        return ownerOid;
    }

    public Integer getId() {
        return id;
    }
}
