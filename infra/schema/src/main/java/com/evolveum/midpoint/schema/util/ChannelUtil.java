/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;

public class ChannelUtil {

    public static String inflate(String channelUri) {
        if (channelUri == null) {
            return null;
        } else if (QNameUtil.isUnqualified(channelUri)) {
            return SchemaConstants.NS_CHANNEL_PLUS_HASH + channelUri;
        } else {
            return channelUri;
        }
    }

    public static String deflate(String channelUri) {
        if (channelUri == null) {
            return null;
        }
        if (channelUri.startsWith(SchemaConstants.NS_CHANNEL_PLUS_HASH)) {
            return channelUri.substring(SchemaConstants.NS_CHANNEL_PLUS_HASH.length());
        } else {
            return channelUri;
        }
    }
}
