package bluej.extensions;

import bluej.debugger.ObjectWrapper;
import bluej.debugger.jdi.JdiObject;
import bluej.pkgmgr.PkgMgrFrame;

import com.sun.jdi.*;

import bluej.pkgmgr.Package;
import bluej.views.*;
import bluej.debugger.*;
import bluej.utility.Debug;

/**
 * A wrapper for a field of a BlueJ class.
 * Behaviour is similar to the Reflection API.
 * 
 * @version $Id: BField.java 1966 2003-05-21 09:09:15Z damiano $
 */

/*
 * The same reasoning of BConstructor apply here.
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 * Previous attempt by Clive Miller, University of Kent at Canterbury, 2002
 */
 
public class BField
{
    private FieldView bluej_view;
    private Identifier parentId;
    
    BField ( Identifier aParentId, FieldView i_bluej_view )
    {
        parentId = aParentId;
        bluej_view = i_bluej_view;
    }        

    /**
     * Check to see if the field name matches the given one.
     * 
     * @return true if it does, false othervide
     */
    public boolean matches ( String fieldName )
        {
        // Who is so crazy to give me a null name ?
        if ( fieldName == null ) return false;

        return fieldName.equals(getName());
        }


    /**
     * Return the name of the field.
     * Similar to reflection API.
     */
    public String getName()
        {
        // Tested ok, 070303 Damiano
        return bluej_view.getName();
        }

    /**
     * Return the type of the field.
     * Similar to Reflection API.
     */
    public Class getType()
        {
        // Tested ok, 070303 Damiano
        return bluej_view.getType().getViewClass();
        }

    /**
     * Returns the modifiers of this field.
     * The <code>java.lang.reflect.Modifier</code> class can be used to decode the modifiers.
     * Similar to reflection API
     */
    public int getModifiers ()
        {
        return bluej_view.getModifiers();
        }
        

    /**
     * When you are inspecting a static field use this one.
     */
    private Object getStaticField () throws ProjectNotOpenException
      {
      Package bluejPkg = parentId.getBluejPackage();
      
      String wantFieldName = getName();

      // I need to get the view of the parent of this Field
      // That must be the Class that I want to look after....
      View parentView = bluej_view.getDeclaringView();
      String className = parentView.getQualifiedName();

      // UFF, there seems to be no way to get the package from the view...
      // Maybe should ask Michael, when he has time...
      DebuggerClassLoader loader = bluejPkg.getRemoteClassLoader();
      if ( loader == null ) 
        {
        // This is really an error
        Debug.message("BField.getStatucField: Class="+className+" Field="+wantFieldName+" ERROR: cannod get DebuggerClassLoader");
        return null;
        }
      
      DebuggerClass debuggerClass = bluejPkg.getDebugger().getClass(className, loader);
      if ( debuggerClass == null ) 
        {
        // This may not be an error, the class name may be wrong...
        Debug.message("BField.getStatucField: Class="+className+" Field="+wantFieldName+" WARNING: cannod get debuggerClass");
        return null;
        }

      // Now I want the Debugger object of that field.
      // I do it this way since there is no way to get it by name...
      int staticCount=debuggerClass.getStaticFieldCount();
      DebuggerObject debugObj=null;
      for ( int index=0; index<staticCount; index++ )
        {
        if ( wantFieldName.equals(debuggerClass.getStaticFieldName(index)) )
          {
          debugObj = debuggerClass.getStaticFieldObject(index);
          break;
          }
        }

      if ( debugObj == null ) 
        {
        // No need to complain about it it may not be a static field...
//        Debug.message("BField.getStatucField: Class="+className+" Field="+wantFieldName+" DEBUG: fieldObject==null");
        return null;
        }

      ObjectReference objRef = debugObj.getObjectReference();
      if ( objRef == null )
        {
        // WARNING At the moment it is not possible to have reflection behaviour
        // You NEED to have an instance of the class to get static fields...
        // Therefore.... this is normal behaviour and not an ERROR...
//        Debug.message("BField.getStatucField: Class="+className+" Field="+wantFieldName+" ERROR: ObjectReference==null");
        return null;
        }

      return doGetVal(bluejPkg, wantFieldName, objRef);
      }


    /**
     * Return the value of this field of the given object.
     * This is similar to Reflection API.
     *
     * In the case that the field is of primitive type (<code>int</code> etc.), 
     * the return value is of the appropriate Java wrapper type (<code>Integer</code> etc.).
     * In the case that the field contains an object then 
     * an appropriate BObject will be returned. 
     *
     * The main reason that this method is on a field (derived from a class), 
     * rather than directly on an object, is to allow for the retrieval of 
     * static field values without having to create an object of the appropriate type.
     *
     * As in the Relection API, in order to get the value of a static field pass 
     * null as the parameter to this method.
     */
    public Object getValue ( BObject onThis ) throws ProjectNotOpenException
        {
        // If someone gives me a null it means that he wants a static field
        if ( onThis == null ) return getStaticField();
        
        ObjectReference objRef = onThis.getObjectReference();

        ReferenceType type = objRef.referenceType();

        Field thisField = type.fieldByName (bluej_view.getName());
        if ( thisField == null ) return null;
       
        Package bluej_pkg = onThis.getBluejPackage();
        return doGetVal(bluej_pkg, bluej_view.getName(), objRef.getValue(thisField));
        }


    /**
     * Given a Value that comes from th remote debugger machine, converts it into somethig
     * that is usable. The real important thing here is to return a BObject for objects 
     * that can be put into the bench.
     */
    static Object doGetVal ( Package bluej_pkg, String instanceName, Value val )
        {
        if ( val == null ) return null;
        
        if (val instanceof StringReference) return ((StringReference) val).value();
        if (val instanceof BooleanValue) return new Boolean (((BooleanValue) val).value());
        if (val instanceof ByteValue)    return new Byte (((ByteValue) val).value());
        if (val instanceof CharValue)    return new Character (((CharValue) val).value());
        if (val instanceof DoubleValue)  return new Double (((DoubleValue) val).value());
        if (val instanceof FloatValue)   return new Float (((FloatValue) val).value());
        if (val instanceof IntegerValue) return new Integer (((IntegerValue) val).value());
        if (val instanceof LongValue)    return new Long (((LongValue) val).value());
        if (val instanceof ShortValue)   return new Short (((ShortValue) val).value());

        if (val instanceof ObjectReference)
          {
          PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluej_pkg);
          ObjectWrapper objWrap = new ObjectWrapper (pmf, pmf.getObjectBench(), JdiObject.getDebuggerObject((ObjectReference)val),instanceName);
          return new BObject ( objWrap );
          }

        return val.toString();
        }
    }
