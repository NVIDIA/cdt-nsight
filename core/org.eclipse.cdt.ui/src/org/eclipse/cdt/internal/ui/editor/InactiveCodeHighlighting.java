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
 *******************************************************************************/
package org.eclipse.cdt.internal.ui.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorElifStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorElseStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorEndifStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfdefStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfndefStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.text.IBackgroundHighlight;
import org.eclipse.cdt.ui.text.ILineBackgroundPainter;

import org.eclipse.cdt.internal.ui.LineBackgroundPainter;

/**
 * Paints code lines disabled by preprocessor directives (#ifdef etc.) with a configurable background color
 * (default light gray).
 * 
 * @see LineBackgroundPainter
 * @since 4.0
 */
public class InactiveCodeHighlighting implements IBackgroundHighlight, IPropertyChangeListener {
	/**
	 * Implementation of <code>IRegion</code> that can be reused by setting the offset and the length.
	 */
	private static class HighlightPosition extends TypedPosition implements IRegion {
		public HighlightPosition(int offset, int length, String type) {
			super(offset, length, type);
		}

		public HighlightPosition(IRegion region, String type) {
			super(region.getOffset(), region.getLength(), type);
		}
	}
	/** Preference key for inactive code painter color */
	public static final String INACTIVE_CODE_COLOR = "inactiveCodeColor"; //$NON-NLS-1$
	/** Preference key for inactive code painter enablement */
	public static final String INACTIVE_CODE_ENABLE = "inactiveCodeEnable"; //$NON-NLS-1$
	/** The key to use for the {@link LineBackgroundPainter} */
	private static final String INACTIVE_CODE_KEY = "inactiveCode"; //$NON-NLS-1$

	private IDocument fDocument;
	/** The line background painter */
	private ILineBackgroundPainter fLineBackgroundPainter;
	private IPreferenceStore fPrefStore;

	public List<Position> collectPositions(IASTTranslationUnit translationUnit) {
		if (!isInactiveCodePositionsActive() || translationUnit == null) {
			return Collections.emptyList();
		}
		String fileName = translationUnit.getFilePath();
		if (fileName == null) {
			return Collections.emptyList();
		}
		List<Position> positions = new ArrayList<Position>();
		int inactiveCodeStart = -1;
		boolean inInactiveCode = false;
		Stack<Boolean> inactiveCodeStack = new Stack<Boolean>();

		IASTPreprocessorStatement[] preprocStmts = translationUnit.getAllPreprocessorStatements();

		for (IASTPreprocessorStatement statement : preprocStmts) {
			IASTFileLocation floc = statement.getFileLocation();
			if (floc == null || !fileName.equals(floc.getFileName())) {
				// preprocessor directive is from a different file
				continue;
			}
			if (statement instanceof IASTPreprocessorIfStatement) {
				IASTPreprocessorIfStatement ifStmt = (IASTPreprocessorIfStatement) statement;
				inactiveCodeStack.push(Boolean.valueOf(inInactiveCode));
				if (!ifStmt.taken()) {
					if (!inInactiveCode) {
						inactiveCodeStart = floc.getNodeOffset();
						inInactiveCode = true;
					}
				}
			} else if (statement instanceof IASTPreprocessorIfdefStatement) {
				IASTPreprocessorIfdefStatement ifdefStmt = (IASTPreprocessorIfdefStatement) statement;
				inactiveCodeStack.push(Boolean.valueOf(inInactiveCode));
				if (!ifdefStmt.taken()) {
					if (!inInactiveCode) {
						inactiveCodeStart = floc.getNodeOffset();
						inInactiveCode = true;
					}
				}
			} else if (statement instanceof IASTPreprocessorIfndefStatement) {
				IASTPreprocessorIfndefStatement ifndefStmt = (IASTPreprocessorIfndefStatement) statement;
				inactiveCodeStack.push(Boolean.valueOf(inInactiveCode));
				if (!ifndefStmt.taken()) {
					if (!inInactiveCode) {
						inactiveCodeStart = floc.getNodeOffset();
						inInactiveCode = true;
					}
				}
			} else if (statement instanceof IASTPreprocessorElseStatement) {
				IASTPreprocessorElseStatement elseStmt = (IASTPreprocessorElseStatement) statement;
				if (!elseStmt.taken() && !inInactiveCode) {
					inactiveCodeStart = floc.getNodeOffset();
					inInactiveCode = true;
				} else if (elseStmt.taken() && inInactiveCode) {
					int inactiveCodeEnd = floc.getNodeOffset();
					positions.add(createHighlightPosition(inactiveCodeStart, inactiveCodeEnd, false,
							INACTIVE_CODE_KEY));
					inInactiveCode = false;
				}
			} else if (statement instanceof IASTPreprocessorElifStatement) {
				IASTPreprocessorElifStatement elifStmt = (IASTPreprocessorElifStatement) statement;
				if (!elifStmt.taken() && !inInactiveCode) {
					inactiveCodeStart = floc.getNodeOffset();
					inInactiveCode = true;
				} else if (elifStmt.taken() && inInactiveCode) {
					int inactiveCodeEnd = floc.getNodeOffset();
					positions.add(createHighlightPosition(inactiveCodeStart, inactiveCodeEnd, false,
							INACTIVE_CODE_KEY));
					inInactiveCode = false;
				}
			} else if (statement instanceof IASTPreprocessorEndifStatement) {
				try {
					boolean wasInInactiveCode = inactiveCodeStack.pop().booleanValue();
					if (inInactiveCode && !wasInInactiveCode) {
						int inactiveCodeEnd = floc.getNodeOffset() + floc.getNodeLength();
						positions.add(createHighlightPosition(inactiveCodeStart, inactiveCodeEnd, true,
								INACTIVE_CODE_KEY));
					}
					inInactiveCode = wasInInactiveCode;
				} catch (EmptyStackException e) {
				}
			}
		}
		if (inInactiveCode) {
			// handle unterminated #if - http://bugs.eclipse.org/255018
			int inactiveCodeEnd = fDocument.getLength();
			positions
					.add(createHighlightPosition(inactiveCodeStart, inactiveCodeEnd, true, INACTIVE_CODE_KEY));
		}
		return positions;
	}

