package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.transform.BasicTransformerAdapter;
import org.hibernate.transform.ResultTransformer;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ExtCountsResult implements Serializable {

    public static final String EXT_COUNT_PROJECTION = "stringsCount, longsCount, datesCount, referencesCount, clobsCount, polysCount";

    public static final ResultTransformer RESULT_TRANSFORMER = new BasicTransformerAdapter() {

        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
            return new ExtCountsResult(tuple);
        }
    };

    private Short stringsCount;
    private Short longsCount;
    private Short datesCount;
    private Short referencesCount;
    private Short clobsCount;
    private Short polysCount;

    public ExtCountsResult(Object[] values) {
        this((Short) values[0], (Short) values[1], (Short) values[2], (Short) values[3],
                (Short) values[4], (Short) values[5]);
    }

    public ExtCountsResult(Short stringsCount, Short longsCount, Short datesCount, Short referencesCount,
                           Short clobsCount, Short polysCount) {

        this.stringsCount = stringsCount;
        this.longsCount = longsCount;
        this.datesCount = datesCount;
        this.referencesCount = referencesCount;
        this.clobsCount = clobsCount;
        this.polysCount = polysCount;
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

    public Short getClobsCount() {
        if (clobsCount == null) {
            clobsCount = 0;
        }
        return clobsCount;
    }

    public Short getPolysCount() {
        if (polysCount == null) {
            polysCount = 0;
        }
        return polysCount;
    }
}
