package cemfreitas.autorizadorportal.manager;

import java.awt.Frame;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JOptionPane;

import org.slf4j.Logger;

import cemfreitas.autorizadorportal.AutorizadorConstants;
import cemfreitas.autorizadorportal.AutorizadorConstants.ClientConnectionStatus;
import cemfreitas.autorizadorportal.AutorizadorConstants.TransactionPhase;
import cemfreitas.autorizadorportal.AutorizadorConstants.TransactionStatus;
import cemfreitas.autorizadorportal.manager.transaction.TransactionAcknowledge;
import cemfreitas.autorizadorportal.manager.transaction.TransactionAuthorization;
import cemfreitas.autorizadorportal.manager.transaction.TransactionCancellation;
import cemfreitas.autorizadorportal.manager.transaction.TransactionLogon;
import cemfreitas.autorizadorportal.manager.transaction.TransactionPurchase;
import cemfreitas.autorizadorportal.manager.transaction.TransactionReversal;
import cemfreitas.autorizadorportal.utils.AppFunctions;
import cemfreitas.autorizadorportal.utils.AutorizadorParams;
import cemfreitas.autorizadorportal.utils.Logging;

/* TransactionManager Class.
 * Implements a server to receive transactions ISO from a terminal.
 * For each connection received, starts a thread to process the transaction.
 */
public class TransactionManager {
	//Socket parameters.
	private static final int MAX_NUMBER_CONNECTION = 1000;
	private static final int LISTENING_PORT = AutorizadorParams.getValueAsInt("PortaEscuta");
	private static final int MIN_ISO_MIN_LENGTH = 14;

	//Logger for discarded transactions
	private Logger discardLog = Logging.getDiscard();

	private ServerSocket server = null;

	//Called by main method, execute a server socket. 
	public void perform() {

		Socket connection = null;
		try {
			server = new ServerSocket(LISTENING_PORT, MAX_NUMBER_CONNECTION);
		} catch (IOException e) {
			//If server socked could not be established, exit application. 
			JOptionPane.showMessageDialog(new Frame(), "Erro ao escutar na porta : " + LISTENING_PORT, "Portal Card",
					JOptionPane.ERROR_MESSAGE);
			System.exit(0);
			return;
		}

		while (true) {

			try {
				// Waiting for terminal connections 
				connection = server.accept();				

			} catch (IOException e) {
				continue;
			}
			//Starts thread to process transaction.
			ReceiveTransaction transaction = new ReceiveTransaction(connection);
			transaction.start();
		}
	}

	/*
	 * Inner class used to process a terminal transaction. Instantiates a
	 * mediator class and give it the connection streams, the mediator is
	 * responsible for process the transaction.
	 * 
	 * It is notified by mediator whenever the transaction status changes.
	 */

	private class Transaction extends Thread implements Observer {
		byte[] message;
		Manager transaction;
		Socket connection;

		//Receives connection streams from outer class.
		Transaction(byte[] message, Socket connection) {
			this.message = message;
			this.connection = connection;
		}

		@Override
		public void run() {
			try {
				//If it is a valid message, instantiate a proper class to manage the transaction according its type.
				String transactionType = getTransactionType(message);
				switch (transactionType) {
				case "0100":
					transaction = new TransactionAuthorization(message, connection);
					break;
				case "0200":
					transaction = new TransactionPurchase(message, connection);
					break;
				case "0420":
					transaction = new TransactionReversal(message, connection);
					break;
				case "0202":
					transaction = new TransactionAcknowledge(message, connection);
					break;
				case "0800":
					transaction = new TransactionLogon(message, connection);
					break;
				case "0400":
					transaction = new TransactionCancellation(message, connection);
					break;
				default://Invalid type. Discard it.
					StringBuilder sb = new StringBuilder();
					sb.append("Tipo de transacao invalida: ");
					sb.append(transactionType);
					sb.append("\n");
					sb.append(AppFunctions.dumpString(message));
					sb.append("\n");
					discardLog.info(sb.toString());//Log into discard log file.
					return;
				}

				transaction.addObservable(this);//Register as an Observable  
				transaction.perform();

			} catch (IOException e) {
				return;
			}
		}

