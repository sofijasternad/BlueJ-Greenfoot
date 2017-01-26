/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2015,2016,2017  Michael Kolling and John Rosenberg 

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
package bluej.prefmgr;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import bluej.BlueJTheme;
import bluej.utility.Utility;
import bluej.utility.javafx.SwingNodeFixed;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingNode;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import bluej.Config;
import bluej.classmgr.ClassMgrPrefPanel;
import bluej.editor.moe.EditorPrefPanel;
import bluej.editor.moe.KeyBindingsPanel;
import bluej.extmgr.ExtensionPrefManager;
import bluej.extmgr.ExtensionsManager;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A JDialog subclass to allow the user to interactively edit
 * preferences.
 *
 * <p>A singleton.
 *
 * @author  Andrew Patterson
 * @author  Michael Kolling
 */
public class PrefMgrDialog
{
    private static PrefMgrDialog dialog = null;
    
    /**
     * Creating the preference panes requires thread-hopping.  This property
     * is set to true (on the FXPlatform thread) once they are ready.
     */
    @OnThread(Tag.FXPlatform)
    private BooleanProperty prefPanesCreated = new SimpleBooleanProperty(false);
    
    /** Indicates whether the dialog has been prepared for display. */
    private boolean prepared = false;

    /**
     * Show the preferences dialog when ready.  The dialog
     * may not be visible yet when this method returns.
     */
    public static void showDialog()
    {
        getInstance().prepareDialogThen(() -> {
            dialog.window.show();
            // Work around bug where every other time dialog is shown, it would have wrong size:
            dialog.tabbedPane.getScene().getWindow().sizeToScene();
        });
    }

    /**
     * Show the preferences dialog when ready.  The dialog
     * may not be visible yet when this method returns.
     * 
     * @param paneNumber The index of the pane to show
     */
    public static void showDialog(int paneNumber) {
        getInstance().prepareDialogThen(() -> {
            dialog.selectTab(paneNumber);
            dialog.window.show();
            // Work around bug where every other time dialog is shown, it would have wrong size:
            dialog.tabbedPane.getScene().getWindow().sizeToScene();
        });
    }
    /**
     * Prepare this dialog for display then run the given action.
     */
    @OnThread(Tag.FXPlatform)
    private void prepareDialogThen(FXPlatformRunnable runnable)
    {
        if (!prepared)
        {
            if (prefPanesCreated.get())
                makeDialog();
            else
            {
                // Will only get called when it becomes true:
                JavaFXUtil.addSelfRemovingListener(prefPanesCreated, b -> JavaFXUtil.runNowOrLater(() -> prepareDialogThen(runnable))); 
                return;
            }
            prepared = true;
        }
        dialog.startEditing();
        runnable.run();
    }

    /**
     * Returns the current instance of the dialog, can be null.
     * @return the current instance of the dialog, can be null.
     */
    public static final PrefMgrDialog getInstance ()
    {
        if (dialog == null) {
            dialog = new PrefMgrDialog();
        }
        return dialog;
    }


    private ArrayList<PrefPanelListener> listeners = new ArrayList<PrefPanelListener>();
    private ArrayList<Node> tabs = new ArrayList<>();
    private ArrayList<String> titles = new ArrayList<String>();

    private Dialog<Void> window;
    private TabPane tabbedPane = null;
    
    /**
     * Setup the UI for the dialog and event handlers for the dialog's buttons.
     *
     * @param title the title of the dialog
     */
    private PrefMgrDialog()
    {
        createPrefPanes();
    }

    /**
     * Create all known preference panes.
     */
    private void createPrefPanes()
    {
        // Editor panel is first:
        EditorPrefPanel panel = new EditorPrefPanel();
        add(0, panel, Config.getString("prefmgr.edit.prefpaneltitle"), panel);
        // Misc will be third, for now is second:
        MiscPrefPanel panel2 = new MiscPrefPanel();
        add(1, panel2, Config.getString("prefmgr.misc.prefpaneltitle"), panel2);
        // Interface will be fourth, for now is third:
        InterfacePanel panel3 = new InterfacePanel();
        add(2, panel3, Config.getString("prefmgr.interface.title"), panel3);
        // Libraries will be fifth, for now is fourth:
        ClassMgrPrefPanel userConfigLibPanel = new ClassMgrPrefPanel();
        add(3, userConfigLibPanel, Config.getString("classmgr.prefpaneltitle"), userConfigLibPanel);

        SwingUtilities.invokeLater(() -> {
            SwingNode kbSwing = new SwingNodeFixed();
            KeyBindingsPanel kbPanel = new KeyBindingsPanel(() -> null);
            kbSwing.setContent(kbPanel.makePanel());
            Platform.runLater(() -> {
                // Now we can insert the keybindings panel as second:
                add(1, kbSwing, Config.getString("prefmgr.edit.keybindingstitle"), kbPanel);
            });
            if (!Config.isGreenfoot())
            {
                SwingNode extSwing = new SwingNodeFixed();
                ExtensionPrefManager mgr = ExtensionsManager.getInstance().getPrefManager();
                extSwing.setContent(mgr.getPanel());
                Platform.runLater(() -> {
                    // Extensions is sixth:
                    add(5, extSwing, Config.getString("extmgr.extensions"), mgr);
                });
                if (Config.isRaspberryPi())
                {
                    //display the Raspberry Pi specific options
                    RaspberryPiPanel rpp = new RaspberryPiPanel();
                    SwingNode piSwing = new SwingNodeFixed();
                    piSwing.setContent(rpp);
                    Platform.runLater(() -> {
                        // Pi is seventh:
                        add(6, piSwing, Config.getString("extmgr.raspberryPi.rpiPanelTitle"), rpp);
                    });
                }
            }
            Platform.runLater(() -> prefPanesCreated.set(true));
        });
    }
    
