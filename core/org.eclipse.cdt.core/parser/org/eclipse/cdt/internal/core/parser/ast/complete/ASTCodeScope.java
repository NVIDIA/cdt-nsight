/*
 * Created on Sep 2, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.eclipse.cdt.internal.core.parser.ast.complete;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.cdt.core.parser.ISourceElementRequestor;
import org.eclipse.cdt.core.parser.ast.IASTCodeScope;
import org.eclipse.cdt.core.parser.ast.IASTDeclaration;
import org.eclipse.cdt.internal.core.parser.pst.IContainerSymbol;

/**
 * @author jcamelon
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ASTCodeScope extends ASTScope implements IASTCodeScope {

	private List declarations = new ArrayList();	
	private final IASTCodeScope ownerCodeScope;

    /**
	 * @param newScope
	 */
	public ASTCodeScope(IContainerSymbol newScope) {
		super( newScope );
		ownerCodeScope = ( newScope.getContainingSymbol().getASTExtension().getPrimaryDeclaration() instanceof IASTCodeScope ) ? 
			(IASTCodeScope) newScope.getContainingSymbol().getASTExtension().getPrimaryDeclaration() : null; 
	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.ISourceElementCallbackDelegate#acceptElement(org.eclipse.cdt.core.parser.ISourceElementRequestor)
	 */
	public void acceptElement(ISourceElementRequestor requestor) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.ISourceElementCallbackDelegate#enterScope(org.eclipse.cdt.core.parser.ISourceElementRequestor)
	 */
	public void enterScope(ISourceElementRequestor requestor) {
		try
        {
            requestor.enterCodeBlock( this );
        }
        catch (Exception e)
        {
            /* do nothing */
        }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.ISourceElementCallbackDelegate#exitScope(org.eclipse.cdt.core.parser.ISourceElementRequestor)
	 */
	public void exitScope(ISourceElementRequestor requestor) {
		try
        {
            requestor.exitCodeBlock( this );
        }
        catch (Exception e)
        {
            /* do nothing */
        }
	}

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.parser.ast.IASTCodeScope#getOwnerCodeScope()
     */
    public IASTCodeScope getOwnerCodeScope()
    {
        return ownerCodeScope;
    }
    
    public Iterator getDeclarations()
    {
    	return declarations.iterator();
    }
    
    public void addDeclaration(IASTDeclaration declaration)
    {
    	declarations.add(declaration);
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.ast.IASTNode#lookup(java.lang.String, org.eclipse.cdt.core.parser.ast.IASTNode.LookupKind)
	 */
	public LookupResult lookup(String prefix, LookupKind kind) {
		// TODO Auto-generated method stub
		return null;
	}

}
