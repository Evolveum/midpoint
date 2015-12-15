package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.repo.sql.data.common.any.*;
import org.apache.commons.lang.Validate;
import org.hibernate.transform.BasicTransformerAdapter;
import org.hibernate.transform.ResultTransformer;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class GetObjectResult implements Serializable {

    public static final Class[] EXT_COUNT_CLASSES = new Class[]{ROExtString.class, ROExtLong.class, ROExtDate.class,
            ROExtReference.class, ROExtPolyString.class, ROExtBoolean.class};

    public static final ResultTransformer RESULT_TRANSFORMER = new BasicTransformerAdapter() {

        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
            return new GetObjectResult(tuple);
        }
    };

    private byte[] fullObject;

    private Short stringsCount;
    private Short longsCount;
    private Short datesCount;
    private Short referencesCount;
    private Short polysCount;
    private Short booleansCount;

    public GetObjectResult(Object[] values) {
        this((byte[]) values[0],
                values.length > 1 ? (Short) values[1] : null,
                values.length > 2 ? (Short) values[2] : null,
                values.length > 3 ? (Short) values[3] : null,
                values.length > 4 ? (Short) values[4] : null,
                values.length > 5 ? (Short) values[5] : null,
                values.length > 6 ? (Short) values[6] : null);
    }

    public GetObjectResult(byte[] fullObject, Short stringsCount, Short longsCount, Short datesCount,
                           Short referencesCount, Short polysCount, Short booleansCount) {

        Validate.notNull(fullObject, "Full object xml must not be null.");

        this.fullObject = fullObject;

        this.stringsCount = stringsCount;
        this.longsCount = longsCount;
        this.datesCount = datesCount;
        this.referencesCount = referencesCount;
        this.polysCount = polysCount;
        this.booleansCount = booleansCount;
    }

    public Short[] getCountProjection() {
        return new Short[]{getStringsCount(), getLongsCount(), getDatesCount(),
                getReferencesCount(), getPolysCount(), getBooleansCount()};
    }

    public byte[] getFullObject() {
        return fullObject;
    }

    public Short getStringsCount() {
        if (stringsCount == null) {
            stringsCount = 0;
        }
        return stringsCount;
    }

    public Short getLongsCount() {
        if (longsCount == null) {
            longsCount = 0;
        }
        return longsCount;
    }

    public Short getDatesCount() {
        if (datesCount == null) {
            datesCount = 0;
        }
        return datesCount;
    }

    public Short getReferencesCount() {
        if (referencesCount == null) {
            referencesCount = 0;
        }
        return referencesCount;
    }

    public Short getPolysCount() {
        if (polysCount == null) {
            polysCount = 0;
        }
        return polysCount;
    }

    public Short getBooleansCount() {
        if (booleansCount == null) {
            booleansCount = 0;
        }
        return booleansCount;
    }
}
