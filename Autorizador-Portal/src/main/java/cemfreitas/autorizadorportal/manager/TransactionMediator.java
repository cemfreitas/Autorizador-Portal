package cemfreitas.autorizadorportal.manager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;

import cemfreitas.autorizadorportal.AutorizadorConstants;
import cemfreitas.autorizadorportal.AutorizadorConstants.TransactionPhase;
import cemfreitas.autorizadorportal.MVC.TransactionData;
import cemfreitas.autorizadorportal.utils.AutorizadorLog;
import cemfreitas.autorizadorportal.utils.AutorizadorParams;
import cemfreitas.autorizadorportal.utils.Logging;

/* TransactionMediator abstract class
 * Implements the mediator pattern and does the communication among the classes used to process the transaction.   
 * Provides base services to process each kind of transaction.   
 * 
 * Implements the observer pattern which notifies the Transaction Manager (TM) when a transaction is unpacked 
 * and when it is completed, successfully or not.
 * 
 * Implements Manager interface which contains the methods used by TM.
 * 
 * Implements Callable interface which contains the method call used by Service Executor API for time out control.  
 *         
 */
public abstract class TransactionMediator extends Observable implements Mediator, Manager, Callable<Object> {
	private static final long timeOutCunfigured = AutorizadorParams.getValueAsInt("TimeoutTransacao");//Get transaction time out value.
	protected Logger traceLog = Logging.getTrace();

	//Used by Transaction implementations.
	protected Transaction transactionTerminal, transHsmDB, transAutorizationDB, transactionHSM;

	//Used by stream connection from TM
	private Socket connection;
	private OutputStream outputStream;

	//Used by transactions bytes representation 
	private byte[] transactionFromTerminal, transactionToTerminal, transactionFromHSM;

	//Used by transactions ISO8583 representation
	protected ISOMsg isoTransactionFromTerminal, isoTransactionToTerminal;

	//Flags to transaction types and its execution status.
	protected boolean isHSMTransaction, isEbcdic, isTimeOut, isAborted, isHsmDisconected;

	//Hold the stored procedure returns
	private List<String> databaseHsmReturn, databaseAutReturn;

	//Used by TM to get the current transaction phase.
	private TransactionPhase transactionPhase;

	//Hold a transaction to be displayed on the screen.	
	protected TransactionData transactionData;

	//Hold an exception when occurs. 
	protected Exception autException, hsmException;

	// Hold the execution time
	protected long hsmDbExecutionTime, hsmExecutionTime, autDbExecutionTime, autExecutionTime;

	// Constructor.
	//Receives CM message and connection from TM
	public TransactionMediator(byte[] transactionFromTerminal, Socket connection) throws IOException {
		this.transactionFromTerminal = transactionFromTerminal;
		this.connection = connection;
		this.outputStream = connection.getOutputStream();
	}

	//Add the TM as Observable
	@Override
	public void addObservable(Observer o) {
		this.addObserver(o);
	}

