package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaMatchResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.getExtensionItemRealValue;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.setExtensionPropertyRealValues;

public class ShadowObjectTypeStatisticsTypeUtil {

    public static ShadowObjectClassStatisticsType getObjectTypeStatisticsRequired(GenericObjectType holder) {
        return getObjectTypeStatisticsRequired(holder.asPrismObject());
    }


    public static ShadowObjectClassStatisticsType getObjectTypeStatisticsRequired(PrismObject<GenericObjectType> holder) {
        return MiscUtil.argNonNull(
                getExtensionItemRealValue(holder.asObjectable().getExtension(), SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE_STATISTICS),
                "No statistics in %s", holder);
    }

    public static GenericObjectType createObjectTypeStatisticsObject(
            String resourceOid, String resourceName, String kind, String intent, ShadowObjectClassStatisticsType statistics)
            throws SchemaException {
        var object = new GenericObjectType()
                .name("Statistics for %s:%s,%s (%s)".formatted(
                        resourceName, kind, intent, statistics.getTimestamp()));
        var holderPcv = object.asPrismContainerValue();
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_RESOURCE_OID, resourceOid);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_KIND_NAME, kind);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_INTENT_NAME, intent);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_OBJECT_TYPE_STATISTICS, statistics);
        return object;
    }

    public static SchemaMatchResultType getObjectTypeSchemaMatchRequired(GenericObjectType holder) {
        return getObjectTypeSchemaMatchRequired(holder.asPrismObject());
    }


    public static SchemaMatchResultType getObjectTypeSchemaMatchRequired(PrismObject<GenericObjectType> holder) {
        return MiscUtil.argNonNull(
                getExtensionItemRealValue(holder.asObjectable().getExtension(), SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE_SCHEMA_MATCH),
                "No schema match in %s", holder);
    }

    public static GenericObjectType createObjectTypeSchemaMatchObject(
            String resourceOid, String kind, String intent, SchemaMatchResultType schemaMatch)
            throws SchemaException {
        var object = new GenericObjectType()
                .name("Schema Match for %s:%s,%s (%s)".formatted(
                        resourceOid, kind, intent, schemaMatch.getTimestamp()));
        var holderPcv = object.asPrismContainerValue();
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_RESOURCE_OID, resourceOid);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_KIND_NAME, kind);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_INTENT_NAME, intent);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_OBJECT_TYPE_SCHEMA_MATCH, schemaMatch);
        return object;
    }

}
