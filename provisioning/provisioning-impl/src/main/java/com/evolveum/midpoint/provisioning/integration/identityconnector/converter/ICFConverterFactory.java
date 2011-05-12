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

import com.evolveum.midpoint.provisioning.converter.BasicConverterFactory;
import com.evolveum.midpoint.provisioning.converter.CompositeConverterFactory;
import com.evolveum.midpoint.provisioning.converter.DefaultConverterFactory;

/**
 * ICF specific converters.
 *
 * @author elek
 */
public class ICFConverterFactory extends CompositeConverterFactory {

    private static ICFConverterFactory instance = new ICFConverterFactory();

    private ICFConverterFactory() {
        try {
            addConverterFactory(DefaultConverterFactory.getInstace());
            BasicConverterFactory icfConverters = new BasicConverterFactory();
            icfConverters.registerConverter(new StringToGuardedStringConverter());
            icfConverters.registerConverter(new GuardedStringToStringConverter());
            addConverterFactory(icfConverters);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public static ICFConverterFactory getInstance() {
        return instance;
    }
}
