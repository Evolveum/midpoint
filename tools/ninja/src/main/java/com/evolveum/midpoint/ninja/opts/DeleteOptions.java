package com.evolveum.midpoint.ninja.opts;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.FileReferenceConverter;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages", commandDescription = "delete")
public class DeleteOptions {

    public static final String P_OID = "-o";
    public static final String P_OID_LONG = "--oid";

    public static final String P_RAW = "-r";
    public static final String P_RAW_LONG = "--raw";

    public static final String P_FORCE = "-F";
    public static final String P_FORCE_LONG = "--force";

    public static final String P_ASK = "-a";
    public static final String P_ASK_LONG = "--ask";

    public static final String P_TYPE = "-t";
    public static final String P_TYPE_LONG = "--type";

    public static final String P_FILTER = "-f";
    public static final String P_FILTER_LONG = "--filter";

    @Parameter(names = {P_OID, P_OID_LONG}, descriptionKey = "delete.oid")
    private String oid;

    @Parameter(names = {P_RAW, P_RAW_LONG}, descriptionKey = "delete.raw")
    private boolean raw;

    @Parameter(names = {P_FORCE, P_FORCE_LONG}, descriptionKey = "delete.force")
    private boolean force;

    @Parameter(names = {P_ASK, P_ASK_LONG}, descriptionKey = "delete.ask")
    private boolean ask;

    @Parameter(names = {P_TYPE, P_TYPE_LONG}, descriptionKey = "delete.type")
    private ObjectTypes type;

    @Parameter(names = {P_FILTER, P_FILTER_LONG}, descriptionKey = "delete.filter",
            converter = FileReferenceConverter.class, validateWith = FileReferenceConverter.class)
    private FileReference filter;

    public String getOid() {
        return oid;
    }

    public boolean isRaw() {
        return raw;
    }

    public boolean isForce() {
        return force;
    }

    public boolean isAsk() {
        return ask;
    }

    public ObjectTypes getType() {
        return type;
    }

    public FileReference getFilter() {
        return filter;
    }
}
