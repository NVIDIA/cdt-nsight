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

import org.eclipse.cdt.core.dom.ast.IASTNode;

/**
 * Interface for AST nodes that need custom handling when converting to 
 * source code.
 */
public interface IWritableNode extends IASTNode {
	/**
	 * @return writer that can persist given node.
	 */
	INodeWriter getWriter();
}
