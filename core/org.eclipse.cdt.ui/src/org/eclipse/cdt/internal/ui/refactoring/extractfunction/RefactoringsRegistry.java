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
package org.eclipse.cdt.internal.ui.refactoring.extractfunction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ltk.core.refactoring.Refactoring;

import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.ui.CUIPlugin;

/**
 * This registry returns refactorings for given language.
 */
public class RefactoringsRegistry {
	private static final String EXTENSION_POINT = CUIPlugin.PLUGIN_ID + ".refactoringDelegates"; //$NON-NLS-1$

	private static final String ATTR_CLASS = "class";  //$NON-NLS-1$
	private static final String ATTR_KIND = "kind"; //$NON-NLS-1$
	private static final String ATTR_LANGUAGE_ID = "languageId"; //$NON-NLS-1$

	/**
	 * ID of the extract function refactoring.
	 */
	public static final String EXTRACT_FUNCTION = "extractFunction"; //$NON-NLS-1$

	/**
	 * @param language
	 *            language object, can be <code>null</code>
	 * @param kind
	 *            refactoring ID. It can be one of the built-in refactorings or a refactoring from other
	 *            plugins.
	 * @return new instance of the Refactoring object. It will try to find refactoring for the particular
	 *         language and will return default one when there is no language-specific. Will return default
	 *         one if language ID is null.
	 */
	public static Refactoring getLanguageDelegate(ILanguage language, String kind) throws CoreException {
		IConfigurationElement candidate = null;
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
				EXTENSION_POINT);
		for (IConfigurationElement element : elements) {
			if (kind.equals(element.getAttribute(ATTR_KIND))) {
				candidate = element;
				if (language == null || language.getId() == null
						|| language.getId().equals(element.getAttribute(ATTR_LANGUAGE_ID))) {
					break;
				}
			}
		}
		if (candidate != null) {
			return (Refactoring) candidate.createExecutableExtension(ATTR_CLASS);
		} else {
			return getLegacyDelegate(kind);
		}
	}

	private static Refactoring getLegacyDelegate(String kind) {
		if (EXTRACT_FUNCTION.equals(kind)) {
			return new ExtractFunctionRefactoring();
		}
		return null;
	}
}
