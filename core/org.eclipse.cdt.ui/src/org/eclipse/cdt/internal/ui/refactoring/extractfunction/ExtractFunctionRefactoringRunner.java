/*******************************************************************************
 * Copyright (c) 2008, 2009 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 *  
 * Contributors: 
 * Institute for Software - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.ui.refactoring.extractfunction;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.model.LanguageManager;
import org.eclipse.cdt.ui.CUIPlugin;

import org.eclipse.cdt.internal.ui.refactoring.RefactoringRunner;

/**
 * @author Emanuel Graf
 */
public class ExtractFunctionRefactoringRunner extends RefactoringRunner  {

	public ExtractFunctionRefactoringRunner(IFile file, ISelection selection,
			IShellProvider shellProvider, ICProject cProject) {
		super(file, selection, null, shellProvider, cProject);
	}

	@Override
	public void run() {
		ExtractFunctionInformation info = new ExtractFunctionInformation();
		ILanguage language = null;
		try {
			language = LanguageManager.getInstance().getLanguageForFile(file, null);
		} catch (CoreException e1) {
			CUIPlugin.log(e1);
		}

		try {
			ExtractFunctionRefactoring refactoring = (ExtractFunctionRefactoring) RefactoringsRegistry.getLanguageDelegate(language,
					RefactoringsRegistry.EXTRACT_FUNCTION);
			if (refactoring != null) {
				refactoring.init(file, selection, info, project);
				// new ExtractFunctionRefactoring(file,selection,info, project);
				ExtractFunctionRefactoringWizard wizard = new ExtractFunctionRefactoringWizard(refactoring,
						info);
				RefactoringWizardOpenOperation operator = new RefactoringWizardOpenOperation(wizard);

				operator.run(shellProvider.getShell(), refactoring.getName());
			}
		} catch (InterruptedException e) {
			// initial condition checking got canceled by the user.
		} catch (CoreException e) {
			CUIPlugin.log(e);
		}
	}
}
