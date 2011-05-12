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

import org.springframework.core.convert.converter.Converter;

/**
 * Factory to get converter instance.
 *
 * @author elek
 */
@Deprecated
public interface ConverterFactory {

    /**
     * Return with the appropriate converter.
     *
     *
     * @param <S>
     * @param <T>
     * @param targetClass
     * @param value
     * @return
     * @throws UnsupportedOperationException if no converter registerd
     */
    <S, T> Converter<S, T> getConverter(Class<T> targetClass, S value);
}
