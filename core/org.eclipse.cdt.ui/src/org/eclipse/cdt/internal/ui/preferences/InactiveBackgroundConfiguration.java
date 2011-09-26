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
package org.eclipse.cdt.internal.ui.preferences;

import org.eclipse.swt.graphics.RGB;

import org.eclipse.cdt.ui.text.IHighlightConfiguration;

import org.eclipse.cdt.internal.ui.editor.InactiveCodeHighlighting;

/**
 * Node for inactive code background.
 */
public class InactiveBackgroundConfiguration implements IHighlightConfiguration {
	public String getBoldPreferenceKey() {
		return null;
	}

	public String getCategory() {
		return PreferencesMessages.CEditorColoringConfigurationBlock_coloring_category_code;
	}

	public String getColorPreferenceKey() {
		return InactiveCodeHighlighting.INACTIVE_CODE_COLOR;
	}

	public RGB getDefaultColor() {
		return new RGB(224, 224, 224);
	}

	public String getDisplayName() {
		return PreferencesMessages.CEditorPreferencePage_behaviorPage_inactiveCodeColor;
	}

	public String getEnabledPreferenceKey() {
		return InactiveCodeHighlighting.INACTIVE_CODE_ENABLE;
	}

	public String getItalicPreferenceKey() {
		return null;
	}

	public String getStrikethroughPreferenceKey() {
		return null;
	}

	public String getUnderlinePreferenceKey() {
		return null;
	}

	public boolean isBoldByDefault() {
		return false;
	}

	public boolean isEnabledByDefault() {
		return true;
	}

	public boolean isItalicByDefault() {
		return false;
	}

	public boolean isStrikethroughByDefault() {
		return false;
	}

	public boolean isUnderlineByDefault() {
		return false;
	}

	public boolean isSemanticHighlighting() {
		return false;
	}
}
