/*******************************************************************************
 * Copyright (c) 2006, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Anton Leherbauer (Wind River Systems) - initial API and implementation
 *     Markus Schorn (Wind River Systems)
 *     NVIDIA - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.ui.editor;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.model.LanguageManager;
import org.eclipse.cdt.ui.CDTUITools;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.text.IBackgroundHighlight;
import org.eclipse.cdt.ui.text.ILineBackgroundPainter;

import org.eclipse.cdt.internal.core.model.ASTCache;

import org.eclipse.cdt.internal.ui.LineBackgroundPainter;
import org.eclipse.cdt.internal.ui.text.ICReconcilingListener;

/**
 * This class supports pluggable background highlightins.
 * 
 * @see IBackgroundHighlight
 * @since 8.0
 */
public class BackgroundHighlightingManager implements ICReconcilingListener, ITextInputListener,
		ILineBackgroundPainter {
	private static final class ContributionsComparator implements Comparator<IConfigurationElement> {
		public int compare(IConfigurationElement o1, IConfigurationElement o2) {
			int layer1 = getLayerValue(o1.getAttribute(ATTR_LAYER), o1);
			int layer2 = getLayerValue(o2.getAttribute(ATTR_LAYER), o2);
			if (layer1 == layer2) {
				int c = comparePossiblyNullStrings(o1.getAttribute(ATTR_LANGUAGE),
						o2.getAttribute(ATTR_LANGUAGE));
				if (c == 0) {
					// This is simply to have stable sort order - we do not really guarantee the
					// order.
					c = comparePossiblyNullStrings(o1.getAttribute(ATTR_CLASS), o2.getAttribute(ATTR_CLASS));
				}
				return c;
			} else {
				return layer1 > layer2 ? 1 : -1;
			}
		}

		protected int comparePossiblyNullStrings(String s1, String s2) {
			if (s1 == null) {
				return s2 == null ? 0 : 1;
			} else if (s2 == null) {
				return -1;
			} else {
				return s1.trim().compareTo(s2.trim());
			}
		}

		protected final int getLayerValue(String layerString, IConfigurationElement element) {
			if (layerString == null || layerString.trim().length() == 0) {
				return 1;
			}
			try {
				return Integer.valueOf(layerString.trim());
			} catch (NumberFormatException e) {
				CUIPlugin
						.logError(String
								.format("Layer %s is not a valid value in %s background highlight from %s plugin", layerString, //$NON-NLS-1$
										element.getAttribute(ATTR_CLASS), element.getContributor().getName()));
				return 1;
			}
		}
	}

	private static final String ATTR_CLASS = "class"; //$NON-NLS-1$
	private static final String ATTR_LANGUAGE = "language"; //$NON-NLS-1$
	private static final String ATTR_LAYER = "layer"; //$NON-NLS-1$
	private static final String ELEMENT_BACKGROUND_HIGHLIGHTING = "backgroundHighlighting"; //$NON-NLS-1$

	/** The list of currently highlighted positions */
	private List<Position> fCodePositions = Collections.emptyList();
	private IDocument fDocument;
	/** The editor this is installed on */
	private CEditor fEditor;
	/** The lock for job manipulation */
	private Object fJobLock = new Object();
	/** The line background painter */
	private LineBackgroundPainter fLineBackgroundPainter;
	/** The current translation unit */
	private ITranslationUnit fTranslationUnit;
	/** The background job doing the AST parsing */
	private Job fUpdateJob;
	private Collection<IBackgroundHighlight> highlightings;

	/*
	 * @see org.eclipse.cdt.internal.ui.text.ICReconcilingListener#aboutToBeReconciled()
	 */
	public void aboutToBeReconciled() {
	}

	private List<Position> collectPositions(IASTTranslationUnit ast) {
		List<Position> positions = new LinkedList<Position>();
		for (IBackgroundHighlight highlighting : highlightings) {
			positions.addAll(highlighting.collectPositions(ast));
		}
		return positions;
	}

	private Collection<IBackgroundHighlight> getHighlightings(ILanguage language) {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
				SemanticHighlightingRegistry.EXTENSION_POINT_ID);
		Set<IConfigurationElement> set = new TreeSet<IConfigurationElement>(new ContributionsComparator());
		for (IConfigurationElement element : elements) {
			if (ELEMENT_BACKGROUND_HIGHLIGHTING.equals(element.getName())) {
				String lang = element.getAttribute(ATTR_LANGUAGE);
				if (lang == null || (language != null && sameLanguage(lang, language))) {
					set.add(element);
				}
			}
		}
		Collection<IBackgroundHighlight> collection = new LinkedList<IBackgroundHighlight>();
		for (IConfigurationElement element : set) {
			try {
				collection.add((IBackgroundHighlight) element.createExecutableExtension(ATTR_CLASS));
			} catch (Exception e) {
				CUIPlugin.log(String.format("Failed to instantiate highlighter %s from plugin %s", //$NON-NLS-1$
						element.getAttribute(ATTR_CLASS), element.getContributor().getName()), e);
			}
		}
		return collection;
	}

	private boolean sameLanguage(String lang, ILanguage language) {
		return language.equals(LanguageManager.getInstance().getLanguage(lang));
	}

	/*
	 * @see
	 * org.eclipse.jface.text.ITextInputListener#inputDocumentAboutToBeChanged(org.eclipse.jface.text.IDocument
	 * , org.eclipse.jface.text.IDocument)
	 */
	public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
		if (fEditor != null && fLineBackgroundPainter != null && !fLineBackgroundPainter.isDisposed()) {
			fLineBackgroundPainter.removeHighlightPositions(fCodePositions);
			fCodePositions = Collections.emptyList();
		}
	}

	/*
	 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentChanged(org.eclipse.jface.text.IDocument,
	 * org.eclipse.jface.text.IDocument)
	 */
	public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
		fDocument = newInput;
	}

	/**
	 * Install this highlighting on the given editor and line background painter.
	 * 
	 * @param editor
	 * @param lineBackgroundPainter
	 */
	public void install(CEditor editor, LineBackgroundPainter lineBackgroundPainter) {
		assert fEditor == null;
		assert editor != null && lineBackgroundPainter != null;
		
		
		fEditor = editor;
		fLineBackgroundPainter = lineBackgroundPainter;
		ICElement cElement = fEditor.getInputCElement();
		if (cElement instanceof ITranslationUnit) {
			fTranslationUnit = (ITranslationUnit) cElement;
		} else {
			fTranslationUnit = null;
		}
		fDocument = fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
		ILanguage language;
		try {
			language = fTranslationUnit != null ? fTranslationUnit.getLanguage() : null;
		} catch (CoreException e) {
			CUIPlugin.log(e);
			language = null;
		}
		highlightings = getHighlightings(language);
		for (IBackgroundHighlight highlight : highlightings) {
			highlight.install(fDocument, this);
		}
		fEditor.getViewer().addTextInputListener(this);
		fEditor.addReconcileListener(this);
	}

	public boolean isDisposed() {
		return fLineBackgroundPainter.isDisposed();
	}

	/*
	 * @see org.eclipse.cdt.internal.ui.text.ICReconcilingListener#reconciled(IASTTranslationUnit, boolean,
	 * IProgressMonitor)
	 */
	public void reconciled(IASTTranslationUnit ast, final boolean force, IProgressMonitor progressMonitor) {
		if (progressMonitor != null && progressMonitor.isCanceled()) {
			return;
		}
		final List<Position> newInactiveCodePositions = collectPositions(ast);
		Runnable updater = new Runnable() {
			public void run() {
				if (fEditor != null && fLineBackgroundPainter != null && !fLineBackgroundPainter.isDisposed()) {
					fLineBackgroundPainter
							.replaceHighlightPositions(fCodePositions, newInactiveCodePositions);
					fCodePositions = newInactiveCodePositions;
				}
			}
		};
		if (fEditor != null && !Display.getDefault().getThread().equals(Thread.currentThread())) {
			Display.getDefault().asyncExec(updater);
		}
	}

	/**
	 * Force refresh.
	 */
	public void refresh() {
		scheduleJob();
	}

	/**
	 * Schedule update of the inactive code positions in the background.
	 */
	private void scheduleJob() {
		synchronized (fJobLock) {
			if (fUpdateJob == null) {
				fUpdateJob = new Job(CEditorMessages.InactiveCodeHighlighting_job) {
					@Override
					protected IStatus run(final IProgressMonitor monitor) {
						IStatus result = Status.OK_STATUS;
						if (fTranslationUnit != null) {
							final ASTProvider astProvider = CUIPlugin.getDefault().getASTProvider();
							result = astProvider.runOnAST(fTranslationUnit, ASTProvider.WAIT_IF_OPEN,
									monitor, new ASTCache.ASTRunnable() {
										public IStatus runOnAST(ILanguage lang, IASTTranslationUnit ast) {
											reconciled(ast, true, monitor);
											return Status.OK_STATUS;
										}
									});
						}
						if (monitor.isCanceled()) {
							result = Status.CANCEL_STATUS;
						}
						return result;
					}
				};
				fUpdateJob.setPriority(Job.DECORATE);
			}
			if (fUpdateJob.getState() == Job.NONE) {
				fUpdateJob.schedule();
			}
		}
	}

	public void setBackgroundColor(String key, RGB color) {
		fLineBackgroundPainter.setBackgroundColor(key, getColor(color));
	}

	private Color getColor(RGB color) {
		if (color != null) {
			return CDTUITools.getColorManager().getColor(color);
		} else {
			return null;
		}
	}

	/**
	 * Uninstall this highlighting from the editor. Does nothing if already uninstalled.
	 */
	public void uninstall() {
		synchronized (fJobLock) {
			if (fUpdateJob != null && fUpdateJob.getState() == Job.RUNNING) {
				fUpdateJob.cancel();
			}
		}
		for (IBackgroundHighlight highlight : highlightings) {
			highlight.uninstall();
		}
		if (fLineBackgroundPainter != null && !fLineBackgroundPainter.isDisposed()) {
			fLineBackgroundPainter.removeHighlightPositions(fCodePositions);
			fCodePositions = Collections.emptyList();
			fLineBackgroundPainter = null;
		}
		if (fEditor != null) {
			fEditor.removeReconcileListener(this);
			if (fEditor.getViewer() != null) {
				fEditor.getViewer().removeTextInputListener(this);
			}
			fEditor = null;
			fTranslationUnit = null;
			fDocument = null;
		}
	}
}
