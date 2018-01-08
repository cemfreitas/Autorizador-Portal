package cemfreitas.autorizadorportal.MVC;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

/* This class compounds one of the four parts on the main screen.   
 * It shows the Connection status panel on the main screen. 
 */
public class ConnectionView extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3438253104171640737L;
	
	//Use a BlinkLabel interface to choose between a method which blinks or not	
	private BlinkLabelnterface lbHSMStatusConnect = new BlinkLabel();

	public ConnectionView() {
		//Setting border and layout
		setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Conexões ",
				TitledBorder.CENTER, TitledBorder.TOP));
		setLayout(new BorderLayout());

		// Creating Connection Status components		
		JLabel labelHSM = new JLabel("HSM :");		

		JPanel statusHSM = new JPanel();
		statusHSM.setLayout(new FlowLayout());
		statusHSM.add(labelHSM);
		statusHSM.add((Component) lbHSMStatusConnect);		

		add(statusHSM, BorderLayout.WEST);		
		
		//Set as disconected at the first time.
		setHSMConnectionOff();		
	}
	
	/* Change the connection status of the HSM. 
	 * 
	 */
	
	void setHSMConnectionOn() {
		lbHSMStatusConnect.setConnectedText("Conectado");		
	}
	
	void setHSMConnectionOff() {
		lbHSMStatusConnect.setDesconectedText("Desconectado");
	}
	
	void setHSMDisabled() {
		lbHSMStatusConnect.setDisbledText("Desabilitado");
	}	
	
}
