/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Anton Leherbauer (Wind River Systems) - Adapted for CDT
 *     Eugene Ostroukhov (NVIDIA) - Allow contributing highlightings through
 *                                  extension point
 *******************************************************************************/

package org.eclipse.cdt.internal.ui.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTImplicitName;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IEnumeration;
import org.eclipse.cdt.core.dom.ast.IEnumerator;
import org.eclipse.cdt.core.dom.ast.IField;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.ILabel;
import org.eclipse.cdt.core.dom.ast.IMacroBinding;
import org.eclipse.cdt.core.dom.ast.IParameter;
import org.eclipse.cdt.core.dom.ast.IProblemBinding;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.cdt.core.dom.ast.c.ICExternalBinding;
import org.eclipse.cdt.core.dom.ast.c.ICFunctionScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPBlockScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPFunctionScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNamespace;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateNonTypeParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateParameter;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.PreferenceConstants;
import org.eclipse.cdt.ui.text.AbstractConfigurableHighlighting;
import org.eclipse.cdt.ui.text.IHighlightConfiguration;
import org.eclipse.cdt.ui.text.ISemanticHighlighting;

import org.eclipse.cdt.internal.core.dom.parser.cpp.ICPPUnknownBinding;
import org.eclipse.cdt.internal.core.dom.parser.cpp.OverloadableOperator;


/**
 * Semantic highlightings.
 * Derived from JDT.
 * 
 * @since 4.0
 */
public class SemanticHighlightings {

	private static final RGB RGB_BLACK = new RGB(0, 0, 0);

	/**
	 * A named preference part that controls the highlighting of static fields.
	 */
	public static final String STATIC_FIELD="staticField"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of fields.
	 */
	public static final String FIELD="field"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of method declarations.
	 */
	public static final String METHOD_DECLARATION="methodDeclaration"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of static method invocations.
	 */
	public static final String STATIC_METHOD_INVOCATION="staticMethod"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of function declarations.
	 */
	public static final String FUNCTION_DECLARATION="functionDeclaration"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of functions.
	 */
	public static final String FUNCTION="function"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of local variables.
	 */
	public static final String LOCAL_VARIABLE_DECLARATION="localVariableDeclaration"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of local variable references.
	 */
	public static final String LOCAL_VARIABLE="localVariable"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of global variables.
	 */
	public static final String GLOBAL_VARIABLE="globalVariable"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of parameter variables.
	 */
	public static final String PARAMETER_VARIABLE="parameterVariable"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of template parameters.
	 */
	public static final String TEMPLATE_PARAMETER="templateParameter"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of methods.
	 */
	public static final String METHOD="method"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of classes.
	 */
	public static final String CLASS="class"; //$NON-NLS-1$
	
	/**
	 * A named preference part that controls the highlighting of enums.
	 */
	public static final String ENUM="enum"; //$NON-NLS-1$
	
	/**
	 * A named preference part that controls the highlighting of macro references.
	 */
	public static final String MACRO_REFERENCE="macroSubstitution"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of macro definitions.
	 */
	public static final String MACRO_DEFINITION="macroDefinition"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of typedefs.
	 */
	public static final String TYPEDEF="typedef"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of namespaces.
	 */
	public static final String NAMESPACE="namespace"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of labels.
	 */
	public static final String LABEL="label"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of enumerators.
	 */
	public static final String ENUMERATOR="enumerator"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of problems.
	 */
	public static final String PROBLEM="problem"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of external SDK.
	 */
	public static final String EXTERNAL_SDK="externalSDK"; //$NON-NLS-1$
	
	/**
	 * A named preference part that controls the highlighting of operators that have been overloaded.
	 */
	public static final String OVERLOADED_OPERATOR="overloadedOperator"; //$NON-NLS-1$
	
	/**
	 * Semantic highlightings
	 */
	private static AbstractConfigurableHighlighting[] fgSemanticHighlightings;
	
	/**
	 * Registry of the per-language highlighters.
	 */
	private static final SemanticHighlightingRegistry fHRegistry = new SemanticHighlightingRegistry();

