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

package com.evolveum.midpoint.provisioning.converter;

import java.util.ArrayList;
import java.util.List;
import org.springframework.core.convert.converter.Converter;

/**
 * Proxy the request to the first matching factory.
 *
 * @author elek
 */
public class CompositeConverterFactory extends BasicConverterFactory {

    private List<ConverterFactory> factories = new ArrayList<ConverterFactory>();

    @Override
    public <S, T> Converter<S, T> getConverter(Class<T> targetClass, S value) {
        for (ConverterFactory factory : factories) {
            try {
                Converter converter = factory.getConverter(targetClass, value);
                return converter;
            } catch (NoSuchConverterException ex) {
                //noop, try the next factory
            }
        }
        throw new NoSuchConverterException("Converter not found for class " + targetClass);
    }

    public void addConverterFactory(ConverterFactory factory){
        factories.add(factory);

    }
}
