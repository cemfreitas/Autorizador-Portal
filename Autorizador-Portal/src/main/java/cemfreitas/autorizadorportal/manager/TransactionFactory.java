package cemfreitas.autorizadorportal.manager;

/* TransactionFactory class.
 * Implements Factory pattern.
 * Encapsulates the Transaction classes instantiation.
 */
public class TransactionFactory extends TransactionFactoryBase {

	@Override
	public Transaction createTerminalTransaction(Mediator mediator) throws AutorizadorException {		
		return new TransactionTerminal(mediator);
	}

	@Override
	public Transaction createHsmTransaction(Mediator mediator) throws AutorizadorException {		
		return new TransactionHSM(mediator);
	}

	@Override
	public Transaction createHsmDbTransactionDES(Mediator mediator) throws AutorizadorException {
		return new TransactionHsmDESDB(mediator);
	}
	
	@Override
	public Transaction createHsmDbTransaction3DES(Mediator mediator) throws AutorizadorException {
		return new TransactionHsm3DESDB(mediator);
	}

	@Override
	public Transaction createAutDbTransaction(Mediator mediator) throws AutorizadorException {
		return new TransactionAutorizationDB(mediator);
	}
}