	/**
	 * Semantic highlighting for static fields.
	 */
	private static final class StaticFieldHighlighting extends AbstractConfigurableHighlighting {

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return STATIC_FIELD;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return new RGB(0, 0, 192);
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_staticField; 
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTName) {
				IASTName name= (IASTName)node;
				if (name instanceof ICPPASTQualifiedName && name.isReference()) {
					return false;
				}
				IBinding binding= token.getBinding();
				if (binding instanceof IField && !(binding instanceof IProblemBinding)) {
					return ((IField)binding).isStatic();
				}
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for fields.
	 */
	private static final class FieldHighlighting extends AbstractConfigurableHighlighting {

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return FIELD;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return new RGB(0, 0, 192);
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_field; 
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTName) {
				IASTName name= (IASTName)node;
				if (name instanceof ICPPASTQualifiedName && name.isReference()) {
					return false;
				}
				IBinding binding= token.getBinding();
				if (binding instanceof IField) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for method declarations.
	 */
	private static final class MethodDeclarationHighlighting extends AbstractConfigurableHighlighting {

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return METHOD_DECLARATION;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return RGB_BLACK;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_methodDeclaration; 
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTImplicitName)
				return false;

			if (node instanceof IASTName) {
				IASTName name= (IASTName)node;
				if (!name.isReference()) {
					IBinding binding= token.getBinding();
					if (binding instanceof ICPPMethod) {
						return true;
					} else if (binding instanceof IProblemBinding) {
						// try to be derive from AST
						node= name.getParent();
						while (node instanceof IASTName) {
							node= node.getParent();
						}
						if (node instanceof ICPPASTFunctionDeclarator) {
							if (name instanceof ICPPASTQualifiedName) {
								ICPPASTQualifiedName qName= (ICPPASTQualifiedName)name;
								IASTName[] names= qName.getNames();
								if (names.length > 1) {
									if (names[names.length - 2].getBinding() instanceof ICPPClassType) {
										return true;
									}
								}
							} else {
								while (node != token.getRoot() && !(node.getParent() instanceof IASTDeclSpecifier)) {
									node= node.getParent();
								}
								if (node instanceof ICPPASTCompositeTypeSpecifier) {
									return true;
								}
							}
						}
					}
				}
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for static method invocations.
	 */
	private static final class StaticMethodInvocationHighlighting extends AbstractConfigurableHighlighting {

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return STATIC_METHOD_INVOCATION;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return RGB_BLACK;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_staticMethodInvocation; 
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTName) {
				IASTName name= (IASTName)node;
				if (name instanceof ICPPASTQualifiedName) {
					return false;
				}
				if (!name.isReference()) {
					return false;
				}
				IBinding binding= token.getBinding();
				if (binding instanceof ICPPMethod && !(binding instanceof IProblemBinding)) {
					return ((ICPPMethod)binding).isStatic();
				}
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for methods.
	 */
	private static final class MethodHighlighting extends AbstractConfigurableHighlighting {

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return METHOD;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return RGB_BLACK;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_method; 
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTImplicitName)
				return false;
			if (node instanceof IASTName) {
				IASTName name= (IASTName)node;
				if (name instanceof ICPPASTQualifiedName && name.isReference()) {
					return false;
				}
				IBinding binding= token.getBinding();
				if (binding instanceof ICPPMethod) {
					return true;
				}
			}
			return false;
		}

	}

	/**
	 * Semantic highlighting for function declarations.
	 */
	private static final class FunctionDeclarationHighlighting extends AbstractConfigurableHighlighting {

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return FUNCTION_DECLARATION;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return RGB_BLACK;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_functionDeclaration; 
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTImplicitName)
				return false;

			if (node instanceof IASTName) {
				IASTName name= (IASTName)node;
				if (name.isDeclaration()) {
					IBinding binding= token.getBinding();
					if (binding instanceof IFunction 
							&& !(binding instanceof ICPPMethod)) {
						return true;
					} else if (binding instanceof IProblemBinding) {
						// try to derive from AST
						if (name instanceof ICPPASTQualifiedName) {
							return false;
						}
						node= name.getParent();
						while (node instanceof IASTName) {
							node= node.getParent();
						}
						if (node instanceof IASTFunctionDeclarator) {
							while (node != token.getRoot() && !(node.getParent() instanceof IASTDeclSpecifier)) {
								node= node.getParent();
							}
							if (node instanceof ICPPASTCompositeTypeSpecifier) {
								return false;
							}
							return true;
						}
					}
				}
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for functions.
	 */
	private static final class FunctionHighlighting extends AbstractConfigurableHighlighting {

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return FUNCTION;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return RGB_BLACK;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_function; 
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTImplicitName)
				return false;
			if (node instanceof IASTName) {
				IASTName name= (IASTName)node;
				if (name instanceof ICPPASTQualifiedName && name.isReference()) {
					return false;
				}
				IBinding binding= token.getBinding();
				if (binding instanceof IFunction && !(binding instanceof ICPPMethod)) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Semantic highlighting for local variable declarations.
	 */
	private static final class LocalVariableDeclarationHighlighting extends AbstractConfigurableHighlighting {

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return LOCAL_VARIABLE_DECLARATION;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return new RGB(128, 0, 0);
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_localVariableDeclaration; 
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTName) {
				IASTName name= (IASTName)node;
				if (name.isDeclaration()) {
					IBinding binding= token.getBinding();
					if (binding instanceof IVariable
							&& !(binding instanceof IField)
							&& !(binding instanceof IParameter)
							&& !(binding instanceof IProblemBinding)) {
						try {
							IScope scope= binding.getScope();
							if (LocalVariableHighlighting.isLocalScope(scope)) {
								return true;
							}
						} catch (DOMException exc) {
							CUIPlugin.log(exc);
						} 
					}
				}
			}
			return false;
		}

}

	/**
	 * Semantic highlighting for local variables.
	 */
	private static final class LocalVariableHighlighting extends AbstractConfigurableHighlighting {

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return LOCAL_VARIABLE;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return RGB_BLACK;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_localVariable; 
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTName) {
				IASTName name= (IASTName)node;
				if (name.isReference()) {
					IBinding binding= token.getBinding();
					if (binding instanceof IVariable
							&& !(binding instanceof IField)
							&& !(binding instanceof IParameter)
							&& !(binding instanceof IProblemBinding)) {
						try {
							IScope scope= binding.getScope();
							if (isLocalScope(scope)) {
								return true;
							}
						} catch (DOMException exc) {
							CUIPlugin.log(exc);
						} 
					}
				}
			}
			return false;
		}

	    public static boolean isLocalScope(IScope scope) {
	        while (scope != null) {
	            if (scope instanceof ICPPFunctionScope ||
	                    scope instanceof ICPPBlockScope ||
	                    scope instanceof ICFunctionScope) {
	                return true;
	            }
	            try {
	                scope= scope.getParent();
	            } catch (DOMException e) {
	                scope= null;
	            }
	        }
	        return false;
	    }
}

