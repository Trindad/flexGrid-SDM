package flexgridsim.util;

import org.python.core.Py;
import org.python.core.PyArray;
import org.python.core.PyClass;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;


/**
 * KMeans 
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

	public String[] kmeans(double [][]features, int k) {
		pythonInterpreter.exec("from kmeans import KMeans");
        PyClass kmeansDef = (PyClass) pythonInterpreter.get("KMeans");
        PyObject kmeans = kmeansDef.__call__();
        PyArray xy = new PyArray(Double.class, features);
        PyObject []args = { xy, new PyInteger(k) };
        PyObject pyObject = kmeans.invoke("kmeans",args);
        pyObject.getType();
        
        return pyObject.toString().split("-");	
	}
}