/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Anton Leherbauer (Wind River Systems) - Adapted for CDT
 *     Eugene Ostroukhov (NVIDIA) - implemented highlighting extension 
 *                                  through extension point
 *******************************************************************************/

package org.eclipse.cdt.internal.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.ui.text.CSourceViewerConfiguration;
import org.eclipse.cdt.ui.text.ICPartitions;
import org.eclipse.cdt.ui.text.IColorManager;
import org.eclipse.cdt.ui.text.ISemanticHighlighting;

import org.eclipse.cdt.internal.ui.text.CPresentationReconciler;
import org.eclipse.cdt.internal.ui.text.CSourceViewerScalableConfiguration;

/**
 * Semantic highlighting manager.
 * Cloned from JDT.
 * 
 * @since 4.0
 */
public class SemanticHighlightingManager implements IPropertyChangeListener {

	/**
	 * Highlighting style.
	 */
	public static class HighlightingStyle {

		/** Text attribute */
		private TextAttribute fTextAttribute;
		/** Enabled state */
		private boolean fIsEnabled;

		/**
		 * Initialize with the given text attribute.
		 * @param textAttribute The text attribute
		 * @param isEnabled the enabled state
		 */
		public HighlightingStyle(TextAttribute textAttribute, boolean isEnabled) {
			setTextAttribute(textAttribute);
			setEnabled(isEnabled);
		}

		/**
		 * @return Returns the text attribute.
		 */
		public TextAttribute getTextAttribute() {
			return fTextAttribute;
		}

		/**
		 * @param textAttribute The background to set.
		 */
		public void setTextAttribute(TextAttribute textAttribute) {
			fTextAttribute= textAttribute;
		}

		/**
		 * @return the enabled state
		 */
		public boolean isEnabled() {
			return fIsEnabled;
		}

		/**
		 * @param isEnabled the new enabled state
		 */
		public void setEnabled(boolean isEnabled) {
			fIsEnabled= isEnabled;
		}
	}

	/**
	 * Highlighted Positions.
	 */
	public static class HighlightedPosition extends Position {

		/** Highlighting of the position */
		private HighlightingStyle fStyle;

		/** Lock object */
		private Object fLock;

		/**
		 * Initialize the styled positions with the given offset, length and foreground color.
		 *
		 * @param offset The position offset
		 * @param length The position length
		 * @param highlighting The position's highlighting
		 * @param lock The lock object
		 */
		public HighlightedPosition(int offset, int length, HighlightingStyle highlighting, Object lock) {
			super(offset, length);
			fStyle= highlighting;
			fLock= lock;
		}

		/**
		 * @return Returns a corresponding style range.
		 */
		public StyleRange createStyleRange() {
			int len= 0;
			if (fStyle.isEnabled())
				len= getLength();

			TextAttribute textAttribute= fStyle.getTextAttribute();
			int style= textAttribute.getStyle();
			int fontStyle= style & (SWT.ITALIC | SWT.BOLD | SWT.NORMAL);
			StyleRange styleRange= new StyleRange(getOffset(), len, textAttribute.getForeground(), textAttribute.getBackground(), fontStyle);
			styleRange.strikeout= (style & TextAttribute.STRIKETHROUGH) != 0;
			styleRange.underline= (style & TextAttribute.UNDERLINE) != 0;

			return styleRange;
		}

		/**
		 * Uses reference equality for the highlighting.
		 *
		 * @param off The offset
		 * @param len The length
		 * @param highlighting The highlighting
		 * @return <code>true</code> iff the given offset, length and highlighting are equal to the internal ones.
		 */
		public boolean isEqual(int off, int len, HighlightingStyle highlighting) {
			synchronized (fLock) {
				return !isDeleted() && getOffset() == off && getLength() == len && fStyle == highlighting;
			}
		}

		/**
		 * Is this position contained in the given range (inclusive)? Synchronizes on position updater.
		 *
		 * @param off The range offset
		 * @param len The range length
		 * @return <code>true</code> iff this position is not delete and contained in the given range.
		 */
		public boolean isContained(int off, int len) {
			synchronized (fLock) {
				return !isDeleted() && off <= getOffset() && off + len >= getOffset() + getLength();
			}
		}

		public void update(int off, int len) {
			synchronized (fLock) {
				super.setOffset(off);
				super.setLength(len);
			}
		}

		/*
		 * @see org.eclipse.jface.text.Position#setLength(int)
		 */
		@Override
		public void setLength(int length) {
			synchronized (fLock) {
				super.setLength(length);
			}
		}

		/*
		 * @see org.eclipse.jface.text.Position#setOffset(int)
		 */
		@Override
		public void setOffset(int offset) {
			synchronized (fLock) {
				super.setOffset(offset);
			}
		}

