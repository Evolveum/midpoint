package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.transform.BasicTransformerAdapter;
import org.hibernate.transform.ResultTransformer;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author lazyman
 * @author mederly
 */
public class GetContainerableResult implements Serializable {

    public static final ResultTransformer RESULT_TRANSFORMER = new BasicTransformerAdapter() {
        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
            return new GetContainerableResult(tuple);       // ownerOid, id, fullObject
        }
    };

    private final String ownerOid;
    private final byte[] fullObject;

    private GetContainerableResult(Object[] values) {
        this((String) values[0], (byte[]) values[2]);
    }

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
