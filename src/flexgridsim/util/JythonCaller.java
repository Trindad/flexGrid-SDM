package flexgridsim.util;

import org.python.core.Py;
import org.python.core.PyArray;
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

	public void kmeans(double [][]features, int k) {
		pythonInterpreter.exec("from divider import KMeans");
        PyClass dividerDef = (PyClass) pythonInterpreter.get("KMeans");
        PyObject divider = dividerDef.__call__();
        PyArray xy = new PyArray(Double.class, features);
        PyObject []args = { xy, new PyInteger(k) };
        PyObject pyObject = divider.invoke("kmeans",args);
        System.out.println(pyObject.toString());	
	}
}