		/*
		 * @see org.eclipse.jface.text.Position#delete()
		 */
		@Override
		public void delete() {
			synchronized (fLock) {
				super.delete();
			}
		}

		/*
		 * @see org.eclipse.jface.text.Position#undelete()
		 */
		@Override
		public void undelete() {
			synchronized (fLock) {
				super.undelete();
			}
		}

		/**
		 * @return Returns the highlighting.
		 */
		public HighlightingStyle getHighlighting() {
			return fStyle;
		}
	}

	/**
	 * Highlighted ranges.
	 */
	public static class HighlightedRange extends Region {
		/** The highlighting key as returned by {@link ISemanticHighlighting#getId()}. */
		private String fKey;

		/**
		 * Initialize with the given offset, length and highlighting key.
		 *
		 * @param offset
		 * @param length
		 * @param key the highlighting key as returned by {@link ISemanticHighlighting#getId()}
		 */
		public HighlightedRange(int offset, int length, String key) {
			super(offset, length);
			fKey= key;
		}

		/**
		 * @return the highlighting key as returned by {@link ISemanticHighlighting#getId()}
		 */
		public String getKey() {
			return fKey;
		}

		/*
		 * @see org.eclipse.jface.text.Region#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {
			return super.equals(o) && o instanceof HighlightedRange && fKey.equals(((HighlightedRange)o).getKey());
		}

		/*
		 * @see org.eclipse.jface.text.Region#hashCode()
		 */
		@Override
		public int hashCode() {
			return super.hashCode() | fKey.hashCode();
		}
	}

	/** Semantic highlighting presenter */
	protected SemanticHighlightingPresenter fPresenter;
	/** Semantic highlighting reconciler */
	private SemanticHighlightingReconciler fReconciler;

	/** Semantic highlightings */
	protected ISemanticHighlighting[] fSemanticHighlightings;
	/** Highlightings */
	protected HighlightingStyle[] fHighlightings;

	/** The editor */
	private CEditor fEditor;
	/** The source viewer */
	protected CSourceViewer fSourceViewer;
	/** The preference store */
	protected IPreferenceStore fPreferenceStore;
	/** The source viewer configuration */
	protected CSourceViewerConfiguration fConfiguration;
	/** The presentation reconciler */
	protected CPresentationReconciler fPresentationReconciler;
	/** Editor language **/
	private ILanguage fLanguage;

	/** The hard-coded ranges */
	protected HighlightedRange[][] fHardcodedRanges;

	/**
	 * Install the semantic highlighting on the given editor infrastructure
	 *
	 * @param editor The C editor
	 * @param sourceViewer The source viewer
	 * @param colorManager The color manager
	 * @param preferenceStore The preference store
	 */
	public void install(CEditor editor, CSourceViewer sourceViewer, IColorManager colorManager, IPreferenceStore preferenceStore) {
		fEditor= editor;
		fSourceViewer= sourceViewer;
		fPreferenceStore= preferenceStore;
		fLanguage = sourceViewer.getLanguage();
		if (fEditor != null) {
			fConfiguration= new CSourceViewerScalableConfiguration(colorManager, preferenceStore, editor, ICPartitions.C_PARTITIONING);
			fPresentationReconciler= (CPresentationReconciler) fConfiguration.getPresentationReconciler(sourceViewer);
		} else {
			fConfiguration= null;
			fPresentationReconciler= null;
		}

		fPreferenceStore.addPropertyChangeListener(this);

		if (isEnabled())
			enable();
	}

	/**
	 * Install the semantic highlighting on the given source viewer infrastructure. No reconciliation will be performed.
	 *
	 * @param sourceViewer the source viewer
	 * @param colorManager the color manager
	 * @param preferenceStore the preference store
	 * @param hardcodedRanges the hard-coded ranges to be highlighted
	 */
	public void install(CSourceViewer sourceViewer, IColorManager colorManager, IPreferenceStore preferenceStore, HighlightedRange[][] hardcodedRanges) {
		fHardcodedRanges= hardcodedRanges;
		install(null, sourceViewer, colorManager, preferenceStore);
	}

	/**
	 * Enable semantic highlighting.
	 */
	private void enable() {
		initializeHighlightings();

		fPresenter= new SemanticHighlightingPresenter();
		fPresenter.install(fSourceViewer, fPresentationReconciler);

		if (fEditor != null) {
			fReconciler= new SemanticHighlightingReconciler();
			fReconciler.install(fEditor, fSourceViewer, fPresenter, fSemanticHighlightings, fHighlightings);
		} else {
			fPresenter.updatePresentation(null, createHardcodedPositions(), new HighlightedPosition[0]);
		}
	}

