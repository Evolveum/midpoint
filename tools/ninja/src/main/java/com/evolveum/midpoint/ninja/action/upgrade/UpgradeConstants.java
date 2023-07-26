package com.evolveum.midpoint.ninja.action.upgrade;

public class UpgradeConstants {

    public static final String SUPPORTED_VERSION_LTS = "4.4.6";

    public static final String SUPPORTED_VERSION_FEATURE = "4.7.2";

    public static final String SUPPORTED_VERSION_TARGET = "4.8";

    public static final String[] SUPPORTED_VERSIONS = { SUPPORTED_VERSION_LTS, SUPPORTED_VERSION_FEATURE };

    public static final String UPGRADE_TEMP_DIRECTORY = ".upgrade";

    public static final String LABEL_SCHEMA_CHANGE_NUMBER = "schemaChangeNumber";

    public static final String LABEL_SCHEMA_AUDIT_CHANGE_NUMBER = "schemaAuditChangeNumber";

    public static final String SUPPORTED_SCHEMA_CHANGE_NUMBER = Integer.toString(1);

    public static final String SUPPORTED_SCHEMA_AUDIT_CHANGE_NUMBER = Integer.toString(1);
}
