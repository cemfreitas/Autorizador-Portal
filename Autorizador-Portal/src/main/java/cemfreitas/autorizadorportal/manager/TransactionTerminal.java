package cemfreitas.autorizadorportal.manager;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOField;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.slf4j.Logger;

import cemfreitas.autorizadorportal.AutorizadorConstants;
import cemfreitas.autorizadorportal.AutorizadorConstants.TransactionStatus;
import cemfreitas.autorizadorportal.MVC.TransactionData;
import cemfreitas.autorizadorportal.utils.AppFunctions;
import cemfreitas.autorizadorportal.utils.AutorizadorParams;
import cemfreitas.autorizadorportal.utils.Logging;

/* TransactionTerminal class.
 * Extends abstract class TransactionBase and provides further
 * implementations for the Terminal transactions.
 */
public class TransactionTerminal extends TransactionBase {
	private static final String packageName = "config/ISO8583_Portal.xml";
	private Logger traceLog = Logging.getTrace();
	private GenericPackager packager;
	private Mediator mediator;
	private TransactionData transactionData;// Holds transaction info to show on view.

	//Logger for discarded transactions
	private Logger discardLog = Logging.getDiscard();

	public TransactionTerminal(Mediator mediator) throws AutorizadorException {
		this.mediator = mediator;
		transactionData = new TransactionData();
	}

	// Send the transaction response back.
	// It overrides the superclass implementation.
	@Override
	public void send() throws AutorizadorException {
		OutputStream outputStream;
		byte[] transaction;

		outputStream = mediator.getOutputStream(); // Get the outputstream from mediator.
		transaction = mediator.getTransactionToTerminal(); // Get the transaction from mediator.

		try {
			outputStream.write(transaction);
			if (traceLog.isTraceEnabled()) {
				traceLog.trace(" ----- Resposta enviada ao terminal -----");
				traceLog.trace(AppFunctions.hexdump(transaction));
			}

		} catch (IOException e) {
			throw new AutorizadorException("Erro ao enviar transacao para o terminal: " + e.getMessage());
		}

	}

	// Pack response transaction to be sent.
	@Override
	public void pack() throws AutorizadorException {
		byte[] transaction;
		ISOMsg isoTransaction = new ISOMsg();// Create a new ISO transaction.

		if (packager == null) {
			getPackager();
		}
		isoTransaction.setPackager(packager);

		List<String> isoFields = mediator.getDatabaseAutReturn();// Get DB
																	// return
																	// with the
																	// transaction
																	// Fields
																	// processed
																	// by the SP
																	// Authorization.

		try {

			// Populates the ISO transactions with the DB return.
			isoTransaction.set(new ISOField(0, isoFields.get(1).substring(1, isoFields.get(1).length())));

			for (int i = 2; i < isoFields.size(); i++) {
				if (isoFields.get(i).charAt(0) == '1') {
					isoTransaction.set(new ISOField(i, isoFields.get(i).substring(1, isoFields.get(i).length())));
				}
			}

			//Get ISO binary fields
			String de52 = isoFields.get(52);
			String de64 = isoFields.get(64);
			String de65 = isoFields.get(65);
			String de96 = isoFields.get(96);
			String de128 = isoFields.get(128);

			if (isoFields.size() == 129 && de52.charAt(0) == '1') {
				isoTransaction.set(52, AppFunctions.hex2byte(de52.substring(1, de52.length())));
			}

			if (isoFields.size() == 129 && de65.charAt(0) == '1') {
				isoTransaction.set(65, AppFunctions.hex2byte(de65.substring(1, de65.length())));
			}

			if (isoFields.size() == 129 && de64.charAt(0) == '1') {
				isoTransaction.set(64, AppFunctions.hex2byte(de64.substring(1, de64.length())));
			}

			if (isoFields.size() == 129 && de96.charAt(0) == '1') {
				isoTransaction.set(96, AppFunctions.hex2byte(de96.substring(1, de96.length())));
			}

			if (isoFields.size() == 129 && de128.charAt(0) == '1') {
				isoTransaction.set(128, AppFunctions.hex2byte(de128.substring(1, de128.length())));
			}

			transaction = isoTransaction.pack();// Pack the transaction.

			//Check whether is a EBCDIC transaction.
			if (mediator.isEbcdic()) {
				//If so, transform it in EBCDIC
				transaction = AppFunctions.asciiToEbcdic(transaction);
			}

			transaction = AppFunctions.addHeader(transaction);// Add a proper
																// header (it
																// contains the
																// size of
																// message).

			//Check whether message requires trailing char 0D
			if (AutorizadorParams.getValue("Trailing0D_Message") != null
					&& AutorizadorParams.getValue("Trailing0D_Message").trim().equalsIgnoreCase("S")) {
				transaction = AppFunctions.append((byte) 0x0D, transaction);				
			}

			mediator.setTransactionToTerminal(transaction);// Send to mediator the
															// binary packed
															// transaction
			mediator.setIsoTransactionToTerminal(isoTransaction);// Send to mediator
			// the ISO packed
			// transaction
			if (traceLog.isTraceEnabled()) {
				traceLog.trace(" ----- Transacao empacotada com sucesso -----");
			}

		} catch (ISOException e) {
			throw new AutorizadorException("Erro ao empacotar resposta para o terminal :" + e.getMessage());
		}
	}