	/**
	 * Computes the hard-coded positions from the hard-coded ranges
	 *
	 * @return the hard-coded positions
	 */
	protected HighlightedPosition[] createHardcodedPositions() {
		List<HighlightedPosition> positions= new ArrayList<HighlightedPosition>();
		for (int i= 0; i < fHardcodedRanges.length; i++) {
			HighlightedRange range= null;
			HighlightingStyle hl= null;
			for (int j= 0; j < fHardcodedRanges[i].length; j++ ) {
				hl= getHighlighting(fHardcodedRanges[i][j].getKey());
				if (hl.isEnabled()) {
					range= fHardcodedRanges[i][j];
					break;
				}
			}

			if (range != null)
				positions.add(fPresenter.createHighlightedPosition(range.getOffset(), range.getLength(), hl));
		}
		return positions.toArray(new HighlightedPosition[positions.size()]);
	}

	/**
	 * Returns the highlighting corresponding to the given key.
	 *
	 * @param id the highlighting id as returned by {@link ISemanticHighlighting#getId()}
	 * @return the corresponding highlighting
	 */
	private HighlightingStyle getHighlighting(String id) {
		for (int i= 0; i < fSemanticHighlightings.length; i++) {
			ISemanticHighlighting semanticHighlighting= fSemanticHighlightings[i];
			if (id.equals(semanticHighlighting.getId()))
				return fHighlightings[i];
		}
		return null;
	}

	/**
	 * Uninstall the semantic highlighting
	 */
	public void uninstall() {
		disable();

		if (fPreferenceStore != null) {
			fPreferenceStore.removePropertyChangeListener(this);
			fPreferenceStore= null;
		}

		fEditor= null;
		fSourceViewer= null;
		fConfiguration= null;
		fPresentationReconciler= null;
		fHardcodedRanges= null;
	}

	/**
	 * Disable semantic highlighting.
	 */
	private void disable() {
		if (fReconciler != null) {
			fReconciler.uninstall();
			fReconciler= null;
		}

		if (fPresenter != null) {
			fPresenter.uninstall();
			fPresenter= null;
		}

		if (fSemanticHighlightings != null)
			disposeHighlightings();
	}

	/**
	 * @return <code>true</code> iff semantic highlighting is enabled in the preferences
	 */
	protected boolean isEnabled() {
		return SemanticHighlightings.isEnabled(fPreferenceStore, fLanguage);
	}

	/**
	 * Initialize semantic highlightings.
	 */
	protected void initializeHighlightings() {
		fSemanticHighlightings= SemanticHighlightings.getSemanticHighlightings(fLanguage);
		fHighlightings= new HighlightingStyle[fSemanticHighlightings.length];

		for (int i= 0, n= fSemanticHighlightings.length; i < n; i++) {
			ISemanticHighlighting semanticHighlighting= fSemanticHighlightings[i];

			fHighlightings[i]= new HighlightingStyle(semanticHighlighting.getTextAttribute(), semanticHighlighting.isEnabled());
		}
	}

	/**
	 * Dispose the semantic highlightings.
	 */
	protected void disposeHighlightings() {
		for (int i= 0, n= fSemanticHighlightings.length; i < n; i++)
			fSemanticHighlightings[i].dispose();

		fSemanticHighlightings= null;
		fHighlightings= null;
	}

	/*
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		handlePropertyChangeEvent(event);
	}

	/**
	 * Handle the given property change event
	 *
	 * @param event The event
	 * @return 
	 */
	protected boolean handlePropertyChangeEvent(PropertyChangeEvent event) {
		if (fPreferenceStore == null)
			return false; // Uninstalled during event notification

		if (fConfiguration != null)
			fConfiguration.handlePropertyChangeEvent(event);

		if (SemanticHighlightings.affectsEnablement(fPreferenceStore, event, fLanguage)) {
			if (isEnabled())
				enable();
			else
				disable();
		}

		if (!isEnabled())
			return false;
		
		boolean refreshNeeded= false;

		for (int i= 0, n= fSemanticHighlightings.length; i < n; i++) {
			ISemanticHighlighting semanticHighlighting= fSemanticHighlightings[i];

			if (semanticHighlighting.styleUpdated(event)) {
				fHighlightings[i].setEnabled(semanticHighlighting.isEnabled());
				fHighlightings[i].setTextAttribute(semanticHighlighting.getTextAttribute());
				fPresenter.highlightingStyleChanged(fHighlightings[i]);
				refreshNeeded= true;
			}
		}
		
		if (refreshNeeded && fReconciler != null)
			fReconciler.refresh();
		
		return refreshNeeded;
	}

	/**
	 * Force refresh of highlighting.
	 */
	public void refresh() {
		if (fReconciler != null) {
			fReconciler.refresh();
		}
	}
}
