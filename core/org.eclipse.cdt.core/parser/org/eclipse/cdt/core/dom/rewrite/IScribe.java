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

/**
 * Rewrites DOM.
 */
public interface IScribe {
	/**
	 * @param string to be print
	 */
	void print(String string);
	/**
	 * Outputs single space
	 */
	void printSpace();
	/**
	 * @param string to print
	 */
	void print(char[] string);
	
	/**
	 * Outputs newline
	 */
	void newLine();
	/**
	 * @param c character to print
	 */
	void print(char c);
	/**
	 * @param string to be printed with space appended
	 */
	void printStringSpace(String string);
	/**
	 * Increments indentation level
	 */
	void incrementIndentationLevel();
	/**
	 * Decrements indentation level
	 */
	void decrementIndentationLevel();
	/**
	 * Prints semicolon if semicolon printing is enabled. If it is disabled
	 * this call will enable it but still print nothing.
	 */
	void printSemicolon();
	/**
	 * Disables printing newlines
	 */
	void noNewLines();
	/**
	 * Enables newlines printing
	 */
	void newLines();
	
	/**
	 * Enables newlines printing
	 */
	void newLine(int count);
	/**
	 * @param count number of spaces to print
	 */
	void printSpaces(int count);
	/**
	 * Prevents next call to {@link IScribe#printSemicolon()} from outputting semicolon.
	 */
	void noSemicolon();
	/**
	 * Prints left brace ({) and increases indentation level
	 */
	void printLBrace();
	/**
	 * Prints right brace (}) and decreases indentation level
	 */
	void printRBrace();
	/**
	 * @param code line to print followed by newline
	 */
	void println(String code);
	
}
