package cemfreitas.autorizadorportal.manager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;

import cemfreitas.autorizadorportal.utils.AppFunctions;
import cemfreitas.autorizadorportal.utils.AutorizadorParams;
import cemfreitas.autorizadorportal.utils.Logging;

/* TransactionAutorizationDB class.
 * Extends the abstracted class TransactionBaseDB and provides implementations 
 * specific to the database authorization
 */
public class TransactionAutorizationDB extends TransactionBaseDB {
	private static final String storedProcedureName = AutorizadorParams.getValue("SP_Autorizacao");
	private Logger traceLog = Logging.getTrace();
	private Mediator mediator;

	public TransactionAutorizationDB(Mediator mediator) {
		super(storedProcedureName);// Send SP to be executed by superclass
		this.mediator = mediator;// Get the mediator reference
	}

	// Set the List of parameters to be executed by the SP
	// The parameters comes from terminal transaction previously unpacked.
	private void setParameters() throws AutorizadorException {

		ISOMsg transactionFromTerminal = mediator.getIsoTransactionFromTerminal();// Get terminal ISO transaction.
		List<String> parametersList = new ArrayList<String>();

		parametersList.add(transactionFromTerminal.hasField(0) ? "1" + (String) transactionFromTerminal.getValue(0) : "0");		

		Object isoValue;
		for (int i = 2; i < 129; i++) {
			if (transactionFromTerminal.hasField(i)) {
				isoValue = transactionFromTerminal.getValue(i);
				if (isoValue instanceof byte[]) {
					parametersList.add("1" + AppFunctions.hexString((byte[]) isoValue));
				} else {
					parametersList.add("1" + (String) isoValue);
				}
			} else {
				parametersList.add("0");
			}
		}
		
		String de39 = "0";		

		try {
			parametersList.set(99, "1" + InetAddress.getLocalHost() + ":" + AutorizadorParams.getValue("PortaEscuta"));
		} catch (UnknownHostException e) {
			parametersList.set(99, "");
		}

		// If transaction was sent to HSM, set response field de39 with the
		// proper if HSM failed.
		if (mediator.isHSMTransaction()) {
			if (mediator.getTransactionFromHSM() != null) {
				parametersList.set(125, "1" + new String(mediator.getTransactionFromHSM()));
			} else {
				de39 = "183";
			}
		}
		parametersList.set(38, de39);
		setParametersList(parametersList);
	}

	// After parameters set, execute SP on database.
	@Override
	public long doTransaction() throws AutorizadorException {
		setParameters();
		if (traceLog.isTraceEnabled()) {
			traceLog.trace(" ----- Chamada da SP Autorizacao -----");
			traceLog.trace(getTransactionToTrace(getParametersList()));
		}
		long beginExecution = System.currentTimeMillis();
		send();
		receive();
		long endExecution = System.currentTimeMillis();
		if (traceLog.isTraceEnabled()) {
			traceLog.trace(" Resultado da SP Autorizacao : {}", listDbExecutionResult());
		}
		mediator.setDatabaseAutReturn(getDatabaseReturn());
		if (!mediator.getDatabaseAutReturn().get(0).equals("0")) {			
			throw new AutorizadorException("SP autorizacao retornou status diferente de zero. Transacao cancelada.");			
		}
		return (endExecution - beginExecution);
	}

	// For trace purposes.
	private String listDbExecutionResult() {
		StringBuffer result = new StringBuffer();
		int deNumber = 0;		
		
		for (String de : getDatabaseReturn()) {
			result.append("DE");
			result.append(deNumber++);
			result.append(" :");
			result.append(de);
			result.append(", ");
		}

		return result.substring(0, result.length() - 2);
	}

}
