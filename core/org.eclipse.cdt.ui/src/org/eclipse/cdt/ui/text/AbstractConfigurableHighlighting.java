/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Anton Leherbauer (Wind River Systems) - Adapted for CDT
 *     Markus Schorn (Wind River Systems)
 *     Eugene Ostroukhov (NVIDIA) - Allow contributing highlightings through
 *                                  extension point
 *******************************************************************************/

package org.eclipse.cdt.ui.text;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.PreferenceConstants;

import org.eclipse.cdt.internal.ui.editor.SemanticToken;
import org.eclipse.cdt.internal.ui.preferences.PreferencesMessages;

/**
 * Semantic highlighting. Cloned from JDT.
 * 
 * This is a base class for highlightings that use default preferences
 * layout and can be shown on the semantic highlighting preference page.
 * 
 * Extenders are supposed to customize the preference name and override
 * {@link ISemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)}
 * method.
 * 
 * @since 4.0
 */
public abstract class AbstractConfigurableHighlighting implements IHighlightConfiguration, ISemanticHighlighting {
	/** Init debugging mode */
	private static final boolean DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.cdt.ui/debug/SemanticHighlighting")); //$NON-NLS-1$//$NON-NLS-2$

	private IColorManager fColorManager;
	private IPreferenceStore fPreferenceStore;
	private AtomicReference<TextAttribute> fTextAttribute = new AtomicReference<TextAttribute>();

	public AbstractConfigurableHighlighting() {
		fColorManager = CUIPlugin.getDefault().getTextTools().getColorManager();
		fPreferenceStore = CUIPlugin.getDefault().getPreferenceStore();
	}

	private void adaptToTextForegroundChange(PropertyChangeEvent event) {
		RGB rgb = null;

		Object value = event.getNewValue();
		if (value instanceof RGB)
			rgb = (RGB) value;
		else if (value instanceof String)
			rgb = StringConverter.asRGB((String) value);

		if (rgb != null) {

			String property = event.getProperty();
			Color color = fColorManager.getColor(property);

			if ((color == null || !rgb.equals(color.getRGB()))) {
				fColorManager.unbindColor(property);
				fColorManager.bindColor(property, rgb);
				color = fColorManager.getColor(property);
			}

			TextAttribute oldAttr = getTextAttribute();
			setTextAttribute(new TextAttribute(color, oldAttr.getBackground(), oldAttr.getStyle()));
		}
	}

	private void adaptToTextStyleChange(PropertyChangeEvent event, int styleAttribute) {
		boolean eventValue = false;
		Object value = event.getNewValue();
		if (value instanceof Boolean)
			eventValue = ((Boolean) value).booleanValue();
		else if (IPreferenceStore.TRUE.equals(value))
			eventValue = true;

		TextAttribute oldAttr = getTextAttribute();
		boolean activeValue = (oldAttr.getStyle() & styleAttribute) == styleAttribute;

		if (activeValue != eventValue)
			setTextAttribute(new TextAttribute(oldAttr.getForeground(), oldAttr.getBackground(), eventValue
					? oldAttr.getStyle() | styleAttribute : oldAttr.getStyle() & ~styleAttribute));
	}

	private void addColor(String colorKey) {
		if (fColorManager != null && colorKey != null && fColorManager.getColor(colorKey) == null) {
			RGB rgb = PreferenceConverter.getColor(fPreferenceStore, colorKey);
			fColorManager.unbindColor(colorKey);
			fColorManager.bindColor(colorKey, rgb);
		}
	}

	@Override
	public final boolean affectsEnablement(PropertyChangeEvent event) {
		return event.getProperty().equals(getEnabledPreferenceKey());
	}

	@Override
	public abstract boolean consumes(SemanticToken token);

	private TextAttribute createInitialTextAttribute() {
		String colorKey = getColorPreferenceKey();
		addColor(colorKey);

		String boldKey = getBoldPreferenceKey();
		int style = fPreferenceStore.getBoolean(boldKey) ? SWT.BOLD : SWT.NORMAL;

		String italicKey = getItalicPreferenceKey();
		if (fPreferenceStore.getBoolean(italicKey))
			style |= SWT.ITALIC;

		String strikethroughKey = getStrikethroughPreferenceKey();
		if (fPreferenceStore.getBoolean(strikethroughKey))
			style |= TextAttribute.STRIKETHROUGH;

		String underlineKey = getUnderlinePreferenceKey();
		if (fPreferenceStore.getBoolean(underlineKey))
			style |= TextAttribute.UNDERLINE;

		final TextAttribute textAttribute = new TextAttribute(fColorManager.getColor(PreferenceConverter
				.getColor(fPreferenceStore, colorKey)), null, style);
		return textAttribute;
	}

	@Override
	public final void dispose() {
		removeColor(getColorPreferenceKey());
	}

