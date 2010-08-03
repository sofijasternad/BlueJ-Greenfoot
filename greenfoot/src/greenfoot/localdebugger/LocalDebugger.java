/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.localdebugger;

import greenfoot.core.Simulation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import bluej.classmgr.BPClassLoader;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerListener;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerResult;
import bluej.debugger.DebuggerTestResult;
import bluej.debugger.DebuggerThreadTreeModel;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.SourceLocation;
import bluej.runtime.ExecServer;
import bluej.utility.Debug;

/**
 * A "local" debugger. This implements various parts of the Debugger interface, to allow
 * executing user code in the local VM. Some of the interface is not implemented however.
 * 
 * @author Davin McCall
 */
public class LocalDebugger extends Debugger
{

    @Override
    public void addDebuggerListener(DebuggerListener l)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addObject(String scopeId, String newInstanceName,
            DebuggerObject dob)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close(boolean restart)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disposeWindows()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebuggerClass getClass(String className)
            throws ClassNotFoundException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebuggerObject getMirror(String value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, DebuggerObject> getObjects()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebuggerObject getStaticValue(String className, String fieldName)
            throws ClassNotFoundException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getStatus()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebuggerThreadTreeModel getThreadTreeModel()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String guessNewName(String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String guessNewName(DebuggerObject obj)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void hideSystemThreads(boolean hide)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebuggerResult instantiateClass(String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebuggerResult instantiateClass(String className,
            String[] paramTypes, DebuggerObject[] args)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void launch()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void newClassLoader(BPClassLoader bpClassLoader)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeBreakpointsForClass(String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeDebuggerListener(DebuggerListener l)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeObject(String scopeId, String instanceName)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * A class to support running user code on the simulation thread.
     * 
     * @author Davin McCall
     */
    private static class QueuedExecution implements Runnable
    {
        private Class<?> c;
        private DebuggerResult result;
        
        public QueuedExecution(Class <?> c)
        {
            this.c = c;
        }
        
        public synchronized void run()
        {
            try {
                Method m = c.getMethod("run", new Class[0]);
                Object result = m.invoke(null, new Object[0]);
                LocalObject resultObject = wrapResult(result, m.getReturnType());
                this.result = new DebuggerResult(resultObject);
            }
            catch (IllegalAccessException iae) {
                Debug.reportError("LocalDebugger runClassMain error", iae);
                result = new DebuggerResult(new ExceptionDescription("Internal error"));
            }
            catch (NoSuchMethodException nsme) {
                Debug.reportError("LocalDebugger runClassMain error", nsme);
                result = new DebuggerResult(new ExceptionDescription("Internal error"));
            }
            catch(InvocationTargetException ite) {
                ite.getCause().printStackTrace(System.err);
                ExceptionDescription exception = getExceptionDescription(ite.getCause());
                result = new DebuggerResult(exception);
            }
            notify();
        }
        
        public synchronized DebuggerResult getResult()
        {
            while (result == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // should be safe to ignore
                }
            }
            return result;
        }
    }
    
    @Override
    public DebuggerResult runClassMain(String className)
            throws ClassNotFoundException
    {
        ClassLoader currentLoader = ExecServer.getCurrentClassLoader();
        Class<?> c = currentLoader.loadClass(className);
        QueuedExecution qe = new QueuedExecution(c);
        Simulation.getInstance().runLater(qe);
        return qe.getResult();
    }

    @Override
    public DebuggerTestResult runTestMethod(String className, String methodName)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, DebuggerObject> runTestSetUp(String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toggleBreakpoint(String className, int line, boolean set,
            Map<String, String> properties)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toggleBreakpoint(String className, String method,
            boolean set, Map<String, String> properties)
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Wrap a value, that is the result of a method call, in a form that the
     * ResultInspector can understand.<p>
     * 
     * Also ensure that if the result is a primitive type it is correctly
     * unwrapped.
     * 
     * @param r  The result value
     * @param c  The result type
     * @return   A DebuggerObject which wraps the result
     */
    private static LocalObject wrapResult(final Object r, Class<?> c)
    {
        Object wrapped;
        if (c == boolean.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public boolean result = ((Boolean) r).booleanValue();
            };
        }
        else if (c == byte.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public byte result = ((Byte) r).byteValue();
            };
        }
        else if (c == char.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public char result = ((Character) r).charValue();
            };
        }
        else if (c == short.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public short result = ((Short) r).shortValue();
            };
        }
        else if (c == int.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public int result = ((Integer) r).intValue();
            };
        }
        else if (c == long.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public long result = ((Long) r).longValue();
            };
        }
        else if (c == float.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public float result = ((Float) r).floatValue();
            };
        }
        else if (c == double.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public double result = ((Double) r).doubleValue();
            };
        }
        else {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public Object result = r;
            };
        }
        return LocalObject.getLocalObject(wrapped);
    }
    
    /**
     * Convert a Throwable into an ExceptionDescription.
     */
    private static ExceptionDescription getExceptionDescription(Throwable t)
    {
        List<SourceLocation> stack = new ArrayList<SourceLocation>();
        StackTraceElement [] stackTrace = t.getStackTrace();
        for (StackTraceElement element : stackTrace) {
            stack.add(new SourceLocation(element.getClassName(), element.getFileName(),
                    element.getMethodName(), element.getLineNumber()));
        }
        new ExceptionDescription(t.getClass().getName(), t.getLocalizedMessage(), stack);
        return null;
    }
}
