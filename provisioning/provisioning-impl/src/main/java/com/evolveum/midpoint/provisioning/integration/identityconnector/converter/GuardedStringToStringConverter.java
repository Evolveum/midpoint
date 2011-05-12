/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.integration.identityconnector.converter;

import com.evolveum.midpoint.annotations.CustomValueConverter;
import org.springframework.core.convert.converter.Converter;
import org.identityconnectors.common.security.GuardedString;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
@CustomValueConverter
public class GuardedStringToStringConverter implements Converter<GuardedString, String> {

    public static final String code_id = "$Id$";

    @Override
    public String convert(GuardedString value) {
        String result = null;
        final String[] clearText = new String[1];

        GuardedString.Accessor accessor = new GuardedString.Accessor() {

            @Override
            public void access(char[] clearChars) {
                clearText[0] = new String(clearChars);
            }
        };

        value.access(accessor);
        result = clearText[0];
        return result;
    }
}
