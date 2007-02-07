package bluej.groupwork;

import org.netbeans.lib.cvsclient.event.BinaryMessageEvent;
import org.netbeans.lib.cvsclient.event.CVSListener;
import org.netbeans.lib.cvsclient.event.FileAddedEvent;
import org.netbeans.lib.cvsclient.event.FileInfoEvent;
import org.netbeans.lib.cvsclient.event.FileRemovedEvent;
import org.netbeans.lib.cvsclient.event.FileToRemoveEvent;
import org.netbeans.lib.cvsclient.event.FileUpdatedEvent;
import org.netbeans.lib.cvsclient.event.MessageEvent;
import org.netbeans.lib.cvsclient.event.ModuleExpansionEvent;
import org.netbeans.lib.cvsclient.event.TerminationEvent;


/**
 * This class is used by registering it with the EventManager of the client
 * which is to execute a command.
 * client.getEventManager().addCVSListener(updateServerResponse);
 * The BasicServerResponse will record the messages from the server that the
 * server wants the user to see in response to the command. It also offers a way
 * to wait for the command to finish.
 * @author fisker
 *
 */
public class BasicServerResponse implements CVSListener
{
    /**
     * Stores a tagged line
     */
    private final StringBuffer taggedLine = new StringBuffer();
    private StringBuffer message = new StringBuffer();
    boolean isTerminated = false;
    private TerminationEvent terminationEvent;
    private String newline = System.getProperty("line.separator");

    /**
     * Called when the server wants to send a message to be displayed to
     * the user. The message is only for information purposes and clients
     * can choose to ignore these messages if they wish.
     * @param e the event
     */
    public void messageSent(MessageEvent e)
    {
        String line = e.getMessage();
        // PrintStream stream = e.isError() ? System.err : System.out;

        if (e.isTagged()) {
            String message = MessageEvent.parseTaggedMessage(taggedLine, line);

            // if we get back a non-null line, we have something
            // to output. Otherwise, there is more to come and we
            // should do nothing yet.
            if (message != null) {
                this.message.append(message);
                // if (e.isError()) {
                //     System.err.println(message);
                // }
            }
        } else {
            message.append(line + newline);
            // if (e.isError()) {
            //     System.err.println(message);
            // }
        }
    }

    /* (non-Javadoc)
     * @see org.netbeans.lib.cvsclient.event.CVSListener#messageSent(org.netbeans.lib.cvsclient.event.BinaryMessageEvent)
     */
    public void messageSent(BinaryMessageEvent e)
    {
        //System.out.println("BasicServerResponse: 68: " + e.getMessage());
    }

    /* (non-Javadoc)
     * @see org.netbeans.lib.cvsclient.event.CVSListener#fileAdded(org.netbeans.lib.cvsclient.event.FileAddedEvent)
     */
    public void fileAdded(FileAddedEvent arg0)
    {
        //Debug.message("BasicServerResponse: fileAdded: " + arg0.getFilePath());
    }

    /* (non-Javadoc)
     * @see org.netbeans.lib.cvsclient.event.CVSListener#fileRemoved(org.netbeans.lib.cvsclient.event.FileRemovedEvent)
     */
    public void fileRemoved(FileRemovedEvent arg0)
    {
        //Debug.message("BasicServerResponse: fileRemoved: " + arg0.getFilePath());
    }

    /* (non-Javadoc)
     * @see org.netbeans.lib.cvsclient.event.CVSListener#fileUpdated(org.netbeans.lib.cvsclient.event.FileUpdatedEvent)
     */
    public void fileUpdated(FileUpdatedEvent arg0)
    {
        //Debug.message("BasicServerResponse: fileUpdated: " + arg0.getFilePath());
    }

    /* (non-Javadoc)
     * @see org.netbeans.lib.cvsclient.event.CVSListener#fileInfoGenerated(org.netbeans.lib.cvsclient.event.FileInfoEvent)
     */
    public void fileInfoGenerated(FileInfoEvent arg0)
    {
        //FileInfoContainer fic = arg0.getInfoContainer();
        //Debug.message("BasicServerResponse: fileInfoGenerated: " + fic.getClass() + ": " + arg0.getInfoContainer().getFile());
    }

    public void fileToRemove(FileToRemoveEvent ftre)
    {
        //Debug.message("BasicServerResponse: fileToRemove: " + ftre.getFilePath());
    }

    /* (non-Javadoc)
     * @see org.netbeans.lib.cvsclient.event.CVSListener#commandTerminated(org.netbeans.lib.cvsclient.event.TerminationEvent)
     */
    public void commandTerminated(TerminationEvent terminationEvent)
    {
        isTerminated = true;

        this.terminationEvent = terminationEvent;

        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * When this method is called, it blocks the caller until the command being
     * executed is finished.
     */
    public synchronized void waitForExecutionToFinish()
    {
        while (!isTerminated) {
            try {
                wait();
            } catch (InterruptedException e) {
                // nothing to do
            }
        }
    }

    /* (non-Javadoc)
     * @see org.netbeans.lib.cvsclient.event.CVSListener#moduleExpanded(org.netbeans.lib.cvsclient.event.ModuleExpansionEvent)
     */
    public void moduleExpanded(ModuleExpansionEvent arg0)
    {
    }

    /**
     * Returns whether the command was executed succesfully.
     * @return True if the command was executed succesfully
     */
    public boolean isError()
    {
        waitForExecutionToFinish();

        return terminationEvent != null && terminationEvent.isError();
    }

    /**
     * Get the message from the server.
     * @return String contaning the message
     */
    public String getMessage()
    {
        return message.toString();
    }
}
