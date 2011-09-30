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
package org.eclipse.cdt.core.dom.rewrite;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.internal.core.dom.rewrite.astwriter.ASTWriterVisitor;
import org.eclipse.cdt.internal.core.dom.rewrite.astwriter.MacroExpansionHandler;
import org.eclipse.cdt.internal.core.dom.rewrite.commenthandler.NodeCommentMap;

/**
 * Custom writer for AST nodes.
 */
public interface INodeWriter {
	/**
	 * This method is called to rewrite provided AST node.
	 * 
	 * @param node that needs to be rewritten
	 * @param scribe delegate that creates rewrites
	 * @param writerVisitor AST visitor that can rewrite nodes (i.e. can be used to rewrite children)
	 * @param commentMap comment map
	 * @param macroHandler handles macro expansions
	 */
	void write(IWritableNode node, IScribe scribe, ASTWriterVisitor writerVisitor, NodeCommentMap commentMap, MacroExpansionHandler macroHandler);
}
