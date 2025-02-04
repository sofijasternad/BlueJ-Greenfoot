/*
 This file is part of the BlueJ program.
 Copyright (C) 2023  Michael Kolling and John Rosenberg

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
package bluej.extmgr;

import bluej.extensions2.MenuGenerator;
import javafx.scene.control.MenuItem;

import java.util.Collections;
import java.util.List;

/**
 * An adaptation of ExtensionMenu that only supports single menu items
 */
public interface ExtensionMenuSingle extends ExtensionMenu
{
    /**
     * Calls the extension to get a menu item.
     *
     * @param menuGenerator
     *            The {@link MenuGenerator} which creates the menu.
     * @return The {@link MenuItem} the extension provides or <code>null</code>
     *         if it does not provide a menu entry.
     */
    MenuItem getMenuItem(MenuGenerator menuGenerator);

    /**
     * Post a notification about a menu going to be displayed.
     *
     * @param menuGenerator
     *            The {@link MenuGenerator} which creates the menu.
     * @param onThisItem
     *            The {@link MenuItem} which is about to show.
     */
    void postMenuItem(MenuGenerator menuGenerator, MenuItem onThisItem);

    // Default implementation to work with a single menu item.  Do not override.
    @Override
    default List<MenuItem> getMenuItems(MenuGenerator menuGenerator)
    {
        return Collections.singletonList(getMenuItem(menuGenerator));
    }

    // Default implementation to work with a single menu item.  Do not override.
    @Override
    default void postMenuItems(MenuGenerator menuGenerator, List<MenuItem> onThisItem)
    {
        postMenuItem(menuGenerator, onThisItem.get(0));
    }
}
