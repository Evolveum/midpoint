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

/**
 * A generic class for pairs.
 *
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class Pair<A, B> {

    public static final String code_id = "$Id$";
    public final A fst;
    public final B snd;

    public Pair(A fst, B snd) {
        this.fst = fst;
        this.snd = snd;
    }

    @Override
    public String toString() {
        return "Pair[" + fst + "," + snd + "]";
    }

    private static boolean equals(Object x, Object y) {
        return (x == null && y == null) || (x != null && x.equals(y));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Pair &&
                equals(fst, ((Pair) other).fst) &&
                equals(snd, ((Pair) other).snd);
    }

    @Override
    public int hashCode() {
        if (fst == null) {
            return (snd == null) ? 0 : snd.hashCode() + 1;
        } else if (snd == null) {
            return fst.hashCode() + 2;
        } else {
            return fst.hashCode() * 17 + snd.hashCode();
        }
    }

    public static <A, B> Pair<A, B> of(A a, B b) {
        return new Pair<A, B>(a, b);
    }
}
