package phonepledge.client.gui;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginDialog extends JDialog implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static LoginDialog dialog;
	String credentials[] = { "", "" };
	boolean itsFirst = true;
	boolean itsKeep = false;
	JTextField usernameField = new JTextField(15);
	JPasswordField passwordField = new JPasswordField(15);
	JButton quit, submit;
	
	public LoginDialog(Frame frame) {
		super(frame, "Google Voice Login", true);
		//setTitle("Google Voice Login");
		setModal(true);
		getContentPane().setLayout(new GridLayout(3, 2));
		getContentPane().add(new JLabel("Username:", JLabel.RIGHT));
		getContentPane().add(usernameField);
		getContentPane().add(new JLabel("Password:", JLabel.RIGHT));
		getContentPane().add(passwordField);
		quit = new JButton("Quit");
		getContentPane().add(quit);
		submit = new JButton("Login");
		getContentPane().add(submit);
		submit.addActionListener(this);
		quit.addActionListener(this);
		usernameField.addActionListener(this);
		passwordField.addActionListener(this);
		pack();
	}
	public static String[] showDialog(Component frameComp) {
		Frame frame = JOptionPane.getFrameForComponent(frameComp);
		dialog = new LoginDialog(frame);
		dialog.setLocationRelativeTo(frameComp);
		dialog.setResizable(false);
		dialog.setVisible(true);
		return dialog.getCredentials();
	}
	public String[] getCredentials() {
		return credentials;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(submit)) {
			credentials[0] = usernameField.getText();
			credentials[1] = new String(passwordField.getPassword());
			
			if (credentials[0].length() == 0 || 
					credentials[1].length() == 0)
				return;
			
			setVisible(false);
		} else if (e.getSource().equals(quit)) {
			System.exit(0);
		} else if (e.getSource().equals(usernameField)) {
			if (usernameField.getText().length() > 0)
				passwordField.requestFocus();
		} else if (e.getSource().equals(passwordField)) {
			credentials[0] = usernameField.getText();
			credentials[1] = new String(passwordField.getPassword());
			
			if (credentials[0].length() == 0 || 
					credentials[1].length() == 0)
				return;
			
			setVisible(false);
		}
	}
}
