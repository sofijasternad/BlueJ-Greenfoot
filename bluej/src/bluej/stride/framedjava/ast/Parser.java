/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.stride.framedjava.ast;

import java.io.StringReader;
import java.util.List;

import bluej.parser.JavaParser;
import bluej.parser.ParseFailure;
import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.stride.framedjava.elements.CodeElement;

public class Parser
{

    public static boolean parseableAsType(String s)
    {
        JavaParser p = new JavaParser(new StringReader(s), false);
        try
        {
            // TODO not sure this handles multidim arrays with a size
            p.parseTypeSpec(true);
            
            // Only valid if we have parsed all the way to end of the String:
            LocatableToken tok = p.getTokenStream().nextToken();
            if (tok.getType() != JavaTokenTypes.EOF)
                return false;
            
            return true;
        }
        catch (ParseFailure pf)
        {
            return false;
        }
    }
    
    // Checks if it can be parsed as the part following "import " and before the semi-colon
    public static boolean parseableAsImportTarget(String s)
    {
        JavaParser p = new JavaParser(new StringReader("import " + s + ";"), false);
        try
        {
            p.parseImportStatement();
            
            // Only valid if we have parsed all the way to end of the String:
            
            LocatableToken tok = p.getTokenStream().nextToken();
            if (tok.getType() != JavaTokenTypes.EOF)
                return false;
            
            return true;
        }
        catch (ParseFailure pf)
        {
            return false;
        }
    }

    private static final String DUMMY_STEM = "code__dummy__gf3gen__";

    public static class DummyNameGenerator
    {
        private int index = 0;

        public String generateNewDummyName()
        {
            return DUMMY_STEM + (index++);
        }
    }
    
    public static boolean isDummyName(String name)
    {
        return name.startsWith(DUMMY_STEM);
    }

    public static boolean parseableAsNameDef(String s)
    {
        // We don't need to parse, just lex and see if it comes out as an ident token:
        JavaLexer lexer = new JavaLexer(new StringReader(s));
        LocatableToken t = lexer.nextToken();
        LocatableToken t2 = lexer.nextToken();
        if (t.getType() == JavaTokenTypes.IDENT && t2.getType() == JavaTokenTypes.EOF)
            return true;
        else
            return false;
    }

    public static boolean parseableAsExpression(String e)
    {
        return Parser.parseAsExpression(new JavaParser(new StringReader(e), false));
    }
    
    /**
     *  Tries to run the given parser by calling parseExpression.
     * Any ParseFailure exceptions are caught and false is returned.
     * If there is no exception, but after parsing we are not at EOF
     * then false is also returned.
     * true is only returned if there is no ParseFailure, and we parse
     * all the way to EOF
     */
    public static boolean parseAsExpression(JavaParser p)
    {
        try
        {
            p.parseExpression();
            
            // Only valid if we have parsed all the way to end of the String:
            
            LocatableToken tok = p.getTokenStream().nextToken();
            if (tok.getType() != JavaTokenTypes.EOF)
                return false; 
        }
        catch (ParseFailure pf)
        {
            //Debug.message("Invalid expression: " + pf.getMessage());
            return false;
        }
        return true;
        
    }
    
    public static enum JavaContext
    {
        /** An item within a method (or similar) */
        STATEMENT,
        /** An item within a *class* body (field, method, constructor) */
        CLASS_MEMBER,
        /** Ditto but for interfaces */
        INTERFACE_MEMBER,
        /** Top-level of file; package, imports and a declaration */
        TOP_LEVEL
        //TODO: should there be another context for case statements?
    }
    

    public static List<CodeElement> javaToStride(String java, JavaContext context) throws ParseFailure
    {
        JavaStrideParser parser;
        switch (context)
        {
            case CLASS_MEMBER:
                parser = new JavaStrideParser(java + "}");
                parser.parseClassBody();
                break;
            case STATEMENT:
                parser = new JavaStrideParser(java);
                parser.parseStatement();
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return parser.getCodeElements();
    }


}
