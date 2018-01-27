package cemfreitas.autorizadorportal;

import cemfreitas.autorizadorportal.MVC.AutorizadorController;
import cemfreitas.autorizadorportal.MVC.AutorizadorModel;
import cemfreitas.autorizadorportal.MVC.Controller;
import cemfreitas.autorizadorportal.MVC.Model;
import cemfreitas.autorizadorportal.manager.AutorizadorException;
import cemfreitas.autorizadorportal.manager.TransactionManager;
import cemfreitas.autorizadorportal.manager.TransactionMonitor;
import cemfreitas.autorizadorportal.utils.AutorizadorParams;
import cemfreitas.autorizadorportal.utils.Logging;

/* AutorizadorPortal application.
 * Provides message exchange services to 
 * process ISO8583 transaction.
 *  
 *  Main modules:
 *  
 * TransactionManager (TM) - Establish a server socket to receive messages
 * from MC then delegates to a madiator to process them.
 * 
 *  Mediator - Processes ISO transactions from TM. Acts as a mediator among several 
 *  service classes which provide transaction services such as pack, unpack, send to client, etc.
 *  
 *  Monitor - Monitors all transaction flows and update the main screen with current
 *  transactions and some statistics. 
 *     
 */

/* 0.01 Init
 * 0.02 de100 and de39 were passed as 101 and 40 to the sp aut. due a miss positioning in array. 
 * 0.03 Passing the trans. from HSM to SP aut. intead of passing the return of SP HSM.
 * 0.03 beta Close connection after each transaction.
 * 0.04 Changing in the handle of connection to fix the bug of freezing when receiving multiple transactions.
 * 0.05 Changing the sp_hsm_3des calling to pass the bit59 .
 * 0.06 Take off the 0D on the end of message.
 * 0.06 0D - Temporary test with 0D .
 * 1.00 Including "Trailing0D_Message" on config file. First production version.
 * 1.01 Including transaction 0400 (Cancelled).
 */

public class AutorizadorPortal {
	private static final String version = "1.01";
	private static Model autorizadorModel;
	private static Controller autorizadorController;
	private static TransactionManager transactionManager;

	//main method
	public static void main(String[] args) {
		AutorizadorParams.loadProperties();//Load configuration parameters.
		AutorizadorDB.init();//Initialize database. 
		if (args.length == 1) {//Check whether trace should be on
			if (args[0].equalsIgnoreCase("TRACEON")) {
				Logging.init(true);
			}
		} else {
			Logging.init(false);
		}
		autorizadorModel = new AutorizadorModel();//Instantiate application model
		autorizadorController = new AutorizadorController(autorizadorModel);//Instantiate application controller 
		autorizadorController.setApplicationVersion(version);//Set current version
		TransactionMonitor.init(autorizadorController);//Initialize transaction monitor
		transactionManager = new TransactionManager();//Perform a server to accept		
		transactionManager.perform(); // and process MC messages.
	}

	//Shutdown hook.  
	public static void shutDownApplication() throws AutorizadorException {
		Logging.shutdown();
	}

}
