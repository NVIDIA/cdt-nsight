/*******************************************************************************
 * Copyright (c) 2011 NVIDIA and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     NVIDIA - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.ui.text;

import org.eclipse.swt.graphics.RGB;

/**
 * Interface for interacting with line background painting.
 * 
 * @author eostroukhov
 */
public interface ILineBackgroundPainter {

	/**
	 * @return <code>true</code> if painter is disposed
	 */
	boolean isDisposed();

	/**
	 * Schedules refresh.
	 */
	void refresh();

	/**
	 * Assign color for the key
	 * @param key unique key that may be assigned to source positions
	 * @param color to use for matching positions
	 */
	void setBackgroundColor(String key, RGB color);
}
