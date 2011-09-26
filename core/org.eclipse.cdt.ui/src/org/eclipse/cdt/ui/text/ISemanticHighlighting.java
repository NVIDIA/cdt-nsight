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

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.cdt.internal.ui.editor.SemanticToken;

/**
 * Interface to be implemented by contributors to the extension point
 * <code>org.eclipse.cdt.ui.semanticHighlighting</code>.
 * 
 * @since 8.0
 */
public interface ISemanticHighlighting {
	/**
	 * Returns <code>true</code> iff the semantic highlighting consumes the semantic token.
	 * <p>
	 * NOTE: Implementors are not allowed to keep a reference on the token or on any object retrieved from the
	 * token.
	 * </p>
	 * 
	 * @param token
	 *            the semantic token for a {@link org.eclipse.cdt.core.dom.ast.IASTName}
	 * @return <code>true</code> iff the semantic highlighting consumes the semantic token
	 */
	boolean consumes(SemanticToken token);

	/**
	 * Called when this rule is no longer needed. Highlighting should release all help resources (i.e. SWT
	 * color object, font, etc).
	 */
	void dispose();

	/**
	 * @return unique ID of this rule
	 */
	String getId();

	/**
	 * @return current text attributes for this rule.
	 */
	TextAttribute getTextAttribute();

	/**
	 * @return <code>true</code> if this rule is currently enabled.
	 */
	boolean isEnabled();

	/**
	 * Indicates that the highlighting needs to visit implicit names (e.g. overloaded operators)
	 */
	boolean requiresImplicitNames();

	/**
	 * Is called when preference value is changed.
	 * 
	 * @return <code>true</code> if the style was changed.
	 */
	boolean styleUpdated(PropertyChangeEvent event);

	/**
	 * @return <code>true</code> if the event changes enablement of this preference.
	 */
	boolean affectsEnablement(PropertyChangeEvent event);
}
