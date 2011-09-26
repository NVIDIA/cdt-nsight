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
 * Preference tree node for highlight preferences.
 * 
 * Note that these preferences are currently to be stored in org.eclipse.cdt.ui preference store.
 */
public interface IHighlightConfiguration {
	/**
	 * @return name of the preference that controls if highlight is bold or <code>null</code> if highlight
	 *         does not have one.
	 */
	String getBoldPreferenceKey();

	/**
	 * @return category name. Preferences that return same string value will be grouped in the tree
	 */
	String getCategory();

	/**
	 * @return name of the preference that controls highlight color or <code>null</code> if highlight does not
	 *         have one.
	 */
	String getColorPreferenceKey();

	/**
	 * @return default color value for given preference value. Cannot be <code>null</code>.
	 */
	RGB getDefaultColor();

	/**
	 * @return name that will be shown in the tree.
	 */
	String getDisplayName();

	/**
	 * @return name of the preference that controls if highlight is enabled or <code>null</code> if highlight
	 *         does not have one.
	 */
	String getEnabledPreferenceKey();

	/**
	 * @return name of the preference that controls if highlight is italic or <code>null</code> if highlight
	 *         does not have one.
	 */
	String getItalicPreferenceKey();

	/**
	 * @return name of the preference that controls if highlight is strikethrough or <code>null</code> if
	 *         highlight does not have one.
	 */
	String getStrikethroughPreferenceKey();

	/**
	 * @return name of the preference that controls if highlight is underlined or <code>null</code> if
	 *         highlight does not have one.
	 */
	String getUnderlinePreferenceKey();

	/**
	 * @return if highlight uses bold font by default
	 */
	boolean isBoldByDefault();

	/**
	 * @return if highlight is enabled by default
	 */
	boolean isEnabledByDefault();

	/**
	 * @return if highlight uses italic font by default
	 */
	boolean isItalicByDefault();

	/**
	 * @return if highlight is stricken through by default
	 */
	boolean isStrikethroughByDefault();

	/**
	 * @return if highlight is underlined by default
	 */
	boolean isUnderlineByDefault();
	
	/**
	 * @return <code>true</code> if this highlighting only works when semantic highlight 
	 * is enabled.
	 */
	boolean isSemanticHighlighting();
}
