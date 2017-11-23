package com.evolveum.midpoint.ninja.opts;

import com.beust.jcommander.Parameter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.FileReferenceConverter;
import com.evolveum.midpoint.ninja.util.ObjectTypesConverter;

/**
 * Created by Viliam Repan (lazyman).
 */
public class BaseImportExportOptions {

    public static final String P_RAW = "-r";
    public static final String P_RAW_LONG = "--raw";

    public static final String P_OID = "-o";
    public static final String P_OID_LONG = "--oid";

    public static final String P_TYPE = "-t";
    public static final String P_TYPE_LONG = "--type";

    public static final String P_FILTER = "-f";
    public static final String P_FILTER_LONG = "--filter";

    public static final String P_ZIP = "-z";
    public static final String P_ZIP_LONG = "--zip";

    @Parameter(names = {P_RAW, P_RAW_LONG}, descriptionKey = "baseImportExport.raw")
    private boolean raw;

    @Parameter(names = {P_OID, P_OID_LONG}, descriptionKey = "baseImportExport.oid")
    private String oid;

    @Parameter(names = {P_TYPE, P_TYPE_LONG}, descriptionKey = "baseImportExport.type",
            required = true, validateWith = ObjectTypesConverter.class, converter = ObjectTypesConverter.class)
    private ObjectTypes type;

    @Parameter(names = {P_FILTER, P_FILTER_LONG}, descriptionKey = "baseImportExport.filter",
            converter = FileReferenceConverter.class, validateWith = FileReferenceConverter.class)
    private FileReference filter;

    @Parameter(names = {P_ZIP, P_ZIP_LONG}, descriptionKey = "baseImportExport.zip")
    private boolean zip;

    public boolean isRaw() {
        return raw;
    }

    public ObjectTypes getType() {
        return type;
    }

    public FileReference getFilter() {
        return filter;
    }

    public boolean isZip() {
        return zip;
    }

    public String getOid() {
        return oid;
    }
}
