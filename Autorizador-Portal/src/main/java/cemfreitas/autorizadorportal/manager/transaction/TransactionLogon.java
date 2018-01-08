package cemfreitas.autorizadorportal.manager.transaction;

import java.io.IOException;
import java.net.Socket;

import cemfreitas.autorizadorportal.AutorizadorConstants.TransactionPhase;
import cemfreitas.autorizadorportal.manager.AutorizadorException;
import cemfreitas.autorizadorportal.manager.Mediator;
import cemfreitas.autorizadorportal.manager.TransactionFactory;
import cemfreitas.autorizadorportal.manager.TransactionFactoryBase;
import cemfreitas.autorizadorportal.manager.TransactionMediator;

/*
 * TransactionLogon class.
 * Provides implementation for Logon transactions (0800).
 */
public class TransactionLogon extends TransactionMediator{
	public TransactionLogon(byte[] transactionFromTerminal, Socket connection) throws IOException {
		super(transactionFromTerminal, connection);
	}

	Mediator mediator;	
	
	
	// Callable method used by ExecutorService. Executes the transaction flow.
	@Override
	public Object call() {
		TransactionFactoryBase transactionFactory = new TransactionFactory();
		long beginTransacExec, endTransacExec;

		try {// Outer try/catch block
			beginTransacExec = System.currentTimeMillis(); // Get the time of
															// the beginning
															// transaction
															// execution.

			//Starts to process a terminal transaction.
			transactionTerminal = transactionFactory.createTerminalTransaction(this);
			transactionTerminal.unpack();			
			changeTransactionPhase(TransactionPhase.TRANSAC_UNPACK);//Notify TM with a new unpacked transaction.
			
			// Preparing to execute SP Authorization
			transAutorizationDB = transactionFactory.createAutDbTransaction(this);

			autDbExecutionTime = transAutorizationDB.doTransaction(); // Execute
																		// SP
																		// Authorization

			// Preparing to send the transaction response to terminal.			
			transactionTerminal.pack();// Pack response			

			transactionTerminal.send();// Send it

			endTransacExec = System.currentTimeMillis();//Mark the end of execution and ...
			autExecutionTime = endTransacExec - beginTransacExec;//Calculates the execution time.

		} catch (AutorizadorException | IOException e) {
			autException = e;

			if (traceLog.isTraceEnabled()) {
				if (!e.getMessage().equals("")) {
					traceLog.trace("Origem do erro:", e);// If trace enabled, log
															// the stack error on
															// trace file.
				}
			}
			isAborted = true;// Set flag. Transaction aborted.

		} finally {
			/*
			 * if (traceLog.isTraceEnabled()) {// Turn the trace log off if
			 * enabled, so that be executed only once. Logging.turnTraceOff(); }
			 */

			//Message could not be unpacked. Aborting ...
			if (isoTransactionFromTerminal == null) {
				return null;
			}

			if (isAborted || isTimeOut) {// If occurred some error				
				changeTransactionPhase(TransactionPhase.TRANSAC_AUT_ERROR);//Notify TM with transaction finished with error.
			} else {
				changeTransactionPhase(TransactionPhase.TRANSAC_COMPLETED);//Notify TM with transaction finished successfully.
			}
			
			

		} // End of outer try/catch block
		return null;
	}

}
