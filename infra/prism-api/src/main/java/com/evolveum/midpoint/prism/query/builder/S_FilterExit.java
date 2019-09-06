/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query.builder;

import com.evolveum.midpoint.prism.path.ItemPath;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface S_FilterExit extends S_QueryExit {

    S_AtomicFilterExit endBlock();
    S_FilterExit asc(QName... names);
    S_FilterExit asc(ItemPath path);
    S_FilterExit desc(QName... names);
    S_FilterExit desc(ItemPath path);
    S_FilterExit group(QName... names);
    S_FilterExit group(ItemPath path);
    S_FilterExit offset(Integer n);
    S_FilterExit maxSize(Integer n);
}