	// Unpack transaction received from terminal
	@Override
	public void unpack() throws AutorizadorException {
		try {
			ISOMsg isoTransaction = new ISOMsg();// Create a new ISO
													// transaction.
			byte[] transaction = mediator.getTransactionFromTerminal();// Get terminal transaction from mediator.;

			if (traceLog.isTraceEnabled()) {
				traceLog.trace(" ----- Transacao vinda do terminal -----");
				traceLog.trace(getTransactionToTrace(transaction));
			}

			if (packager == null) {
				getPackager();
			}
			isoTransaction.setPackager(packager);

			if (!AppFunctions.checkHeader(transaction)) {// Check whether the
															// head size is
															// Correct
				StringBuilder sb = new StringBuilder();
				sb.append(
						"Erro ao desempacotar transacao do terminal: Transacao sem informacao de tamanho ou com tamanho invalido:");
				sb.append("\n");
				sb.append(AppFunctions.dumpString(transaction));
				sb.append("\n");
				discardLog.info(sb.toString());//Log into discard log file.				
				throw new AutorizadorException(sb.toString());
			}

			//Check whether transaction has end of file 0X0D char
			char lastChar = (char) transaction[transaction.length - 1];
			if (lastChar == 0x0D) {
				transaction = AppFunctions.copyBytes(transaction, 2, transaction.length - 2);// Remove header and EOF char before unpack.
			} else {
				transaction = AppFunctions.copyBytes(transaction, 2, transaction.length - 1);// Remove only header before unpack.
			}

			char firstChar = (char) transaction[0];
			if (firstChar != '0' && firstChar != '9') {// Message in EBCDIC
				mediator.setEbcdic(true);
				transaction = AppFunctions.ebcdicToAsciiBytes(transaction);
			} else {
				mediator.setEbcdic(false);
			}

			isoTransaction.unpack(transaction);// Unpack it.

			try {
				transactionData.setCodigo(isoTransaction.getMTI()); // Get cod transaction

			} catch (ISOException e) {

			}

			if (isoTransaction.hasField(12) && isoTransaction.hasField(13)) {// Get
																				// date
																				// and
																				// time
				transactionData.setData(isoTransaction.getValue(13) + " " + isoTransaction.getValue(12));
			}

			if (isoTransaction.hasField(3)) {// Get processing code
				transactionData.setProcesso((String) isoTransaction.getValue(3));
			}

			if (isoTransaction.hasField(42)) {// Get Estbelecimento
				transactionData.setEstabelecimento((String) isoTransaction.getValue(42));
			}
			if (isoTransaction.hasField(63)) {// Get De63
				transactionData.setNSU((String) isoTransaction.getValue(63));
			}
			if (isoTransaction.hasField(35)) {// Get number of the card
				transactionData.setNumCartao((String) isoTransaction.getValue(35));
			} else if (isoTransaction.hasField(2)) {
				transactionData.setNumCartao((String) isoTransaction.getValue(2));
			}
			transactionData.setStatus(TransactionStatus.TRANSAC_NEW);// Set status/ to new transaction

			if (isoTransaction.hasField(4)) { // Get the value of transaction
				transactionData.setValor(AppFunctions.parseAmount((String) isoTransaction.getValue(4)));
			}

			mediator.setTransactionData(transactionData);

			mediator.setIsoTransactionFromTerminal(isoTransaction);// Send ISO
																	// transaction
																	// to mediator.

			checkHSMTransaction(isoTransaction);// Verify whether is a HSM
												// transaction and set a flag.
			if (traceLog.isTraceEnabled()) {
				traceLog.trace(" ----- Transacao desempacotada com sucesso -----");
			}

		} catch (ISOException e) {
			StringBuilder sb = new StringBuilder();
			sb.append("Erro ao desempacotar transacao do terminal: ");
			sb.append(e.getMessage());
			sb.append("\n");
			sb.append(AppFunctions.dumpString(mediator.getTransactionFromTerminal()));
			sb.append("\n");
			//Log into discard log file.
			discardLog.info(sb.toString());
			throw new AutorizadorException(sb.toString());
		}

	}

	private void getPackager() throws AutorizadorException {
		try {
			packager = new GenericPackager(packageName);

		} catch (ISOException e) {
			throw new AutorizadorException("Erro ao configurar o empacotador JPOS");
		}
	}

	// Verify according some rules whether is a HSM transaction, then set the
	// proper flag.
	private void checkHSMTransaction(ISOMsg isoTransaction) throws ISOException {
		String de52, mti;

		if (!isoTransaction.hasField(52) || !TransactionMonitor.isHsmEnabled) {
			mediator.setHSMTransaction(false);
			return;
		}

		mti = isoTransaction.getMTI();
		de52 = isoTransaction.getString(52);

		if ((!mti.equals(AutorizadorConstants.TRANSAC_AUTHORIZATION_TYPE)
				|| !mti.equals(AutorizadorConstants.TRANSAC_PURCHASE_TYPE)) && (de52.equals(""))) {// HSM rule			
			mediator.setHSMTransaction(false);
		} else {
			mediator.setHSMTransaction(true);
		}

	}

	// Not used in this class implementation
	@Override
	public long doTransaction() throws AutorizadorException {
		return 0;
	}

	// Not used in this class implementation
	@Override
	public Long call() throws Exception {
		return null;
	}

}
