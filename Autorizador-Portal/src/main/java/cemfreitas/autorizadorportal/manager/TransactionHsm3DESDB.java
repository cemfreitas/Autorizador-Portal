package cemfreitas.autorizadorportal.manager;

import java.util.ArrayList;
import java.util.List;

import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;

import cemfreitas.autorizadorportal.utils.AppFunctions;
import cemfreitas.autorizadorportal.utils.AutorizadorParams;
import cemfreitas.autorizadorportal.utils.Logging;

/* TransactionHsm3DESDB class.
 * Extends abstract class TransactionBaseDB and provides further
 * implementation for 3DES HSM database transactions. 
 */
public class TransactionHsm3DESDB extends TransactionBaseDB {
	private static final String storedProcedureName = AutorizadorParams.getValue("SP_HSM_3DES");	
	private Logger traceLog = Logging.getTrace();
	private Mediator mediator;

	public TransactionHsm3DESDB(Mediator mediator) {		
		super(storedProcedureName);// Send SP to be executed to superclass
		this.mediator = mediator;// Get the mediator reference
	}

	// Execute the HSM SP call.
	@Override
	public long doTransaction() throws AutorizadorException {
		int[] isoFields = new int[] { 2, 32, 35, 41, 45, 52, 59 };// ISO fields used
																// in the SP
																// params.

		ISOMsg transactionFromTerminal = mediator.getIsoTransactionFromTerminal(); // Get from mediator, terminal ISO transaction.

		List<String> parametersList = new ArrayList<String>();

		// Get params data from terminal ISO transaction.
		Object isoValue;
		for (int i = 0; i < isoFields.length; i++) {
			if (transactionFromTerminal.hasField(isoFields[i])) {
				isoValue = transactionFromTerminal.getValue(isoFields[i]);
				if (isoValue instanceof byte[]) {// Some fields are hexa ...
					parametersList.add(AppFunctions.hexString((byte[]) isoValue));
				} else { // others are strings.
					parametersList.add((String) isoValue);
				}
			} else {
				parametersList.add("");
			}
		}
		setParametersList(parametersList);
		if (traceLog.isTraceEnabled()) {
			traceLog.trace(" ----- Chamada da SP HSM -----");
			traceLog.trace(getTransactionToTrace(parametersList));
		}
		long beginExecution = System.currentTimeMillis();
		send();
		receive();
		long endExecution = System.currentTimeMillis();
		if (traceLog.isTraceEnabled()) {
			traceLog.trace("Resultado da {} :{}", storedProcedureName, getDatabaseReturn().get(0));
		}
		mediator.setDatabaseHsmReturn(getDatabaseReturn());// Send to mediator a
															// List of SP
															// response.
		return (endExecution - beginExecution);// Return the total execution
												// time.
	}

}
