/*******************************************************************************
 * Copyright (c) 2009 Alena Laskavaia 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alena Laskavaia  - initial API and implementation
 *******************************************************************************/ 

package org.eclipse.cdt.codan.core.cxx.internal.model.cfg;

import org.eclipse.cdt.codan.internal.core.cfg.StartNode;
import org.eclipse.cdt.codan.provisional.core.model.cfg.IBasicBlock;

/**
 * TODO: add description
 */
public class CxxStartNode extends StartNode {

	/**
	 * @param next
	 */
	public CxxStartNode(IBasicBlock next) {
		super(next);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "start";
	}
}
