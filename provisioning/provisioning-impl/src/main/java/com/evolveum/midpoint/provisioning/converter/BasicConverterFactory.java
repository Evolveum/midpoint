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
import com.evolveum.midpoint.annotations.CustomValueConverter;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

/**
 * Basic  implementation.
 * 
 * @author elek
 */
public class BasicConverterFactory implements ConverterFactory {

    private static final Trace TRACE = TraceManager.getTrace(DefaultConverterFactory.class);

    private final Map<Pair<?, ?>, Converter<?, ?>> _converters = new HashMap<Pair<?, ?>, Converter<?, ?>>();

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
    @Override
    public <S, T> Converter<S, T> getConverter(Class<T> targetClass, S value) {
        if (null != targetClass && null != value) {
            Pair<Class<S>, Class<T>> key = Pair.of((Class<S>) value.getClass(), targetClass);
            Converter<S, T> converter = (Converter<S, T>) _converters.get(key);
            if (null != converter) {
                return converter;
            }
        }
        String msg = "There is no converter registerd to " + targetClass.getCanonicalName() + " from " + null != value ? value.getClass().getCanonicalName() : "null";
        TRACE.error(msg);
        throw new NoSuchConverterException(msg);
    }

    public void addConverter(Pair of, Converter stringToIntegerConverter) {
        _converters.put(of, stringToIntegerConverter);
    }

    @Deprecated
    public void scanForCustomValueConverter(Class location) {
        AnnotationDB db = new AnnotationDB();
        db.setScanClassAnnotations(true);
        db.setScanFieldAnnotations(false);
        db.setScanMethodAnnotations(false);
        db.setScanParameterAnnotations(false);

        try {
            URL url = ClasspathUrlFinder.findClassBase(location);
            db.scanArchives(url);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to scan for resources", ex);
        }

        Set<String> classes = db.getAnnotationIndex().get(CustomValueConverter.class.getName());
        if (null != classes) {
            for (String className : classes) {
                try {
                    Class<?> clazz = Class.forName(className);
                    if (Converter.class.isAssignableFrom(clazz)) {
                        Converter<?, ?> converter = (Converter<?, ?>) clazz.getConstructor().newInstance();
                        registerConverter(converter);
                    }
                } catch (NoSuchMethodException ex) {
                    TRACE.error("Unable to register CustomValueConverter for class: {}", className, ex);
                } catch (SecurityException ex) {
                    TRACE.error("Unable to register CustomValueConverter for class: {}", className, ex);
                } catch (ClassNotFoundException ex) {
                    TRACE.error("Unable to load class: {}", className, ex);
                } catch (InstantiationException ex) {
                    TRACE.error("Unable to register CustomValueConverter for class: {}", className, ex);
                } catch (IllegalAccessException ex) {
                    TRACE.error("Unable to register CustomValueConverter for class: {}", className, ex);
                } catch (IllegalArgumentException ex) {
                    TRACE.error("Unable to register CustomValueConverter for class: {}", className, ex);
                } catch (InvocationTargetException ex) {
                    TRACE.error("Unable to register CustomValueConverter for class: {}", className, ex);
                }
            }
        }
    }

    /**
     * Register a new {@link ValueConverter}
     *
     * @param <T>
     * @param converter
     * @return
     */
    public <F, S> boolean registerConverter(Converter<F, S> converter) {
        if (null != converter) {
            Type[] o = converter.getClass().getGenericInterfaces();
            for (int i = 0; i < o.length; i++) {
                if (o[i] instanceof ParameterizedType) {
                    ParameterizedType t = (ParameterizedType) o[i];
                    if (Converter.class.equals((Class) t.getRawType())) {
                        Class<F> first = (Class) t.getActualTypeArguments()[0];
                        Class<S> second = (Class) t.getActualTypeArguments()[1];
                        Pair<Class<F>, Class<S>> key = Pair.of(first, second);
                        if (null == _converters.get(key)) {
                            TRACE.debug("Type converter registered: {} to {}", first, second);
                            return null != _converters.put(key, converter);
                        }
                        break;
                    }
                }
            }
        }
        return false;
    }
}
