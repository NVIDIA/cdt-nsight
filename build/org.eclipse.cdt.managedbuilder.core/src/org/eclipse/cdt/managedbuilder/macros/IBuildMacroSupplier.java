/*******************************************************************************
 * Copyright (c) 2005, 2010 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.managedbuilder.macros;

import org.eclipse.cdt.core.cdtvariables.ICdtVariable;
import org.eclipse.cdt.managedbuilder.internal.macros.IMacroContextInfo;
import org.eclipse.cdt.utils.cdtvariables.ICdtVariableSupplier;

/**
 * 
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public interface IBuildMacroSupplier extends ICdtVariableSupplier{
	
	/**
	 * 
	 * @param macroName macro name
	 * @param contextType context type
	 * @param contextData context data
	 * @return IBuildMacro
	 */
	public IBuildMacro getMacro(String macroName,
					int contextType, 
					Object contextData);
	
	/**
	 * 
	 * @param contextType context type
	 * @param contextData context data
	 * @return IBuildMacro[]
	 */
	public IBuildMacro[] getMacros(int contextType, 
			Object contextData);
	
	
	public ICdtVariable getVariable(String macroName,
			IMacroContextInfo context);

	public ICdtVariable[] getVariables(IMacroContextInfo context);
}
