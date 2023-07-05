package com.evolveum.midpoint.ninja.action;

import java.util.HashSet;
import java.util.Set;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.FileReferenceConverter;
import com.evolveum.midpoint.ninja.util.ObjectTypesConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

@Parameters(resourceBundle = "messages")
public class SearchOptions {

    public static final String P_RAW = "-r";
    public static final String P_RAW_LONG = "--raw";

    public static final String P_OID_LONG = "--oid";

    public static final String P_TYPE = "-t";
    public static final String P_TYPE_LONG = "--type";

    public static final String P_FILTER = "-f";
    public static final String P_FILTER_LONG = "--filter";

    @Parameter(names = { P_RAW, P_RAW_LONG }, descriptionKey = "search.raw")
    private boolean raw;

    @Parameter(names = { P_OID_LONG }, descriptionKey = "search.oid")
    private String oid;

    @Parameter(names = { P_TYPE, P_TYPE_LONG }, descriptionKey = "search.type",
            validateWith = ObjectTypesConverter.class, converter = ObjectTypesConverter.class)
    private Set<ObjectTypes> type = new HashSet<>();

    @Parameter(names = { P_FILTER, P_FILTER_LONG }, descriptionKey = "search.filter",
            converter = FileReferenceConverter.class, validateWith = FileReferenceConverter.class)
    private FileReference filter;

    public boolean isRaw() {
        return raw;
    }

    public void setRaw(boolean raw) {
        this.raw = raw;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public Set<ObjectTypes> getType() {
        return type;
    }

    public void setType(Set<ObjectTypes> type) {
        this.type = type;
    }

    public FileReference getFilter() {
        return filter;
    }

    public void setFilter(FileReference filter) {
        this.filter = filter;
    }
}
