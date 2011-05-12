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

package com.evolveum.midpoint.util.diff;

import java.lang.reflect.Field;
import org.custommonkey.xmlunit.ComparisonController;
import org.custommonkey.xmlunit.DifferenceEngine;

/**
 * Main purpose of the class is to inject alternative implementation of XPathNodeTracker
 * into XMLUnit's DifferenceEngine
 *
 * @see DifferenceEngine
 * @see XpathNodeTracker
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class MidPointDifferenceEngine extends DifferenceEngine {

    private void setPrivatePropertyValue(String fieldName, Object value) {
        try {
            Class<?> clazz = DifferenceEngine.class;
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(this, value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(ex);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        } catch (NoSuchFieldException ex) {
            throw new IllegalStateException(ex);
        } catch (SecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public MidPointDifferenceEngine(ComparisonController controller) {
        super(controller);

        setPrivatePropertyValue("controlTracker", new MidPointXpathNodeTracker());
        setPrivatePropertyValue("testTracker", new MidPointXpathNodeTracker());

    }

}