	/**
	 * Semantic highlighting for global variables.
	 */
	private static final class GlobalVariableHighlighting extends AbstractConfigurableHighlighting {

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return GLOBAL_VARIABLE;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return RGB_BLACK;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_globalVariable; 
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTName) {
				IASTName name= (IASTName)node;
				if (name instanceof ICPPASTQualifiedName) {
					return false;
				}
				IBinding binding= token.getBinding();
				if (binding instanceof IVariable
						&& !(binding instanceof IField)
						&& !(binding instanceof IParameter)
						&& !(binding instanceof ICPPTemplateNonTypeParameter)
						&& !(binding instanceof IProblemBinding)) {
					try {
						IScope scope= binding.getScope();
						if (!LocalVariableHighlighting.isLocalScope(scope)) {
							return true;
						}
					} catch (DOMException exc) {
						CUIPlugin.log(exc);
					} 
				}
			}
			return false;
		}
		
	}

	/**
	 * Semantic highlighting for parameter variables.
	 */
	private static final class ParameterVariableHighlighting extends AbstractConfigurableHighlighting {

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return PARAMETER_VARIABLE;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return RGB_BLACK;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_parameterVariable; 
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding= token.getBinding();
			if (binding instanceof IParameter) {
				return true;
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for template parameters.
	 */
	private static final class TemplateParameterHighlighting extends AbstractConfigurableHighlighting {

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return TEMPLATE_PARAMETER;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return new RGB(100, 70, 50);
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_templateParameter; 
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTName) {
				IBinding binding= token.getBinding();
				if (binding instanceof ICPPTemplateParameter) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Semantic highlighting for classes.
	 */
	private static final class ClassHighlighting extends AbstractConfigurableHighlighting {

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return CLASS;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return new RGB(0, 80, 50);
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_classes; 
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof ICPPASTQualifiedName || node instanceof ICPPASTTemplateId) {
				return false;
			}
			if (node instanceof IASTName) {
				IBinding binding= token.getBinding();
				if (binding instanceof ICPPClassType && !(binding instanceof ICPPTemplateParameter)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for enums.
	 */
	private static final class EnumHighlighting extends AbstractConfigurableHighlighting {
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return ENUM;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return new RGB(100, 70, 50);
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_enums; 
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTName) {
				IBinding binding= token.getBinding();
				if (binding instanceof IEnumeration) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Semantic highlighting for macro references.
	 */
	private static final class MacroReferenceHighlighting extends AbstractConfigurableHighlighting {
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return MACRO_REFERENCE;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return RGB_BLACK;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_macroSubstitution; 
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding= token.getBinding();
			if (binding instanceof IMacroBinding) {
				IASTName name= (IASTName)token.getNode();
				if (name.isReference()) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Semantic highlighting for macro definitions.
	 */
	private static final class MacroDefinitionHighlighting extends AbstractConfigurableHighlighting {
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return MACRO_DEFINITION;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return RGB_BLACK;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_macroDefintion; 
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding= token.getBinding();
			if (binding instanceof IMacroBinding) {
				IASTName name= (IASTName)token.getNode();
				if (!name.isReference()) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Semantic highlighting for typedefs.
	 */
	private static final class TypedefHighlighting extends AbstractConfigurableHighlighting {
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return TYPEDEF;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return new RGB(0, 80, 50);
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return true;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_typeDef; 
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTName) {
				IASTName name= (IASTName)node;
				if (name instanceof ICPPASTQualifiedName) {
					return false;
				}
				IBinding binding= token.getBinding();
				if (binding instanceof ITypedef) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Semantic highlighting for namespaces.
	 */
	private static final class NamespaceHighlighting extends AbstractConfigurableHighlighting {
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return NAMESPACE;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return RGB_BLACK;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_namespace; 
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding= token.getBinding();
			if (binding instanceof ICPPNamespace) {
				return true;
			}
			return false;
		}
	}
	
	/**
	 * Semantic highlighting for labels.
	 */
	private static final class LabelHighlighting extends AbstractConfigurableHighlighting {
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return LABEL;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return RGB_BLACK;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_label; 
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding= token.getBinding();
			if (binding instanceof ILabel) {
				return true;
			}
			return false;
		}
	}
	
	/**
	 * Semantic highlighting for enumerators.
	 */
	private static final class EnumeratorHighlighting extends AbstractConfigurableHighlighting {
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return ENUMERATOR;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return new RGB(0, 0, 192);
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return true;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return true;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_enumerator; 
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTName) {
				IASTName name= (IASTName)node;
				if (name instanceof ICPPASTQualifiedName) {
					return false;
				}
				IBinding binding= token.getBinding();
				if (binding instanceof IEnumerator) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Semantic highlighting for problems.
	 */
	private static final class ProblemHighlighting extends AbstractConfigurableHighlighting {
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return PROBLEM;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return new RGB(224, 0, 0);
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return true;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isStrikethroughByDefault()
		 */
		@Override
		public boolean isStrikethroughByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_problem; 
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTProblem) {
				return true;
			}
			IBinding binding= token.getBinding();
			if (binding instanceof IProblemBinding) {
				return true;
			}
			return false;
		}
	}
	
	/**
	 * Semantic highlighting for external SDK references.
	 */
	private static final class ExternalSDKHighlighting extends AbstractConfigurableHighlighting {
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return EXTERNAL_SDK;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return new RGB(100, 40, 128);
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return true;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isStrikethroughByDefault()
		 */
		@Override
		public boolean isStrikethroughByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return true;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_externalSDK; 
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node= token.getNode();
			if (node instanceof IASTName) {
				IASTName name= (IASTName)node;
				if (name instanceof ICPPASTQualifiedName) {
					return false;
				}
				if (name instanceof IASTImplicitName) {
					return false;
				}
				if (name.isReference()) {
					IBinding binding= token.getBinding();
					IIndex index= token.getRoot().getIndex();
					return isExternalSDKReference(binding, index);
				}
			}
			return false;
		}

		private boolean isExternalSDKReference(IBinding binding, IIndex index) {
			if (binding instanceof IFunction) {
				try {
					if (binding instanceof IIndexBinding) {
						if (((IIndexBinding) binding).isFileLocal()) {
							return false;
						}
					}
					else if (!(binding instanceof ICExternalBinding)) {
						return false;
					}
					IIndexName[] decls= index.findNames(binding, IIndex.FIND_DECLARATIONS | IIndex.SEARCH_ACROSS_LANGUAGE_BOUNDARIES);
					for (IIndexName decl : decls) {
						IIndexFile indexFile= decl.getFile();
						if (indexFile != null && indexFile.getLocation().getFullPath() != null) {
							return false;
						}
					}
					if (decls.length != 0) {
						return true;
					}
				} catch (CoreException exc) {
					CUIPlugin.log(exc.getStatus());
					return false;
				}
			}
			return false;
		}
	}
	
	/**
	 * Semantic highlighting for functions.
	 */
	private static final class OverloadedOperatorHighlighting extends AbstractConfigurableHighlighting {

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return OVERLOADED_OPERATOR;
		}

		
		@Override
		public boolean requiresImplicitNames() {
			return true;
		}


		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultTextColor() {
			return new RGB(200, 100, 0); // orange
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return CEditorMessages.SemanticHighlighting_overloadedOperators; 
		}

		/*
		 * @see org.eclipse.cdt.internal.ui.editor.SemanticHighlighting#consumes(org.eclipse.cdt.internal.ui.editor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IASTNode node = token.getNode();
			// so far we only have implicit names for overloaded operators and destructors, so this works
			if (node instanceof IASTImplicitName) {
				IASTImplicitName name = (IASTImplicitName) node;
				if (name.isReference() && name.isOperator()) {
					IBinding binding = name.resolveBinding();
					if (binding instanceof ICPPMethod && !(binding instanceof IProblemBinding)
							&& ((ICPPMethod) binding).isImplicit()) {
						return false;
					}
					if (binding instanceof ICPPUnknownBinding)
						return false;
					char[] chars = name.toCharArray();
					if (chars[0] == '~' || OverloadableOperator.isNew(chars)
							|| OverloadableOperator.isDelete(chars)) {
						return false;
					}
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * @return The semantic highlightings, the order defines the precedence of matches, the first match wins.
	 */
	public static ISemanticHighlighting[] getSemanticHighlightings(ILanguage language) {
		return fHRegistry.getHighlighters(language);
	}

	/**
	 * Initialize default preferences in the given preference store.
	 */
	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED, true);

		IHighlightConfiguration[] configurations= getConfigurableHighlightings();
		for (IHighlightConfiguration configuration : configurations) {
			setDefaults(configuration, store);
		}
	}

	public final static void setDefaults(IHighlightConfiguration configuration, IPreferenceStore store) {
		if (configuration.getColorPreferenceKey() != null) {
			PreferenceConverter.setDefault(store, configuration.getColorPreferenceKey(),
					configuration.getDefaultColor());
		}
		setIfKeyNonNull(store, configuration.getBoldPreferenceKey(), configuration.isBoldByDefault());
		setIfKeyNonNull(store, configuration.getItalicPreferenceKey(), configuration.isItalicByDefault());
		setIfKeyNonNull(store, configuration.getUnderlinePreferenceKey(),
				configuration.isUnderlineByDefault());
		setIfKeyNonNull(store, configuration.getStrikethroughPreferenceKey(),
				configuration.isStrikethroughByDefault());
		setIfKeyNonNull(store, configuration.getEnabledPreferenceKey(), configuration.isEnabledByDefault());
	}

	private static void setIfKeyNonNull(IPreferenceStore store, String key, boolean value) {
		if (key != null) {
			store.setDefault(key, value);
		}
	}
	
	/**
	 * Tests whether <code>event</code> in <code>store</code> affects the
	 * enablement of semantic highlighting.
	 *
	 * @param store the preference store where <code>event</code> was observed
	 * @param event the property change under examination
	 * @return <code>true</code> if <code>event</code> changed semantic
	 *         highlighting enablement, <code>false</code> if it did not
	 */
	public static boolean affectsEnablement(IPreferenceStore store, PropertyChangeEvent event, ILanguage language) {
		if (event.getProperty().equals(PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED)) {
			return true;
		}
		ISemanticHighlighting affectedHighlighting = null;
		ISemanticHighlighting[] highlightings= getSemanticHighlightings(language);
		for (ISemanticHighlighting highlighting : highlightings) {
			if (highlighting.affectsEnablement(event)){
				affectedHighlighting = highlighting;
				break;
				
			}
		}
		if (affectedHighlighting == null)
			return false;

		for (ISemanticHighlighting highlighting : highlightings) {
			if (!highlighting.equals(affectedHighlighting) && highlighting.isEnabled()) {
				return false;
			}
		}

		// all others are disabled, so toggling relevantKey affects the enablement
		return true;
	}

	/**
	 * Tests whether semantic highlighting is currently enabled.
	 *
	 * @param store the preference store to consult
	 * @return <code>true</code> if semantic highlighting is enabled,
	 *         <code>false</code> if it is not
	 */
	public static boolean isEnabled(IPreferenceStore store, ILanguage language) {
		if (!store.getBoolean(PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED)) {
			return false;
		}
		ISemanticHighlighting[] highlightings= getSemanticHighlightings(language);
		boolean enable= false;
		for (ISemanticHighlighting highlighting : highlightings) {
			if (highlighting.isEnabled()) {
				enable= true;
				break;
			}
		}

		return enable;
	}

	/**
	 * Do not instantiate
	 */
	private SemanticHighlightings() {
	}

	public static AbstractConfigurableHighlighting[] getBuiltinSemanticHighlightings() {
		if (fgSemanticHighlightings == null)
			fgSemanticHighlightings= new AbstractConfigurableHighlighting[] {
				new MacroReferenceHighlighting(),  // before all others!
				new ProblemHighlighting(),
				new ExternalSDKHighlighting(),
				new ClassHighlighting(),
				new StaticFieldHighlighting(),
				new FieldHighlighting(),  // after all other fields
				new MethodDeclarationHighlighting(),
				new StaticMethodInvocationHighlighting(),
				new ParameterVariableHighlighting(),  // before local variables
				new LocalVariableDeclarationHighlighting(),
				new LocalVariableHighlighting(),
				new GlobalVariableHighlighting(),
				new TemplateParameterHighlighting(), // before template arguments!
				new OverloadedOperatorHighlighting(), // before both method and function
				new MethodHighlighting(), // before types to get ctors
				new EnumHighlighting(),
				new MacroDefinitionHighlighting(),
				new FunctionDeclarationHighlighting(),
				new FunctionHighlighting(),
				new TypedefHighlighting(),
				new NamespaceHighlighting(),
				new LabelHighlighting(),
				new EnumeratorHighlighting(),
		};
		return fgSemanticHighlightings;
	}

	public static IHighlightConfiguration[] getConfigurableHighlightings() {
		return fHRegistry.getConfigurableHighlighters();
	}
}
