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

import net.sourceforge.transmogrify.symtab.parser.SymTabAST;
import java.io.*;


// $Id: SynchronizedPrinter.java 1014 2001-11-30 03:28:10Z ajp $

/**
 * prints an AST node of type JavaTokenTypes.LITERAL_synchronized
 */

public class SynchronizedPrinter extends ChildIgnoringPrinter {
  public SynchronizedPrinter(SymTabAST nodeToPrint) {
    super(nodeToPrint);
  }

  /**
   * prints an AST node of type JavaTokenTypes.LITERAL_synchronized
   * to the printer.  There are two flavors of this node:<br>
   * <br>
   * the node is a modifier to a method<br>
   * the node is a block synchronized on an object<br>
   */
  public void printSelf(PrettyPrinter printout) throws IOException {
    SymTabAST expr = (SymTabAST)nodeToPrint.getFirstChild();

    if (expr != null) {
      SymTabAST rest = (SymTabAST)expr.getNextSibling();
      printout.print(nodeToPrint.getText());
      printPreBlockExpression(printout);
      printOpenParen(printout);
      PrinterFactory.makePrinter(expr).print(printout);
      printCloseParen(printout);
      PrinterFactory.makePrinter(rest).print(printout);
    }
    else {
      printout.print(nodeToPrint.getText() + " ");
    }
  }
}
