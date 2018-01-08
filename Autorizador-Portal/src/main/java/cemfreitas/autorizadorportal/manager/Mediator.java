package cemfreitas.autorizadorportal.manager;

import java.io.OutputStream;
import java.util.List;

import org.jpos.iso.ISOMsg;

import cemfreitas.autorizadorportal.AutorizadorConstants.TransactionPhase;
import cemfreitas.autorizadorportal.MVC.TransactionData;

/* Mediator interface.
 * Implemented by TransactionMediator class and provides service methods to all
 * classes which communicates with mediator.  
 */
public interface Mediator {	
	byte[] getTransactionFromTerminal();	
	ISOMsg getIsoTransactionFromTerminal();		
	List<String> getDatabaseHsmReturn();	
	List<String> getDatabaseAutReturn();		
	OutputStream getOutputStream();	
	byte[] getTransactionToTerminal();		
	ISOMsg getIsoTransactionToTerminal();	
	byte[] getTransactionFromHSM();	
	TransactionPhase getTransactionPhase();	
	TransactionData getTransactionData();	
	long getHsmDbExecutionTime();
	long getHsmExecutionTime();
	long getAutDbExecutionTime();
	long getAutExecutionTime();
	Exception getAutException();	
	Exception getHsmException();
	
	boolean isHSMTransaction();	
	boolean isHsmDisconected();	
	boolean isTimeOut();
	boolean isEbcdic();
	
	void setIsoTransactionFromTerminal(ISOMsg isoTransactionFromTerminal);
	void setHSMTransaction(boolean isHSMTransaction);		
	void setDatabaseHsmReturn(List<String> databaseHsmReturn);
	void setDatabaseAutReturn(List<String> databaseAutReturn);	
	void setTransactionToTerminal(byte[] transactionToTerminal);	
	void setIsoTransactionToTerminal(ISOMsg isoTransactionToTerminal);
	void setTransactionFromHSM(byte[] transactionFromHSM);
	void setTransactionData(TransactionData transactionData);	
	void setHsmDisconected(boolean isHsmDisconected);		
	void setEbcdic(boolean isEbcdic);
}
