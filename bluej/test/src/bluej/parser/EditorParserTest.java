/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.parser;

import java.io.File;
import java.util.Properties;

import javax.swing.text.BadLocationException;

import junit.framework.TestCase;
import bluej.Boot;
import bluej.Config;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

public class EditorParserTest extends TestCase
{
    {
        File bluejLibDir = Boot.getBluejLibDir();
        Config.initialise(bluejLibDir, new Properties(), false);
    }
    
    /**
     * Generate a compilation unit node based on some source code.
     */
    private ParsedCUNode cuForSource(String sourceCode)
    {
        MoeSyntaxDocument document = new MoeSyntaxDocument();
        try {
            document.insertString(0, sourceCode, null);
        }
        catch (BadLocationException ble) {}
        return document.getParser();
    }

    public void test1()
    {
        String sourceCode = ""
            + "class A\n"       // position 0
            + "{\n"             // position 8 
            + "   class B\n"    // position 10 
            + "    {\n"         // position 21 
            + "    }\n"
            + "}\n";
            
        ParsedCUNode pcuNode = cuForSource(sourceCode);
        NodeAndPosition classNP = pcuNode.findNodeAtOrAfter(0, 0);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNP.getNode().getNodeType());
        assertEquals(0, classNP.getPosition());
        
        NodeAndPosition innerNP = classNP.getNode().findNodeAtOrAfter(9, 0);
        
        NodeAndPosition classBNP = innerNP.getNode().findNodeAtOrAfter(innerNP.getPosition(),
                innerNP.getPosition());
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classBNP.getNode().getNodeType());
        assertEquals(13, classBNP.getPosition());
    }
}