	/**
	 * A named preference that controls if the given semantic highlighting has the text attribute bold.
	 * 
	 * @return the bold preference key
	 */
	@Override
	public final String getBoldPreferenceKey() {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + getPreferenceKey()
				+ PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_BOLD_SUFFIX;
	}

	@Override
	public String getCategory() {
		return PreferencesMessages.CEditorColoringConfigurationBlock_coloring_category_code;
	}

	/**
	 * A named preference that controls the given semantic highlighting's color.
	 * 
	 * @return the color preference key
	 */
	@Override
	public final String getColorPreferenceKey() {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + getPreferenceKey()
				+ PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_COLOR_SUFFIX;
	}

	/**
	 * @return the default text color
	 */
	protected abstract RGB getDefaultTextColor();

	/**
	 * @return the display name
	 */
	@Override
	public abstract String getDisplayName();

	/**
	 * A named preference that controls if the given semantic highlighting is enabled.
	 * 
	 * @return the enabled preference key
	 */
	@Override
	public final String getEnabledPreferenceKey() {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + getPreferenceKey()
				+ PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX;
	}

	@Override
	public final String getId() {
		return getPreferenceKey();
	}

	/**
	 * A named preference that controls if the given semantic highlighting has the text attribute italic.
	 * 
	 * @return the italic preference key
	 */
	@Override
	public final String getItalicPreferenceKey() {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + getPreferenceKey()
				+ PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ITALIC_SUFFIX;
	}

	/**
	 * @return the preference key, will be augmented by a prefix and a suffix for each preference
	 */
	protected abstract String getPreferenceKey();

	public final IPreferenceStore getPreferenceStore() {
		return fPreferenceStore;
	}

	/**
	 * A named preference that controls if the given semantic highlighting has the text attribute
	 * strikethrough.
	 * 
	 * @return the strikethrough preference key
	 */
	@Override
	public final String getStrikethroughPreferenceKey() {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + getPreferenceKey()
				+ PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_STRIKETHROUGH_SUFFIX;
	}

	@Override
	public final TextAttribute getTextAttribute() {
		TextAttribute textAttribute = fTextAttribute.get();
		if (textAttribute == null) {
			// I think this getter is only called from SWT thread.
			// But I am not sure so better safe then sorry.
			// TextAttributes do not retain any resources - ok for GC
			textAttribute = createInitialTextAttribute();
			fTextAttribute.compareAndSet(null, textAttribute);
			return fTextAttribute.get();
		}
		return textAttribute;
	}

	/**
	 * A named preference that controls if the given semantic highlighting has the text attribute underline.
	 * 
	 * @return the underline preference key
	 */
	@Override
	public final String getUnderlinePreferenceKey() {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + getPreferenceKey()
				+ PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_UNDERLINE_SUFFIX;
	}

	@Override
	public final boolean isEnabled() {
		return fPreferenceStore.getBoolean(getEnabledPreferenceKey());
	}

	/**
	 * @return <code>true</code> if the text attribute strikethrough is set by default
	 */
	@Override
	public boolean isStrikethroughByDefault() {
		return false;
	}

	/**
	 * @return <code>true</code> if the text attribute underline is set by default
	 * @since 3.1
	 */
	@Override
	public boolean isUnderlineByDefault() {
		return false;
	}

	private void removeColor(String colorKey) {
		fColorManager.unbindColor(colorKey);
	}
	
	@Override
	public RGB getDefaultColor() {
		// Note that this attribute is now also used for background colorings 
		// hence the renamed method
		return getDefaultTextColor();
	}

	/**
	 * Indicates that the highlighting needs to visit implicit names (e.g. overloaded operators)
	 */
	@Override
	public boolean requiresImplicitNames() {
		return false;
	}

	private void setTextAttribute(TextAttribute textAttribute) {
		fTextAttribute.set(textAttribute);
	}
	
	@Override
	public final boolean styleUpdated(PropertyChangeEvent event) {
		if (getColorPreferenceKey().equals(event.getProperty())) {
			adaptToTextForegroundChange(event);
		} else if (getBoldPreferenceKey().equals(event.getProperty())) {
			adaptToTextStyleChange(event, SWT.BOLD);
		} else if (getItalicPreferenceKey().equals(event.getProperty())) {
			adaptToTextStyleChange(event, SWT.ITALIC);
		} else if (getStrikethroughPreferenceKey().equals(event.getProperty())) {
			adaptToTextStyleChange(event, TextAttribute.STRIKETHROUGH);
		} else if (getUnderlinePreferenceKey().equals(event.getProperty())) {
			adaptToTextStyleChange(event, TextAttribute.UNDERLINE);
		} else if (getEnabledPreferenceKey().equals(event.getProperty())) {
			// Do nothing - simply return true
		} else {
			return false;
		}
		return true;
	}
	
	@Override
	public boolean isSemanticHighlighting() {
		return true;
	}
}