    /**
     * Register a panel to be shown in the preferences dialog
     *
     * @param panel     the panel to add
     * @param title     a string describing the panel
     * @param listener  an object which will be notified of events concerning the
     *                  preferences dialog
     */
    public void add(int index, Node panel, String title, PrefPanelListener listener)
    {
        tabs.add(index, panel);
        // Listener order doesn't really matter, but let's go with it anyway:
        listeners.add(index, listener);
        titles.add(index, title);
    }

    private void startEditing()
    {
        for (Iterator<PrefPanelListener> i = listeners.iterator(); i.hasNext(); ) {
            PrefPanelListener ppl = i.next();
            ppl.beginEditing();
        }        
    }

    private void selectTab(int tabNumber)
    {
        tabbedPane.getSelectionModel().select(tabNumber);
    }

    private void makeDialog()
    {
        window = new Dialog<>();
        BlueJTheme.setWindowIconFX(window);
        window.setTitle(Config.getApplicationName() + ": " + Config.getString("prefmgr.title"));
        Config.addDialogStylesheets(window.getDialogPane());
        JavaFXUtil.addStyleClass(window.getDialogPane(), "prefmgr-dialog-pane");
        window.setOnShown(e -> Utility.bringToFrontFX(window.getDialogPane().getScene().getWindow()));
        
        window.setResizable(true);

        tabbedPane = new TabPane();
        tabbedPane.setPrefHeight(550.0);
        JavaFXUtil.addStyleClass(tabbedPane, "prefmgr-tab-pane");

        //window.setOnShown(e -> org.scenicview.ScenicView.show(tabbedPane.getScene()));

        for (ListIterator<Node> i = tabs.listIterator(); i.hasNext(); ) {
            int index = i.nextIndex();
            Node p = i.next();
            Tab tab = new Tab(titles.get(index), p);
            tab.setClosable(false);
            tabbedPane.getTabs().add(tab);
        }

        window.setResultConverter(bt -> {
            if (bt == ButtonType.OK)
            {
                for (Iterator<PrefPanelListener> i = listeners.iterator(); i.hasNext(); )
                {
                    PrefPanelListener ppl = i.next();
                    ppl.commitEditing();
                }
                return null;
            }
            else
            {
                for (Iterator<PrefPanelListener> i = listeners.iterator(); i.hasNext(); ) {
                    PrefPanelListener ppl = i.next();
                    ppl.revertEditing();
                }
            }
            return null;
        });
        window.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        window.getDialogPane().setContent(tabbedPane);
    }
    
    public static Node headedVBox(String titleID, List<Node> contents)
    {
        return headedVBoxTranslated(Config.getString(titleID), contents);
    }

    public static Node headedVBoxTranslated(String title, List<Node> contents)
    {
        VBox body = new VBox();
        body.getChildren().setAll(contents);
        JavaFXUtil.addStyleClass(body, "prefmgr-titled-content");
        TitledPane titledPane = new TitledPane(title, body);
        return JavaFXUtil.withStyleClass(titledPane, "prefmgr-titled");
    }

    public static Node labelledItem(String labelID, Node item)
    {
        return labelledItem(new Label(Config.getString(labelID)), item);
    }
    public static Node labelledItem(Label label, Node item)
    {
        return JavaFXUtil.withStyleClass(new HBox(label, item), "prefmgr-label-hbox");
    }
    
    public static Node wrappedLabel(String content)
    {
        return new TextFlow(JavaFXUtil.withStyleClass(new Text(content), "prefmgr-text-wrapped"));
    }
}
