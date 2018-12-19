package flexgridsim.voncontroller;

import flexgridsim.Database;

public class Monitor implements DatabaseObserver {
	
	public Monitor() {
		Database.attach(this);
	}
	
	public void dataUpdated() {
		
		Database db = Database.getInstance();
		
		//TODO: do STUFF!
	}
	
	/**
	 * verifica se há sobrecarga, e se sim diminuir a captura de dados
	 */
}
