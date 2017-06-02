/*
 This file is part of the BlueJ program. 
 Copyright (C) 2017  Michael Kolling and John Rosenberg
 
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
package bluej.utility.javafx;

import bluej.Config;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Modality;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An FX dialog containing all repeated to code needed when creating
 * version control dialogs.
 *
 * @author Amjad Altadmri
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public class FXCustomizedDialog<R> extends Dialog<R>
{
    protected FXCustomizedDialog(Window owner, String title, String style)
    {
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle(Config.getString(title));
        setResizable(true);
        setDialogPane(new DialogPane() {
            @Override
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)
            protected Node createButtonBar()
            {
                return wrapButtonBar(super.createButtonBar());
            }
        });
        JavaFXUtil.addStyleClass(this.getDialogPane(), style);
        Config.addDialogStylesheets(getDialogPane());
    }

    // For overriding by subclasses: lets you put other elements to left of button bar
    protected Node wrapButtonBar(Node original)
    {
        return original;
    }

    protected void setModal(boolean makeModal)
    {
        initModality(makeModal ? Modality.APPLICATION_MODAL : Modality.NONE);
    }

    protected void rememberPosition(String locationPrefix)
    {
        setOnShown(e -> Config.rememberPosition(asWindow(), locationPrefix));
    }

    protected void setContentPane(Node content)
    {
        getDialogPane().setContent(content);
    }

    public Window asWindow()
    {
        Scene scene = getDialogPane().getScene();
        if (scene == null)
            return null;
        return scene.getWindow();
    }

    public void setLocationRelativeTo(Node comp)
    {

    }

    @OnThread(Tag.FXPlatform)
    public void setLocationRelativeTo(Window window)
    {

    }

    protected void dialogThenHide(FXPlatformRunnable action)
    {
        action.run();
        hide();
    }
}
