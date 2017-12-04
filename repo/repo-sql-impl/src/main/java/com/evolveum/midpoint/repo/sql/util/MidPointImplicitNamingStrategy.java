/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.hibernate.boot.model.naming.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPointImplicitNamingStrategy extends ImplicitNamingStrategyLegacyHbmImpl {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointImplicitNamingStrategy.class);

    private static final int MAX_LENGTH = 30;


    @Override
    public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
        return super.determineJoinColumnName(source);
    }

    @Override
    public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
        Identifier i = super.determineJoinTableName(source);
        LOGGER.trace("determineJoinTableName {} {}", source, i);
        return i;
    }

    @Override
    public Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source) {
        String columnName = source.getAttributePath().getProperty();
        String fullPath = source.getAttributePath().getFullPath();

        String result;
        if (fullPath.startsWith("credentials.") || fullPath.startsWith("activation.")) {
            //credentials and activation are embedded and doesn't need to be qualified

            Identifier i = super.determineBasicColumnName(source);
            LOGGER.trace("determineBasicColumnName {} {}", fullPath, i);
            return i;
        } else {
            result = fullPath.replaceAll("\\.", "_");
        }
        result = fixLength(result);

        Identifier i = toIdentifier(result, source.getBuildingContext());
        LOGGER.trace("determineBasicColumnName {} {}", fullPath, i);
        return i;
    }

    private String fixLength(String input) {
        if (input == null || input.length() <= MAX_LENGTH) {
            return input;
        }

        String result = input;
        String[] array = input.split("_");
        for (int i = 0; i < array.length; i++) {
            int length = array[i].length();
            String lengthStr = Integer.toString(length);

            if (length < lengthStr.length()) {
                continue;
            }

            array[i] = array[i].charAt(0) + lengthStr;

            result = StringUtils.join(array, "_");
            if (result.length() < MAX_LENGTH) {
                break;
            }
        }

        return result;
    }
}
