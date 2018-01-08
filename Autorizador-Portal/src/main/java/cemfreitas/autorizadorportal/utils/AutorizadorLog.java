package cemfreitas.autorizadorportal.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;

import cemfreitas.autorizadorportal.manager.Mediator;

/* AutorizadorLog class.
 * Used from TransactionManager, builds all log information to be log. * 
 */
public class AutorizadorLog {
	private Logger logger = Logging.getLogger();
	private StringBuffer logAutorization;
	public Mediator mediator;

	public AutorizadorLog(Mediator mediator) {
		this.mediator = mediator;
		logAutorization = new StringBuffer();
	}	

	private void logTransactionFromTerminal() {
		logAutorization.append("*********** Inicio da transacao ***********\n");
		logAutorization.append("Terminal  :");
		if (mediator.getTransactionFromTerminal() != null) {
			logAutorization.append(AppFunctions.dumpString(mediator.getTransactionFromTerminal()) + "\n");
		} else {
			logAutorization.append(mediator.getAutException().getMessage() + "\n");
			return;
		}
		logAutorization.append("Desempacotando transacao do Terminal: ");
		if (mediator.getIsoTransactionFromTerminal() != null) {
			logAutorization.append(getTransactionFields(mediator.getIsoTransactionFromTerminal()) + "\n");
		} else {
			logAutorization.append(mediator.getAutException().getMessage() + "\n");
			return;
		}
	}

	private void logHSM() {
		if (mediator.isHSMTransaction()) {
			logAutorization.append("Executando procedure HSM :");
			if (mediator.getDatabaseHsmReturn() != null) {
				logAutorization.append("Procedure HSM executada com sucesso. ");
				logAutorization.append("Tempo de resposta :");
				logAutorization.append(mediator.getHsmDbExecutionTime() + " milisegundos.\n");
				logAutorization.append("Enviando transacao para o HSM :");
				if (mediator.getTransactionFromHSM() != null) {
					logAutorization.append("OK \n");
					logAutorization.append("Tempo de resposta :");
					logAutorization.append(mediator.getHsmExecutionTime() + " milisegundos.\n\n");
				} else {
					if (mediator.getHsmException() != null) {
						logAutorization.append(mediator.getHsmException().getMessage() + "\n\n");
					}
				}
			} else {
				if (mediator.getHsmException() != null) {
					logAutorization.append(mediator.getHsmException().getMessage() + "\n\n");
				}
			}
		}
	}

	private void logProcedureAut() {
		logAutorization.append("Executando procedure Autorizacao :");
		if (mediator.getDatabaseAutReturn() != null) {
			logAutorization.append("Procedure autorizacao executada com sucesso. ");
			logAutorization.append("Tempo de resposta :");
			logAutorization.append(mediator.getAutDbExecutionTime() + " milisegundos.\n\n");
		} else {
			if (mediator.getAutException() != null) {
				logAutorization.append(mediator.getAutException().getMessage() + "\n\n");
			}
			return;
		}
	}

	private void logTerminalResponse() {
		logAutorization.append("Empacotando resposta para o Terminal :");
		if (mediator.getIsoTransactionToTerminal() != null) {
			logAutorization.append(getTransactionFields(mediator.getIsoTransactionToTerminal()) + "\n");
			logAutorization.append("Enviando resposta :");
			if (mediator.getAutException() == null) {
				if (mediator.getTransactionToTerminal() != null) {
					logAutorization.append(AppFunctions.dumpString(mediator.getTransactionToTerminal()) + "\n\n");
				}
			} else {
				if (mediator.getAutException() != null) {
					logAutorization.append(mediator.getAutException().getMessage() + "\n\n");
				}
			}
		} else {
			if (mediator.getAutException() != null) {
				logAutorization.append(mediator.getAutException().getMessage() + "\n\n");
			}
		}
	}

	//log it, according to the proper type transaction.
	public void log() {
		logTransactionFromTerminal();
		switch (mediator.getClass().getSimpleName()) {
		case "TransactionPurchase" :			
			logHSM();
			logProcedureAut();
			logTerminalResponse();
			break;
		case "TransactionAuthorization" :
			logHSM();
			logProcedureAut();
			logTerminalResponse();
			break;
		case "TransactionReversal" :
			logProcedureAut();
			logTerminalResponse();
			break;
		case "TransactionLogon" :
			logProcedureAut();
			logTerminalResponse();
			break;
		case "TransactionAcknowledge" :
			logProcedureAut();			
			break;
		}		
		if (!mediator.isTimeOut()) {
			logAutorization.append("*********** Fim da transacao ***********  ");
			logAutorization.append("Tempo total gasto :");
			logAutorization.append(mediator.getAutExecutionTime());
			logAutorization.append(" milisegundos\n");
		}
		logger.info(logAutorization.toString());
	}

	//Extracts fields from ISO transactions to log.
	private String getTransactionFields(ISOMsg isoTransaction) {
		ISOMsg msgToLog = (ISOMsg) isoTransaction.clone();
		ByteArrayOutputStream bAos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(bAos);
		int[] dEsToHide = new int[] { 2, 20, 23, 35, 45, 52, 55, 125 };// Hide
																		// some
																		// sensitive
																		// data
																		// from
																		// user's
																		// transactions
																		// before
																		// logging.

		for (int i = 0; i < dEsToHide.length; i++) {
			if (msgToLog.hasField(dEsToHide[i])) {
				msgToLog.set(dEsToHide[i], "**********");
			}
		}
		msgToLog.dump(ps, "");
		ps.flush();
		return bAos.toString();

	}

}
