/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork.svn;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.SVNClientInterface;
import org.tigris.subversion.javahl.Status;

import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;

/**
 * A subversion command to commit files.
 * 
 * @author Davin McCall
 */
public class SvnCommitAllCommand extends SvnCommand
{
    protected Set<File> newFiles;
    protected Set<File> binaryNewFiles;
    protected Set<File> deletedFiles;
    protected Set<File> files;
    protected String commitComment;
    
    public SvnCommitAllCommand(SvnRepository repository, Set<File> newFiles, Set<File> binaryNewFiles,
            Set<File> deletedFiles, Set<File> files, String commitComment)
    {
        super(repository);
        this.newFiles = newFiles;
        this.binaryNewFiles = binaryNewFiles;
        this.deletedFiles = deletedFiles;
        this.files = files;
        this.commitComment = commitComment;
    }

    protected TeamworkCommandResult doCommand()
    {
        SVNClientInterface client = getClient();
        
        try {
            // First "svn add" the new files
            Iterator<File> i = newFiles.iterator();
            while (i.hasNext()) {
                File newFile = (File) i.next();
                
                Status status = client.singleStatus(newFile.getAbsolutePath(), false);
                if (! status.isManaged()) {
                    client.add(newFile.getAbsolutePath(), false);
                    if (! newFile.isDirectory()) {
                        client.propertySet(newFile.getAbsolutePath(), "svn:eol-style",
                                "native", false);
                    }
                }
            }
            
            // And binary files
            i = binaryNewFiles.iterator();
            while (i.hasNext()) {
                File newFile = (File) i.next();
                
                Status status = client.singleStatus(newFile.getAbsolutePath(), false);
                if (! status.isManaged()) {
                    client.add(newFile.getAbsolutePath(), false);
                    if (! newFile.isDirectory()) {
                        client.propertySet(newFile.getAbsolutePath(), "svn:mime-type",
                                "application/octet-stream", false);
                    }
                }
            }
            
            // "svn delete" removed files
            i = deletedFiles.iterator();
            while (i.hasNext()) {
                File newFile = (File) i.next();
                client.remove(new String[] {newFile.getAbsolutePath()}, "", true);
            }
            
            // now do the commit
            String [] commitFiles = new String[files.size()];
            i = files.iterator();
            for (int j = 0; j < commitFiles.length; j++) {
                File file = (File) i.next();
                commitFiles[j] = file.getAbsolutePath();
            }
            for (String s : commitFiles) { System.out.println(s); } // DAV
            client.commit(commitFiles, commitComment, false);
            
            if (! isCancelled()) {
                return new TeamworkCommandResult();
            }
        }
        catch (ClientException ce) {
            ce.printStackTrace();
            if (! isCancelled()) {
                return new TeamworkCommandError(ce.getMessage(), ce.getLocalizedMessage());
            }
        }

        return new TeamworkCommandAborted();
    }
}
