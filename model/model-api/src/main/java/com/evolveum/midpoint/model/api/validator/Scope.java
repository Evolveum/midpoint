/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.validator;

public enum Scope {

    QUICK,            // something that can be done quite regularly (e.g. when an item value is changed)
    THOROUGH;        // something that is done on demand, e.g. checking groovy scripts or checking object references

}
