/*******************************************************************************
 * Copyright (c) 2007, 2010 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.build.core.scannerconfig;

import java.util.Map;

import org.eclipse.cdt.make.core.scannerconfig.IScannerConfigBuilderInfo2;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.core.runtime.CoreException;


/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public interface ICfgScannerConfigBuilderInfo2Set {
	boolean isPerRcTypeDiscovery();
	
	void setPerRcTypeDiscovery(boolean on);
	
	Map<CfgInfoContext, IScannerConfigBuilderInfo2> getInfoMap();
	
	CfgInfoContext[] getContexts();
	
	IScannerConfigBuilderInfo2 getInfo(CfgInfoContext context);
	
	IScannerConfigBuilderInfo2 applyInfo(CfgInfoContext context, IScannerConfigBuilderInfo2 base) throws CoreException;
	
	IConfiguration getConfiguration();
	
	boolean isProfileSupported(CfgInfoContext context, String profileId);
}
