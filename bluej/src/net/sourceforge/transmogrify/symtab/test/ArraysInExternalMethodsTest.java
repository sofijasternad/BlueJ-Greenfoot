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
package net.sourceforge.transmogrify.symtab.test;

import net.sourceforge.transmogrify.symtab.*;
import java.io.File;

public class ArraysInExternalMethodsTest extends DefinitionLookupTest {

  private File file;

  public ArraysInExternalMethodsTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    file = new File("test/symtab/ArraysInExternalMethods.java");
    createQueryEngine(new File[] { file });
  }

  public void testExactType() {
    IDefinition ref = getDefinition(file, "main", 9, 30);
    assertNotNull("Reference not created.", ref);
  }

  public void testCompatibleType() {
    IDefinition ref = getDefinition(file, "toArray", 14, 12);
    assertNotNull("Reference not created.", ref);
  }

  public void testCompatibleSourcedType() {
    IDefinition ref = getDefinition(file, "toArray", 18, 12);
    assertNotNull("Reference not created.", ref);
  }

  public void testPrimitiveType() {
    IDefinition ref = getDefinition(file, "toArray", 21, 12);
    assertNotNull("Reference not created.", ref);
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.run(ArraysInExternalMethodsTest.class);
  }

}
