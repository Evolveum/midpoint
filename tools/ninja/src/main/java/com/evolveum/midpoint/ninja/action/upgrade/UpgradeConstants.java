package com.evolveum.midpoint.ninja.action.upgrade;

public class UpgradeConstants {

    public static final String SUPPORTED_VERSION_LTS = "4.4.5";

    public static final String SUPPORTED_VERSION_FEATURE = "4.7.1";

    // todo, master doesn't have a version to upgrade to..., support branches do have  this one
    public static final String SUPPORTED_VERSION_TARGET = null;

    public static final String[] SUPPORTED_VERSIONS = { SUPPORTED_VERSION_LTS, SUPPORTED_VERSION_FEATURE };

    public static final String UPGRADE_TEMP_DIRECTORY = ".upgrade";

    public static final String LABEL_SCHEMA_CHANGE_NUMBER = "schemaChangeNumber";

    public static final String LABEL_SCHEMA_AUDIT_CHANGE_NUMBER = "schemaAuditChangeNumber";

    public static final String SUPPORTED_SCHEMA_CHANGE_NUMBER = Integer.toString(15);

    public static final String SUPPORTED_SCHEMA_AUDIT_CHANGE_NUMBER = Integer.toString(4);
}
