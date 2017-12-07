package flexgridsim.util;

import org.python.core.Py;
import org.python.core.PyClass;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyObjectDerived;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import java.io.InputStream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;


/**
 * 
 * @author trindade
 *
 */
public class JythonCaller {
    
	private PythonInterpreter pythonInterpreter;
    
	public JythonCaller() {
		PySystemState engineSys = new PySystemState();
    	engineSys.path.append(Py.newString("src/python"));
        pythonInterpreter = new PythonInterpreter(null, engineSys);
    }
    
    public void divide() {
    	pythonInterpreter.exec("from divider import Divider");
        PyClass dividerDef = (PyClass) pythonInterpreter.get("Divider");
        PyObject divider = dividerDef.__call__();
        PyObject pyObject = divider.invoke("divide",new PyInteger(20),new PyInteger(4));
        System.out.println(pyObject.toString());
    }
    
    public void multiply() {
    	pythonInterpreter.exec("from divider import Divider");
        PyClass dividerDef = (PyClass) pythonInterpreter.get("Divider");
        PyObject divider = dividerDef.__call__();
        PyObject []args = { new PyInteger(20), new PyInteger(4), new PyInteger(100) };
        PyObject pyObject = divider.invoke("multiply",args);
        System.out.println(pyObject.toString());
    }
}