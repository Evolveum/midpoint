/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import org.apache.wicket.markup.html.navigation.paging.IPageable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class TreeSelectableBean<T extends Serializable> extends SelectableBean<T> implements IPageable {

	private static final Trace LOGGER = TraceManager.getTrace(TreeSelectableBean.class);

	public TreeSelectableBean() {
		super();
	}

	public TreeSelectableBean(T value) {
		super(value);
	}


	private long offset=0;
	private long count=20;

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	@Override
	public long getCurrentPage() {
		return 0;
	}

	@Override
	public void setCurrentPage(long page) {

	}

	@Override
	public long getPageCount() {
		return 10;
	}
}
