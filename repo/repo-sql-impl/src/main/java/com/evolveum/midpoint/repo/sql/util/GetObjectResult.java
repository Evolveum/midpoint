package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.repo.sql.data.common.any.*;
import org.hibernate.transform.BasicTransformerAdapter;
import org.hibernate.transform.ResultTransformer;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;
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
            return new BasicTransformerAdapter() {
                @Override
                public Object transformTuple(Object[] tuple, String[] aliases) {
                    return new GetObjectResult((String) tuple[0], (byte[]) tuple[1],
                            tuple.length > 2 ? (Short) tuple[2] : null,
                            tuple.length > 3 ? (Short) tuple[3] : null,
                            tuple.length > 4 ? (Short) tuple[4] : null,
                            tuple.length > 5 ? (Short) tuple[5] : null,
                            tuple.length > 6 ? (Short) tuple[6] : null,
                            tuple.length > 7 ? (Short) tuple[7] : null);
                }
            };
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
            return Arrays.asList(
                    rootAlias + ".fullObject",
                    rootAlias + ".stringsCount",
                    rootAlias + ".longsCount",
                    rootAlias + ".datesCount",
                    rootAlias + ".referencesCount",
                    rootAlias + ".polysCount",
                    rootAlias + ".booleansCount");
        }
    };

    @NotNull private final String oid;
    @NotNull private final byte[] fullObject;

    private Short stringsCount;
    private Short longsCount;
    private Short datesCount;
    private Short referencesCount;
    private Short polysCount;
    private Short booleansCount;

    public GetObjectResult(@NotNull String oid, @NotNull byte[] fullObject, Short stringsCount, Short longsCount, Short datesCount,
                           Short referencesCount, Short polysCount, Short booleansCount) {


        this.oid = oid;
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

    @NotNull
    public String getOid() {
        return oid;
    }

    @NotNull
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
