/*
Copyright (C) 2001  ThoughtWorks, Inc

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
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package net.sourceforge.transmogrify.symtab.printer;

import net.sourceforge.transmogrify.symtab.parser.*;
import java.io.*;


// $Id: ConstructorDefPrinter.java 1014 2001-11-30 03:28:10Z ajp $

/**
 * prints a constructor definition
 */

public class ConstructorDefPrinter extends ChildIgnoringPrinter {
  public ConstructorDefPrinter(SymTabAST nodeToPrint)
  {
    super(nodeToPrint);
  }

  /**
  * prints the constructor definition to the PrettyPrinter
  *
  * @param printout the PrettyPrinter you would like to print to
  */
  public void printSelf(PrettyPrinter printout) throws IOException
  {
    SymTabAST modifiers = nodeToPrint.getFirstChildOfType(JavaTokenTypes.MODIFIERS);
    SymTabAST ident = nodeToPrint.getFirstChildOfType(JavaTokenTypes.IDENT);
    SymTabAST params = nodeToPrint.getFirstChildOfType(JavaTokenTypes.PARAMETERS);
    SymTabAST sList = nodeToPrint.getFirstChildOfType(JavaTokenTypes.SLIST);

    PrinterFactory.makePrinter(modifiers).print(printout);
    PrinterFactory.makePrinter(ident).print(printout);
    printOpenParen(printout);
    PrinterFactory.makePrinter(params).print(printout);
    printCloseParen(printout);
    PrinterFactory.makePrinter(sList).print(printout);
    newline(printout);
  }
}
