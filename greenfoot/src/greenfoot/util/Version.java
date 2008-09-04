package greenfoot.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a version number. A version is a sequence o numbers separated by
 * full stops and an optional string at the end.
 * 
 * @author Poul Henriksen
 * 
 */
public class Version
{
    /**
     * A change in this number indicates a breaking change that will be likely
     * to break some scenarios.
     */
    private int breakingNumber;

    /**
     * A change in this number indicates a visible (to the user) change that
     * should not break anything in most cases.
     */
    private int nonBreakingNumber;

    /** A change in this number indicates an internal change only. */
    private int internalNumber;

    /** The version number was bad or non-existent */
    private boolean badVersion = false;

    /**
     * Create a new Version from the string.
     * 
     * @param versionString A string in the format X.Y.Z. If the string is null
     *            or invalid, it will be flagged and can be determined by
     *            calling {@link #isBad()}.
     */
    public Version(String versionString)
    {
        if (versionString == null) {
            badVersion = true;
            return;
        }

        String[] split = versionString.split("\\.");
        List<Integer> numbers = new ArrayList<Integer>();

        String lastString = null;
        for (String s : split) {
            try {
                numbers.add(new Integer(Integer.parseInt(s)));
            }
            catch (NumberFormatException nfe) {
                lastString = s;
                break;
            };
        }

        // Make sure to handle the last number - even if there is something
        // after it, like an extra string.
        if (numbers.size() < 3 && lastString != null) {
            // split around any sequence of non-digits.
            String[] endSplit = lastString.split("[^0-9]+");
            // The first element of the array now contains a number
            if (endSplit.length > 0) {
                String candidate = endSplit[0];
                // if the candidate number is matching the beginning, we have
                // found a part of the version number.
                if (lastString.startsWith(candidate)) {
                    numbers.add(new Integer(Integer.parseInt(candidate)));
                }
            }
        }

        if (numbers.size() == 3) {
            breakingNumber = numbers.get(0);
            nonBreakingNumber = numbers.get(1);
            internalNumber = numbers.get(2);
        }
        else {
            badVersion = true;
        }
    }

    /**
     * True if this version number is older than the other version number in a
     * way that will be likely to break some scenarios. Or if any of the
     * versions is a bad version number.
     * 
     */
    public boolean isOlderAndBreaking(Version other)
    {

        return this.breakingNumber < other.breakingNumber || this.badVersion || other.badVersion;
    }

    /**
     * True if this version number is different than the other version number in
     * a way that will be unlikely to break scenarios. Or if any of the versions
     * is a bad version number.
     */
    public boolean isNonBreaking(Version other)
    {
        return this.nonBreakingNumber != other.nonBreakingNumber || this.badVersion || other.badVersion;
    }

    /**
     * True if this version number is different than the other version number
     * but will only contain in internal changes and will not break scenarios.
     * Or if any of the versions is a bad version number.
     * 
     */
    public boolean isInternal(Version other)
    {
        return this.internalNumber != other.internalNumber || this.badVersion || other.badVersion;
    }

    /**
     * True if the version number was not correctly formated.
     * 
     */
    public boolean isBad()
    {
        return badVersion;
    }

    /**
     * Returns the version in the format X.Y.Z.
     */
    public String toString()
    {
        return breakingNumber + "." + nonBreakingNumber + "." + internalNumber;
    }

}
