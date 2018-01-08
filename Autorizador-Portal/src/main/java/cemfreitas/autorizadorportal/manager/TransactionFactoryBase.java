package cemfreitas.autorizadorportal.manager;

public abstract class TransactionFactoryBase {
	public abstract Transaction createTerminalTransaction(Mediator mediator) throws AutorizadorException;	
	public abstract Transaction createHsmTransaction(Mediator mediator) throws AutorizadorException;
	public abstract Transaction createHsmDbTransactionDES(Mediator mediator) throws AutorizadorException;
	public abstract Transaction createHsmDbTransaction3DES(Mediator mediator) throws AutorizadorException;
	public abstract Transaction createAutDbTransaction(Mediator mediator) throws AutorizadorException;
}
