/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2012,2014,2016,2017  Michael Kolling and John Rosenberg

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
package bluej.groupwork.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamViewFilter;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.pkgmgr.Project;
import bluej.utility.FXWorker;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.javafx.JavaFXUtil;

import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Main frame for CVS Status Dialog
 *
 * @author bquig
 */
public class StatusFrame extends FXCustomizedDialog
{
    private Project project;
    private TableView<TeamStatusInfo> statusTable;
    private StatusTableModel statusModel;
    private Button refreshButton;
    private ActivityIndicatorFX progressBar;

    private StatusWorker worker;
    private Repository repository;
    private static final int MAX_ENTRIES = 20;

    /**
     * Creates a new instance of StatusFrame. Called via factory method
     * getStatusWindow.
     */
    public StatusFrame(Project proj)
    {
        project = proj;
        setTitle(Config.getString("team.status"));

        // The layout should be Vertical, if not replace with a VBox.
        getDialogPane().getChildren().addAll(makeMainPane(), makeButtonPanel());
    }

    private ScrollPane makeMainPane()
    {
        // try and set up a reasonable default amount of entries that avoids resizing
        // and scrolling once we get info back from repository
        statusModel = project.getTeamSettingsController().isDVCS() ?
                new StatusTableModelDVCS(project, estimateInitialEntries()) :
                new StatusTableModelNonDVCS(project, estimateInitialEntries());

        //TODO check the next line
        statusTable = new TableView<>(statusModel.getResources());
        //TODO implements the next line
        // statusTable.getTableHeader().setReorderingAllowed(false);



        //set up custom renderer to colour code status message field
//        StatusMessageCellRenderer statusRenderer = new StatusMessageCellRenderer(project);
//        statusTable.setDefaultRenderer(java.lang.Object.class, statusRenderer);
        StatusTableCell cell = new StatusTableCell(project);

        TableColumn<TeamStatusInfo, String> firstColumn = new TableColumn<>(statusModel.getColumnName(0));
        firstColumn.setPrefWidth(70);
        JavaFXUtil.addStyleClass(firstColumn, "team-status-firstColumn");
        firstColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper((String) cell.getValueAt(v.getValue(), 0)));

        TableColumn<TeamStatusInfo, Object> secondColumn = new TableColumn<>(statusModel.getColumnName(1));
        secondColumn.setPrefWidth(40);
        JavaFXUtil.addStyleClass(secondColumn, "team-status-secondColumn");
        secondColumn.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(cell.getValueAt(v.getValue(), 1)));

        TableColumn<TeamStatusInfo, Integer> thirdColumn = new TableColumn<>(statusModel.getColumnName(2));
        thirdColumn.setPrefWidth(60);
        JavaFXUtil.addStyleClass(thirdColumn, "team-status-thirdColumn");
        thirdColumn.setCellValueFactory(v -> new SimpleObjectProperty<>((Integer) cell.getValueAt(v.getValue(), 2)));
//      thirdColumn.setCellFactory(col -> new StatusTableCell(project));

        statusTable.getColumns().setAll(firstColumn, secondColumn, thirdColumn);



        ScrollPane statusScroller = new ScrollPane(statusTable);
