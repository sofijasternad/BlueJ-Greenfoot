package bluej.views;

/**
 * A "callable" is the generalisation of a Constructor and a Method. This class
 * contains aspects common to both of those.
 * 
 * @author Michael Kolling
 *  
 */
public abstract class CallableView extends MemberView
{
    /**
     * Constructor.
     */
    public CallableView(View view) {
        super(view);
    }

    /**
     * @returns a boolean indicating whether this method has parameters
     */
    public abstract boolean hasParameters();
    
    /**
     * @returns a boolean indicating whether this method uses var args
     */
    public abstract boolean isVarArgs();

    /**
     * Changes an array type name (Object[]) to a var arg (Object ...)
     * 
     * @param typeName The name of the type
     * @return A var arg representation of the type
     */
    private String createVarArg(String typeName) {
        String lastArrayStripped = typeName.substring(0,typeName.length()-2);
        return lastArrayStripped + " ...";        
    }

    /**
     * Count of parameters
     * @returns the number of parameters
     */
    public int getParameterCount() {
        return getParameters().length;
    }

    /**
     * Get an array of Class objects representing parameters
     * @returns array of Class objects
     */
    public abstract Class[] getParameters();

    /**
     * Gets an array of strings with the names of the parameters
     * @return
     */
    public String[] getParamNames()
    {
        Comment c = getComment();
        if( c == null )
            return null;
        return c.getParamNames();
    }
    
    /**
     * Gets an array of nicely formatted strings with the types of the parameters 
     */
    public abstract String[] getParamTypes();
}