package bluej.groupwork;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.netbeans.lib.cvsclient.command.FileInfoContainer;
import org.netbeans.lib.cvsclient.command.status.StatusInformation;
import org.netbeans.lib.cvsclient.event.FileInfoEvent;


/**
 * This class is used for registering and storing cvs status request information. 
 *
 * @author bquig
 * @version $Id: StatusServerResponse.java 4704 2006-11-27 00:07:19Z bquig $
 */
public class StatusServerResponse extends BasicServerResponse
{
    private List infoEvents;

    /**
     * Creates a new instance of StatusServerResponse
     */
    public StatusServerResponse()
    {
        infoEvents = new ArrayList();
    }

    /**
     * Keep each status info container so that it can be queried and used
     */
    public void fileInfoGenerated(FileInfoEvent infoEvent)
    {
        FileInfoContainer info = infoEvent.getInfoContainer();
        //StatusInformation statusInfo;

        if (info instanceof StatusInformation) {
            File rfile = info.getFile();
            infoEvents.add(info);
            //statusInfo = (StatusInformation) info;
            //Debug.message("StatusInformation = " + statusInfo);
        }
    }

    public List getStatusInformation()
    {
        return infoEvents;
    }
}