	/**
	 * Create a highlight position aligned to start at a line offset. The region's start is decreased to the
	 * line offset, and the end offset decreased to the line start if <code>inclusive</code> is
	 * <code>false</code>.
	 * 
	 * @param startOffset
	 *            the start offset of the region to align
	 * @param endOffset
	 *            the (exclusive) end offset of the region to align
	 * @param inclusive
	 *            whether the last line should be included or not
	 * @param key
	 *            the highlight key
	 * @return a position aligned for background highlighting
	 */
	private HighlightPosition createHighlightPosition(int startOffset, int endOffset, boolean inclusive,
			String key) {
		final IDocument document = fDocument;
		try {
			if (document != null) {
				int start = document.getLineOfOffset(startOffset);
				int end = document.getLineOfOffset(endOffset);
				startOffset = document.getLineOffset(start);
				if (!inclusive) {
					endOffset = document.getLineOffset(end);
				}
			}
		} catch (BadLocationException x) {
			// concurrent modification?
		}
		return new HighlightPosition(startOffset, endOffset - startOffset, key);
	}

	/**
	 * Returns the shared color for the given key.
	 * 
	 * @param key
	 *            the color key string
	 * @return the shared color for the given key
	 */
	private RGB getColor(String key) {
		if (fPrefStore != null) {
			RGB rgb = PreferenceConverter.getColor(fPrefStore, key);
			return rgb;
		}
		return null;
	}

	/**
	 * Hide inactive code positions.
	 */
	private void hideInactiveCodePositions() {
		if (fLineBackgroundPainter != null) {
			fLineBackgroundPainter.refresh();
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
	 * @param document
	 * @param lineBackgroundPainter
	 */
	public void install(IDocument document, ILineBackgroundPainter lineBackgroundPainter) {
		assert lineBackgroundPainter != null;
		fPrefStore = CUIPlugin.getDefault().getPreferenceStore();
		fLineBackgroundPainter = lineBackgroundPainter;
		fDocument = document;
		fLineBackgroundPainter.setBackgroundColor(INACTIVE_CODE_KEY, getColor(INACTIVE_CODE_COLOR));
		fPrefStore.addPropertyChangeListener(this);
	}

	/**
	 * @return true if inactive code highlighting is active.
	 */
	private boolean isInactiveCodePositionsActive() {
		if (fPrefStore != null) {
			return fPrefStore.getBoolean(INACTIVE_CODE_ENABLE);
		}
		return false;
	}

	public void propertyChange(PropertyChangeEvent event) {
		String p = event.getProperty();
		if (p.equals(INACTIVE_CODE_ENABLE)) {
			if (isInactiveCodePositionsActive()) {
				showInactiveCodePositions(true);
			} else {
				hideInactiveCodePositions();
			}
		} else if (p.equals(INACTIVE_CODE_COLOR)) {
			updateInactiveCodeColor();
		}
	}

	/**
	 * Show inactive code positions.
	 * 
	 * @param refresh
	 *            trigger a refresh of the positions
	 */
	private void showInactiveCodePositions(boolean refresh) {
		if (fLineBackgroundPainter != null) {
			fLineBackgroundPainter.refresh();
		}
	}

	/**
	 * Uninstall this highlighting from the editor. Does nothing if already uninstalled.
	 */
	public void uninstall() {
		if (fLineBackgroundPainter != null && !fLineBackgroundPainter.isDisposed()) {
			fLineBackgroundPainter = null;
		}
		fDocument = null;
	}

	/**
	 * Update the color for inactive code positions.
	 */
	private void updateInactiveCodeColor() {
		if (fLineBackgroundPainter != null) {
			fLineBackgroundPainter.setBackgroundColor(INACTIVE_CODE_KEY, getColor(INACTIVE_CODE_COLOR));
			if (isInactiveCodePositionsActive()) {
				fLineBackgroundPainter.refresh();
			}
		}
	}
}
