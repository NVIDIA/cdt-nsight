/*******************************************************************************
 * Copyright (c) 2006 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/ 

package org.eclipse.cdt.internal.core.pdom.dom.cpp;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.internal.core.pdom.PDOM;
import org.eclipse.cdt.internal.core.pdom.dom.PDOMNode;
import org.eclipse.core.runtime.CoreException;

class PDOMCPPConstructor extends PDOMCPPMethod implements ICPPConstructor {

	public PDOMCPPConstructor(PDOM pdom, PDOMNode parent, ICPPConstructor method) throws CoreException {
		super(pdom, parent, method);
	}

	public PDOMCPPConstructor(PDOM pdom, int record) {
		super(pdom, record);
	}

	public boolean isExplicit() throws DOMException {
		return getBit(getByte(record + ANNOTATION1), PDOMCPPAnnotation.EXPLICIT_CONSTRUCTOR_OFFSET);
	}
	
	public int getNodeType() {
		return PDOMCPPLinkage.CPP_CONSTRUCTOR;
	}
}