		/*
		 * Observer method implementation. Receives the mediator notifications.
		 * *
		 */
		@Override
		public void update(Observable mediatorPushed, Object threadPushed) {
			if (mediatorPushed instanceof Mediator) {
				Mediator mediator = (Mediator) mediatorPushed;
				long threadId = (long) threadPushed;
				TransactionPhase currentPhase = mediator.getTransactionPhase();
				TransactionStatus status = null;
				switch (currentPhase) {
				case TRANSAC_UNPACK:
					// Add transaction data to main screen;
					TransactionMonitor.addTransaction(threadId, mediator.getTransactionData());
					break;
				case TRANSAC_COMPLETED:
					// Update monitor with the proper status transaction.
					String transactionType = mediator.getTransactionData().getCodigo();
					switch (transactionType) {
					case AutorizadorConstants.TRANSAC_REVERSAL_TYPE:
						status = TransactionStatus.TRANSAC_REVERTED;
						break;
					case AutorizadorConstants.TRANSAC_LOGON_TYPE:
						status = TransactionStatus.TRANSAC_LOGON;
						break;
					case AutorizadorConstants.TRANSAC_ACKNOWLEDGE_TYPE:
						status = TransactionStatus.TRANSAC_ACKNOWLEDGE;
						break;
					case AutorizadorConstants.TRANSAC_CANCELLED_TYPE:
						status = TransactionStatus.TRANSAC_CANCELLED;
						break;
					default:
						status = TransactionStatus.TRANSAC_COMPLETED;
						break;
					}
					TransactionMonitor.updateTransaction(threadId, status);
					break;
				case TRANSAC_AUT_ERROR:
					// Update monitor with not completed status transaction
					TransactionMonitor.updateTransaction(threadId, TransactionStatus.TRANSAC_NOT_COMPLETED);
					break;
				}
				//For each notifications other than UNPACK_PHASE, checks HSM connection and update main screen.
				if (currentPhase != TransactionPhase.TRANSAC_UNPACK) {
					if (mediator.isHsmDisconected()) {
						TransactionMonitor.updateHsmConnectionStatusView(ClientConnectionStatus.CLIENT_DISCONNECTED);
					} else {
						TransactionMonitor.updateHsmConnectionStatusView(ClientConnectionStatus.CLIENT_CONNECTED);
					}
				}
			}
		}

		private String getTransactionType(byte[] message) {
			String type = null;

			//Check whether transaction is ASCII or EBCDIC.
			if (message[2] != '0' && message[2] != '9') {//EBCDIC
				type = AppFunctions.ebcdicToAscii(message, 2, 4);
			} else { //ASCII				
				byte[] typeBytes = new byte[4];
				System.arraycopy(message, 2, typeBytes, 0, 4);
				type = new String(typeBytes);
			}

			return type;
		}
	}

	
	//Thread class responsible for receive transaction from a terminal 
	private class ReceiveTransaction extends Thread {
		Socket connection;
		byte[] message;

		public ReceiveTransaction(Socket connection) {
			this.connection = connection;
		}

		@Override
		public void run() {
			while (true) {
				//Receive transaction stream bytes
				message = receiveMessage(connection);				
				if (message != null) {
					//If received successfully, pass it to Transaction inner class  
					Transaction transaction = new Transaction(message, connection);
					transaction.start();
				} else {
					return;
				}
			}

		}
	}

	//Receives message from terminal
	private byte[] receiveMessage(Socket connection) {
		byte[] message = null;
		byte pbyte = 0;
		InputStream inputStream;
		try {
			inputStream = connection.getInputStream();
			pbyte = (byte) inputStream.read();
			message = new byte[inputStream.available()];
			inputStream.read(message);

			message = AppFunctions.concatenate(pbyte, message);

			if (message.length < MIN_ISO_MIN_LENGTH) {//Test whether is an ISO transaction.					
				discardLog.info(AppFunctions.dumpString(message) + "\n");//Log into discard log file.
				message = null;
			}

		} catch (IOException e) {
			//Log into discard log file.
			if (message != null) {
				discardLog.info("Erro ao receber mensagem :" + AppFunctions.dumpString(message) + "\n");
			} else {
				discardLog.info("Erro ao receber mensagem : null \n");
			}
			message = null;
		}

		return message;
	}
}
