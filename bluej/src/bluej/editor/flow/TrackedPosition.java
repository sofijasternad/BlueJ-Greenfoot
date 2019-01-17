package bluej.editor.flow;

import bluej.editor.flow.Document.Bias;

/**
 * A position within a document, that stays persistent.  So if you insert text
 * in the document before it, the position should move accordingly.  If you delete
 * a portion that includes the position, the position moves to the start of the deleted
 * range.
 */
public class TrackedPosition
{
    // package-visible for access by document classes:
    int position;
    final Bias bias;
    private final Document document;
    
    // Package-visible constructor
    public TrackedPosition(Document document, int initialPosition, Bias bias)
    {
        this.document = document;
        this.position = initialPosition;
        this.bias = bias;
    }

    void updateTrackedPosition(int removedStartCharIncl, int removedEndCharExcl, int insertedLength)
    {
        if (this.position > removedStartCharIncl || (this.position == removedStartCharIncl && this.bias == Bias.FORWARD))
        {
            if (this.position < removedEndCharExcl || this.position == removedEndCharExcl && this.bias != Bias.FORWARD)
            {
                this.position = removedStartCharIncl;
            }
            else
            {
                this.position += insertedLength - (removedEndCharExcl - removedStartCharIncl);
            }
        }
    }

    public int getLine()
    {
        return document.getLineFromPosition(position);
    }

    public int getColumn()
    {
        return document.getColumnFromPosition(position);
    }

    public int getPosition()
    {
        return position;
    }
}
