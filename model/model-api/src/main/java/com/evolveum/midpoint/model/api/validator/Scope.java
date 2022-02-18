/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.validator;

public enum Scope {

    QUICK,            // something that can be done quite regularly (e.g. when an item value is changed)
    THOROUGH;        // something that is done on demand, e.g. checking groovy scripts or checking object references

}
