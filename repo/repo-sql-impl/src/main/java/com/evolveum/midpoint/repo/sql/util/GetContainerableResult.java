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
public class GetContainerableResult implements Serializable {

    public static final ResultStyle RESULT_STYLE = new ResultStyle() {
        @Override
        public ResultTransformer getResultTransformer() {
            return new BasicTransformerAdapter() {
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
            return "*";     // TODO ok?
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
