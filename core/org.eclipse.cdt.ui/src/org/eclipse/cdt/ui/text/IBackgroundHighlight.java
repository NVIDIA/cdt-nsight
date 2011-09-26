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

import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

/**
 * Interface for processors that compute background color for C
 * source code.
 *  
 * @author eostroukhov
 */
public interface IBackgroundHighlight {

	/**
	 * Returns list of highlighted positions.
	 * 
	 * @param ast current syntax tree
	 * @return list of positions where highlight should be applied.
	 */
	List<Position> collectPositions(IASTTranslationUnit ast);

	/**
	 * Is called when background highlighting is installed on an editor.
	 * 
	 * @param document document being edited
	 * @param painter line background painter that collects positions
	 */
	void install(IDocument document, ILineBackgroundPainter painter);
	
	/**
	 * Is called when background highlighting is removed from the editor.
	 */
	void uninstall();
}