	// Manager interface method implementation. Uses an Executor Service for time out control.
	@Override
	public void perform() {
		ExecutorService executor = Executors.newCachedThreadPool();

		Future<Object> future = executor.submit(this);// submit reference with call method.

		long timeOutReal;

		//Check whether timeout < min allowed.
		if (timeOutCunfigured < AutorizadorConstants.TIMEOUT_MIN_ALLOWED_TRANSAC) {
			timeOutReal = AutorizadorConstants.TIMEOUT_DEFAULT_TRANSAC;
		} else {
			timeOutReal = timeOutCunfigured;
		}

		try {
			if (traceLog.isTraceEnabled()) {
				traceLog.trace("Conexao " + connection.hashCode() + " recebida");// If trace enabled, log connection received.

			}
			future.get(timeOutReal, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			isTimeOut = true; // If time out, set flag on
			autException = new AutorizadorException("********** Time Out !!! A transacao demorou mais de " + timeOutReal
					+ " milisegundos para executar **********");
		} catch (InterruptedException e) {
			isTimeOut = true;
			autException = new AutorizadorException("********** A execucao da transacao foi interrompida **********");
		} catch (ExecutionException e) {
			isTimeOut = true;
			autException = new AutorizadorException(
					"********** A execucao da transacao terminou com erro :" + e.getMessage() + " ********** ");
		} finally {
			//When transaction process ends, send a mediator reference to build the log informations.
			AutorizadorLog autorizadorLog = new AutorizadorLog(this);
			autorizadorLog.log();
			future.cancel(true); // Attempts to cancel the thread if it is still running.
			/*if (!connection.isClosed()) {
				try {
					connection.close();
					if (traceLog.isTraceEnabled()) {
						traceLog.trace("Conexao " + connection.hashCode() + " finalizada");// If trace enabled, log connection closing.

					}
				} catch (IOException e) {
					autException = new AutorizadorException(
							"********** Erro ao fechar conexão com o terminal :" + e.getMessage() + " ********** ");
				}
			}*/
		}
	}

	// Callable method used by ExecutorService. Executes the transaction flow.
	@Override
	public abstract Object call();

	//Changes phase and notify TM
	protected void changeTransactionPhase(TransactionPhase phase) {
		transactionPhase = phase;
		setChanged();
		notifyObservers(Thread.currentThread().getId());

	}

	//Gets and Sets methods.

	@Override
	public byte[] getTransactionFromTerminal() {
		return transactionFromTerminal;
	}

	@Override
	public ISOMsg getIsoTransactionFromTerminal() {
		return isoTransactionFromTerminal;
	}

	@Override
	public void setIsoTransactionFromTerminal(ISOMsg isoTransactionFromTerminal) {
		this.isoTransactionFromTerminal = isoTransactionFromTerminal;
	}

	@Override
	public boolean isHSMTransaction() {
		return isHSMTransaction;
	}

	@Override
	public void setHSMTransaction(boolean isHSMTransaction) {
		this.isHSMTransaction = isHSMTransaction;
	}

	@Override
	public OutputStream getOutputStream() {
		return outputStream;
	}

	@Override
	public byte[] getTransactionToTerminal() {
		return transactionToTerminal;
	}

	@Override
	public void setTransactionToTerminal(byte[] transactionToTerminal) {
		this.transactionToTerminal = transactionToTerminal;
	}

	@Override
	public ISOMsg getIsoTransactionToTerminal() {
		return isoTransactionToTerminal;
	}

	@Override
	public void setIsoTransactionToTerminal(ISOMsg isoTransactionToTerminal) {
		this.isoTransactionToTerminal = isoTransactionToTerminal;
	}

	@Override
	public byte[] getTransactionFromHSM() {
		return transactionFromHSM;
	}

	@Override
	public void setTransactionFromHSM(byte[] transactionFromHSM) {
		this.transactionFromHSM = transactionFromHSM;
	}

	@Override
	public void setDatabaseHsmReturn(List<String> databaseHsmReturn) {
		this.databaseHsmReturn = databaseHsmReturn;
	}

	@Override
	public List<String> getDatabaseHsmReturn() {
		return databaseHsmReturn;
	}

	@Override
	public void setDatabaseAutReturn(List<String> databaseAutReturn) {
		this.databaseAutReturn = databaseAutReturn;
	}

	@Override
	public List<String> getDatabaseAutReturn() {
		return databaseAutReturn;
	}

	@Override
	public TransactionPhase getTransactionPhase() {
		return transactionPhase;
	}

	@Override
	public TransactionData getTransactionData() {
		return transactionData;
	}

	@Override
	public void setTransactionData(TransactionData transactionData) {
		this.transactionData = transactionData;
	}

	@Override
	public boolean isHsmDisconected() {
		return isHsmDisconected;
	}

	@Override
	public void setHsmDisconected(boolean isHsmDisconected) {
		this.isHsmDisconected = isHsmDisconected;

	}

	@Override
	public long getHsmDbExecutionTime() {
		return hsmDbExecutionTime;
	}

	@Override
	public long getHsmExecutionTime() {
		return hsmExecutionTime;
	}

	@Override
	public long getAutDbExecutionTime() {
		return autDbExecutionTime;
	}

	@Override
	public long getAutExecutionTime() {
		return autExecutionTime;
	}

	@Override
	public Exception getAutException() {
		return autException;
	}

	@Override
	public Exception getHsmException() {
		return hsmException;
	}

	@Override
	public boolean isTimeOut() {
		return isTimeOut;
	}

	@Override
	public boolean isEbcdic() {
		return isEbcdic;
	}

	@Override
	public void setEbcdic(boolean isEbcdic) {
		this.isEbcdic = isEbcdic;

	}
}
