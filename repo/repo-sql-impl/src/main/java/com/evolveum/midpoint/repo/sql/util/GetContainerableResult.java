package com.evolveum.midpoint.repo.sql.util;

import org.apache.commons.lang.Validate;
import org.hibernate.transform.BasicTransformerAdapter;
import org.hibernate.transform.ResultTransformer;

import java.io.Serializable;

/**
 * @author lazyman
 * @author mederly
 */
public class GetContainerableResult implements Serializable {

    public static final ResultTransformer RESULT_TRANSFORMER = new BasicTransformerAdapter() {

        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
            return new GetContainerableResult(tuple);
        }
    };

    private byte[] fullObject;
    private String ownerOid;

    public GetContainerableResult(Object[] values) {
        this((byte[]) values[0], (String) values[1]);
    }

    public GetContainerableResult(byte[] fullObject, String ownerOid) {

        Validate.notNull(fullObject, "Full object xml must not be null.");

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