//        Dimension prefSize = statusTable.getMaximumSize();
//        Dimension scrollPrefSize =  statusTable.getPreferredScrollableViewportSize();
//        Dimension best = new Dimension(scrollPrefSize.width + 50, prefSize.height + 30);
//        statusScroller.setPreferredSize(best);

        return statusScroller;
    }

    /**
     * Create the button panel with a Resolve button and a close button
     * @return Pane the buttonPanel
     */
    private Pane makeButtonPanel()
    {
//        HBox buttonPanel = new Pane(new FlowLayout(FlowLayout.RIGHT));
        FlowPane buttonPanel = new FlowPane(Orientation.HORIZONTAL);
//        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // progress bar
        progressBar = new ActivityIndicatorFX();
        progressBar.setRunning(false);
        buttonPanel.getChildren().add(progressBar);

        //close button
        Button closeButton = new Button();
        closeButton.setOnAction(event -> {
            if (worker != null) {
                worker.abort();
            }
            hide();
        });

        //refresh button
        refreshButton = new Button(Config.getString("team.status.refresh"));
        refreshButton.setDisable(true);
        refreshButton.setOnAction(event -> update());
        refreshButton.requestFocus();

         buttonPanel.getChildren().addAll(progressBar, refreshButton, closeButton);

        return buttonPanel;
    }

    /**
     * try and estimate the number of entries in status table to avoid resizing
     * once repository has responded.
     */
    private int estimateInitialEntries()
    {
        // Use number of targets + README.TXT
        int initialEntries = project.getFilesInProject(true, false).size() + 1;
        // may need to include diagram layout
        //if(project.includeLayout())
        //    initialEntries++;
        // Limit to a reasonable maximum
        if(initialEntries > MAX_ENTRIES) {
            initialEntries = MAX_ENTRIES;
        }
        return initialEntries;
    }

    /**
     * Refresh the status window.
     */
    public void update()
    {
        repository = project.getRepository();
        if (repository != null) {
            progressBar.setRunning(true);
            refreshButton.setDisable(true);
            worker = new StatusWorker();
            worker.start();
        }
        else {
            hide();
        }
    }

    /**
     * Inner class to do the actual cvs status call to ensure that the UI is not
     * blocked during remote call
     */
    class StatusWorker extends FXWorker implements StatusListener
    {
        ObservableList<TeamStatusInfo> resources;
        TeamworkCommand command;
        TeamworkCommandResult result;
        boolean aborted;
        FileFilter filter = project.getTeamSettingsController().getFileFilter(true);

        public StatusWorker()
        {
            super();
            resources = FXCollections.observableArrayList();
            //Set files = project.getTeamSettingsController().getProjectFiles(true);
            command = repository.getStatus(this, filter, true);
        }

        public void abort()
        {
            command.cancel();
            aborted = true;
        }

        @OnThread(Tag.Unique)
        public Object construct()
        {
            result = command.getResult();
            return resources;
        }

        @OnThread(Tag.Any)
        public void gotStatus(TeamStatusInfo info)
        {
            resources.add(info);
        }

        @OnThread(Tag.Any)
        public void statusComplete(StatusHandle commitHandle)
        {
            // Nothing to be done here.
        }

        public void finished()
        {
            progressBar.setRunning(false);
            if (! aborted) {
                if (result.isError()) {
                    StatusFrame.this.dialogThenHide(() -> TeamUtils.handleServerResponseFX(result, StatusFrame.this.asWindow()));
                }
                else {
                    Collections.sort(resources, new Comparator<TeamStatusInfo>() {
                        public int compare(TeamStatusInfo arg0, TeamStatusInfo arg1)
                        {
                            TeamStatusInfo tsi0 = (TeamStatusInfo) arg0;
                            TeamStatusInfo tsi1 = (TeamStatusInfo) arg1;

                            return tsi1.getStatus() - tsi0.getStatus();
                        }
                    });

                    TeamViewFilter filter = new TeamViewFilter();
                    // Remove old package files from display
                    for (Iterator<TeamStatusInfo> iterator = resources.iterator(); iterator.hasNext();) {
                        TeamStatusInfo info = iterator.next();
                        if(! filter.accept(info)) {
                            iterator.remove();
                        }
                    }
                    statusModel.setStatusData(resources);

                    Map<File, String> statusMap = new HashMap<File, String>();

                    for (TeamStatusInfo s : resources)
                    {
                        statusMap.put(s.getFile(), TeamStatusInfo.getStatusString(s.getStatus()));
                    }

                    DataCollector.teamStatusProject(project, repository, statusMap);
                }
                refreshButton.setDisable(false);
            }
        }
    }
}
