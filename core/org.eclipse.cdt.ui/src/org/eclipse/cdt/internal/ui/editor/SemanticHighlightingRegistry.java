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
package org.eclipse.cdt.internal.ui.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.model.LanguageManager;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.text.IHighlightConfiguration;
import org.eclipse.cdt.ui.text.ISemanticHighlighting;

/**
 * This registry keeps per-language bindings of the semantic highlighters.
 * 
 * Semantic highlighters are queried in this order: language-specific -> imported-from-other-lenguages ->
 * default
 * 
 * @since 8.0
 */
public final class SemanticHighlightingRegistry {
	private static final String ATTR_CLASS = "class"; //$NON-NLS-1$
	private static final String ATTR_LANGUAGE_ID = "languageId"; //$NON-NLS-1$

	private static final String ELEMENT_HIGHLIGHT_CONFIGURATION = "highlightConfiguration"; //$NON-NLS-1$
	private static final String ELEMENT_HIGHLIGHTING = "highlighting"; //$NON-NLS-1$

	public static final String EXTENSION_POINT_ID = CUIPlugin.PLUGIN_ID + ".semanticHighlighting"; //$NON-NLS-1$

	private ISemanticHighlighting[] allHighlighters;
	private IHighlightConfiguration[] configurableHighlighters;
	private Map<ILanguage, ISemanticHighlighting[]> table;

	private void createConfigurable(IConfigurationElement element,
			Collection<IHighlightConfiguration> configurables) throws CoreException {
		IHighlightConfiguration highlighting = (IHighlightConfiguration) element
				.createExecutableExtension(ATTR_CLASS);
		configurables.add(highlighting);
	}

	private void createHighlighting(IConfigurationElement element,
			final Map<ILanguage, Collection<ISemanticHighlighting>> perLanguage,
			Collection<ISemanticHighlighting> allLanguages) throws CoreException {
		Collection<ISemanticHighlighting> collection;
		String languageId = element.getAttribute(ATTR_LANGUAGE_ID);
		if (languageId != null) {
			ILanguage language = getLanguage(element, languageId);
			collection = perLanguage.get(language);
			if (collection == null) {
				collection = new LinkedList<ISemanticHighlighting>();
				perLanguage.put(language, collection);
			}
		} else {
			collection = allLanguages;
		}
		ISemanticHighlighting highlighting = (ISemanticHighlighting) element
				.createExecutableExtension(ATTR_CLASS);
		collection.add(highlighting);
	}

	private void fillMap(Map<ILanguage, Collection<ISemanticHighlighting>> cache,
			Collection<ISemanticHighlighting> defaults, Collection<IHighlightConfiguration> configurables) {
		table = new HashMap<ILanguage, ISemanticHighlighting[]>(cache.size() + 1);
		Set<Entry<ILanguage, Collection<ISemanticHighlighting>>> set = cache.entrySet();
		List<ISemanticHighlighting> builtins = Arrays.asList(((ISemanticHighlighting[]) SemanticHighlightings
				.getBuiltinSemanticHighlightings()));
		Collection<ISemanticHighlighting> all = new HashSet<ISemanticHighlighting>();

		int defaultsCount = defaults.size();
		int builtinsCount = builtins.size();

		for (Entry<ILanguage, Collection<ISemanticHighlighting>> entry : set) {
			Collection<ISemanticHighlighting> collection = entry.getValue();
			ArrayList<ISemanticHighlighting> list = new ArrayList<ISemanticHighlighting>(collection.size()
					+ defaultsCount + builtinsCount);
			list.addAll(collection);
			list.addAll(defaults);
			list.addAll(builtins);
			ISemanticHighlighting[] array = list.toArray(new ISemanticHighlighting[list.size()]);
			table.put(entry.getKey(), array);

			all.addAll(collection);
		}

		ArrayList<ISemanticHighlighting> list = new ArrayList<ISemanticHighlighting>(defaultsCount
				+ builtinsCount);
		list.addAll(defaults);
		list.addAll(builtins);
		ISemanticHighlighting[] highlightings = list.toArray(new ISemanticHighlighting[list.size()]);
		table.put(null, highlightings);

		all.addAll(defaults);
		all.addAll(builtins);

		allHighlighters = all.toArray(new ISemanticHighlighting[all.size()]);
		for (ISemanticHighlighting highlighting : all) {
			if (highlighting instanceof IHighlightConfiguration) {
				configurables.add((IHighlightConfiguration) highlighting);
			}
		}
		configurableHighlighters = configurables.toArray(new IHighlightConfiguration[configurables.size()]);
	}

	public ISemanticHighlighting[] getAllHighlighters() {
		if (allHighlighters == null) {
			readExtensions();
		}
		return allHighlighters;
	}

	public IHighlightConfiguration[] getConfigurableHighlighters() {
		if (configurableHighlighters == null) {
			readExtensions();
		}
		return configurableHighlighters;
	}

	public ISemanticHighlighting[] getHighlighters(ILanguage language) {
		if (table == null) {
			readExtensions();
		}
		if (language != null && table.containsKey(language.getId())) {
			return table.get(language);
		} else {
			return table.get(null);
		}
	}

	private ILanguage getLanguage(IConfigurationElement element, String languageId) throws CoreException {
		ILanguage language = LanguageManager.getInstance().getLanguage(languageId);
		if (language == null) {
			throw new CoreException(new Status(IStatus.ERROR, String.format(
					"Language %s not found when reading extensions from plugin %s", languageId, element //$NON-NLS-1$
							.getContributor().getName()), element.getContributor().getName()));
		}
		return language;
	}

	private synchronized void readExtensions() {
		if (table == null) {
			final Map<ILanguage, Collection<ISemanticHighlighting>> cache = new HashMap<ILanguage, Collection<ISemanticHighlighting>>();
			Collection<ISemanticHighlighting> highlightings = new LinkedList<ISemanticHighlighting>();
			Collection<IHighlightConfiguration> configurables = new LinkedList<IHighlightConfiguration>();

			IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
					EXTENSION_POINT_ID);
			for (IConfigurationElement element : elements) {
				try {
					if (ELEMENT_HIGHLIGHTING.equals(element.getName())) {
						createHighlighting(element, cache, highlightings);
					} else if (ELEMENT_HIGHLIGHT_CONFIGURATION.equals(element.getName())) {
						createConfigurable(element, configurables);
					}
				} catch (Exception e) {
					String pluginName = element.getContributor().getName();
					CUIPlugin.log(String.format("Failed to read extension from plugin %s", pluginName), e); //$NON-NLS-1$
				}
			}
			fillMap(cache, highlightings, configurables);
		}
	}

}
