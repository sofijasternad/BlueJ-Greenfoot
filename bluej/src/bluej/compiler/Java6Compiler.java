/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 

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
package bluej.compiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Compiler class implemented using the JavaCompiler
 * 
 * @author Marion Zalk
 *
 */
public class Java6Compiler extends Compiler {

    /**
     * Compile some source files by using the JavaCompiler API. Allows for the addition of user
     * options
     * 
     * @param sources
     *            The files to compile
     * @param observer
     *            The compilation observer
     * @param internal
     *            True if compiling BlueJ-generated code (shell files) False if
     *            compiling user code
     * @return    success
     */
    public boolean compile(File[] sources, CompileObserver observer,
            boolean internal) {
        boolean result = true;
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        String[] options = new String[]{};
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        try
        {  
            //setup the filemanager
            StandardJavaFileManager sjfm = jc.getStandardFileManager(diagnostics, null, null);
            List <File>pathList = new ArrayList<File>();
            List<File> outputList= new ArrayList<File>();
            outputList.add(getDestDir());
            pathList.addAll(Arrays.asList(getProjectClassLoader().getClassPathAsFiles()));
            sjfm.setLocation(StandardLocation.SOURCE_PATH, pathList);
            sjfm.setLocation(StandardLocation.CLASS_PATH, pathList);
            sjfm.setLocation(StandardLocation.CLASS_OUTPUT, outputList);
            //get the source files for compilation  
            Iterable<? extends JavaFileObject> compilationUnits1 =
                sjfm.getJavaFileObjectsFromFiles(Arrays.asList(sources));
            //add any options
            if(isDebug())
                options[0]="-g";
            if(isDeprecation())
                options[1]="-deprecation"; 
            List<String> optionsList=new ArrayList<String>();
            optionsList.addAll(Arrays.asList(options));
            addUserSpecifiedOptions(optionsList, COMPILER_OPTIONS);
            //compile
            jc.getTask(null, sjfm, diagnostics, optionsList, null, compilationUnits1).call();
            sjfm.close();            

        }
        catch(IOException e)
        {
            e.printStackTrace(System.out);
            return false;
        }

        //Query diagnostics for error/warning messages
        List<Diagnostic<? extends JavaFileObject>> diagnosticList = diagnostics.getDiagnostics();        
        String src=null;
        int pos=0;
        String msg=null;
        boolean error=false;
        boolean warning=false;
        int diagnosticErrorPosition=-1;
        int diagnosticWarningPosition=-1;
        //as there is no ordering of errors/warnings in terms of importance
        //need to find an error if there is one, else use the warning
        for (int i=0; i< diagnosticList.size(); i++){
            if (diagnosticList.get(i).getKind().equals(Diagnostic.Kind.ERROR))
            {
                diagnosticErrorPosition=i;
                error=true;
                warning=false;
                break;
            }
            if (diagnosticList.get(i).getKind().equals(Diagnostic.Kind.WARNING)||
                    diagnosticList.get(i).getKind().equals(Diagnostic.Kind.NOTE))
            {
                warning=true;
                //just to ensure the first instance of the warning position is recorded (not the last)
                if (diagnosticWarningPosition==-1){
                    diagnosticWarningPosition=i;
                }
            }
        }
        //diagnosticErrorPosition can either be the warning/error
        if (diagnosticErrorPosition<0)
            diagnosticErrorPosition=diagnosticWarningPosition;
        //set the necessary values
        if (warning||error){
            if (((Diagnostic<?>)diagnosticList.get(diagnosticErrorPosition)).getSource()!=null)
                src= ((Diagnostic<?>)diagnosticList.get(diagnosticErrorPosition)).getSource().toString();
            pos= (int)((Diagnostic<?>)diagnosticList.get(diagnosticErrorPosition)).getLineNumber();
            msg=((Diagnostic<?>)diagnosticList.get(diagnosticErrorPosition)).getMessage(null);

            // Handle compiler error messages 
            if (error) 
            {
                result=false;
                msg=processMessage(msg);
                observer.errorMessage(src, pos, msg);
            }
            // Handle compiler warning messages  
            // If it is a warning message, need to get all the messages
            if (warning) 
            {
                observer.warningMessage(src, pos, msg);
                for (int i=diagnosticErrorPosition+1; i< diagnosticList.size(); i++){
                    msg=((Diagnostic<?>)diagnosticList.get(i)).getMessage(null);
                    observer.warningMessage(src, pos, msg);
                }              
            }
        }
        return result;

    }

    /**
     * @param  String msg representing the message retrieved from the diagnostic tool
     * processMessage tidies up the message returned from the diagnostic tool
     * @return message String
     */
    protected String processMessage(String msg){
        //the message is in this format 
        //path:line number:message
        //i.e includes the path and line number so need to strip that off
        //trimming the message to exclude path etc
        if (msg.indexOf(':')!=-1){
            msg=msg.substring(msg.indexOf(':')+1, msg.length());
            if (msg.indexOf(':')!=-1){
                msg=msg.substring(msg.indexOf(':')+1, msg.length());
            }
        }
        String message =msg;
        if (msg.contains("cannot resolve symbol")
                || msg.contains("cannot find symbol")
                || msg.contains("incompatible types")) {
            //dividing the message into its different lines so can retrieve necessary values
            int index1,index2, index3=0;
            String line2, line3;
            index1=msg.indexOf('\n');
            index2=msg.indexOf('\n',index1+1);
            index3=msg.length();
            //i.e there are only 2 lines not 3
            if (index2<index1)
                index2=index3;
            message=msg.substring(0, index1);
            line2=msg.substring(index1, index2);
            line3=msg.substring(index2,index3);

            //e.g incompatible types
            //found   : int
            //required: java.lang.String
            if (line2.contains("found"))                
                message= message +" - found "+line2.substring(line2.indexOf(':')+2, line2.length());
            if (line3.contains("required"))
                message= message +" but expected "+line3.substring(line3.indexOf(':')+2, line3.length());
            //e.g cannot find symbol
            //symbol: class Persons
            if (line2.contains("symbol"))                
                message= message +" - "+line2.substring(line2.indexOf(':')+2, line2.length());          
        }
        return message;
    }
}