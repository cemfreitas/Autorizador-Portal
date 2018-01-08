package cemfreitas.autorizadorportal.MVC;

import cemfreitas.autorizadorportal.AutorizadorConstants.ClientConnectionStatus;
import cemfreitas.autorizadorportal.AutorizadorConstants.TransactionStatus;

public interface Controller {
	void turnUpdateOn();
	void turnUpdateOff();
	void setConnectStatusHSM(ClientConnectionStatus status);	
	void inertTransaction(long threadId, TransactionData transactionData);
	void updateTransactionStatus(long threadId, TransactionStatus status);
	void updateStatistics(TransactionStatistic statistics);
	void setApplicationVersion(String version);
	void shutDownApplication();
}
