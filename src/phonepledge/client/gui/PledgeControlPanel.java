/*
 *  This file is part of PhonePledge.
 *
 *  PhonePledge is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  PhonePledge is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PhonePledge.  If not, see <http://www.gnu.org/licenses/>.
 */
package phonepledge.client.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import phonepledge.Pledge;
import phonepledge.client.PledgeClient;
import phonepledge.client.StatusCommandListener;
import phonepledge.command.DisplayCommand;
import phonepledge.command.RevokePledgeCommand;
import phonepledge.command.StatusCommand;
import phonepledge.googlevoice.GoogleVoiceConnector;


/* ListDemo.java requires no other files. */
public class PledgeControlPanel extends JPanel implements 
	StatusCommandListener, ChangeListener {
	private static final long serialVersionUID = 1L;
	private final JTable table;
    private final MyTableModel tableModel;
    private static final String acceptString = "Accept";
    private static final String rejectString = "Reject";
    private static final String addString = "Add";
    private static final String nextString = "Next";
    private static final String pledgeString = "Pledge";
    private static final String slideString = "Slide";
    private final JButton rejectButton;
    private final JButton acceptButton;
    private final JTextField pledgeInput;
    private final PledgeClient pledgeClient;
    private final JRadioButton pledgeModeButton;
    private final JRadioButton slideModeButton;
    private final JSpinner tickerSpeedSpinner;
    private final JSpinner transitionSpeedSpinner;
    private GoogleVoiceConnector gvc = null;
    private Thread gvcThread = null;
    private String smsReplyMessage = "Thank you for your pledge!";
    
    public PledgeControlPanel() {
        super(new BorderLayout());
        
        tableModel = new MyTableModel();
        table = new JTable(tableModel);
        
        //table.setAutoResizeMode(JTable.AUTO_RESIZE);
        TableColumn col = table.getColumnModel().getColumn(0);
        col.setPreferredWidth(150);
        col.setMaxWidth(300);
        col.setMinWidth(100);
        col.setWidth(150);
        
        table.setDefaultRenderer(String.class, new MyTableCellRenderer(true));
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() == false) {
					int row = table.getSelectedRow();
					
					if (row == -1)
						return;
						
					Pledge p = (Pledge)tableModel.data.get(row);

					switch (p.getState()) {
					case Pledge.STATE_ACCEPTED:
						//acceptButton.setText("Resend");
						//rejectButton.setText("Revoke");
						rejectButton.setEnabled(true);
						acceptButton.setEnabled(true);
						break;
					case Pledge.STATE_REJECTED:
						rejectButton.setEnabled(false);
						//acceptButton.setText(acceptString);
						//rejectButton.setText(rejectString);
						break;
					case Pledge.STATE_UNDEFINED:
						rejectButton.setEnabled(true);
						acceptButton.setEnabled(true);
						//acceptButton.setText(acceptString);
						//rejectButton.setText(rejectString);
						break;
					}
				}
			}
        });
        
        table.addKeyListener(new TableKeyListener());
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        final JScrollPane listScrollPane = new JScrollPane(table);

        acceptButton = new JButton(acceptString);
        final AcceptRejectListener acceptRejectListener = new AcceptRejectListener();
        acceptButton.setActionCommand(acceptString);
        acceptButton.addActionListener(acceptRejectListener);
        acceptButton.setEnabled(false);
        acceptButton.setToolTipText("Accept selected pledge and queue it for display");
        //System.out.println("Button width=" + acceptButton.getWidth());
        //Dimension buttonSize = new Dimension(acceptButton.getWidth() + 20, acceptButton.getHeight());
        //acceptButton.setMinimumSize(buttonSize);
        //acceptButton.setPreferredSize(buttonSize);
        //acceptButton.setSize(buttonSize);

        rejectButton = new JButton(rejectString);
        rejectButton.setActionCommand(rejectString);
        rejectButton.addActionListener(acceptRejectListener);
        rejectButton.setEnabled(false);
        rejectButton.setToolTipText("Reject and do not display selected pledge");
        //rejectButton.setMinimumSize(buttonSize);
        //rejectButton.setPreferredSize(buttonSize);
        //rejectButton.setSize(buttonSize);
        
        final JButton addButton = new JButton(addString);
        AddListener addListener = new AddListener(addButton);
        addButton.setActionCommand(addString);
        addButton.addActionListener(addListener);
        addButton.setToolTipText("Add new pledge");

        final JButton nextButton = new JButton(nextString);
        nextButton.setActionCommand(nextString);
        nextButton.setToolTipText("Show next slide or queued pledge");
        nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (pledgeClient != null)
					pledgeClient.sendCommand(new DisplayCommand(DisplayCommand.CMD_NEXT_PLEDGE));
			}
        	
        });
        
        final ModeButtonListener modeButtonListener = new ModeButtonListener();
        slideModeButton = new JRadioButton(slideString);
        slideModeButton.addActionListener(modeButtonListener);
        slideModeButton.setToolTipText("Show slides instead of pledges. Accepting a new pledge will automatically show pledges again");
        
        pledgeModeButton = new JRadioButton(pledgeString);
        pledgeModeButton.addActionListener(modeButtonListener);
        pledgeModeButton.setSelected(true);
        pledgeModeButton.setToolTipText("Show pledges instead of slides. If no pledges are currently queued, then already shown pledges will be queued again");
        final ButtonGroup modeButtonGroup = new ButtonGroup();
        modeButtonGroup.add(slideModeButton);
        modeButtonGroup.add(pledgeModeButton);
        
        final JCheckBox smsReplyBox = new JCheckBox("SMS Reply");
        smsReplyBox.setSelected(false);
        smsReplyBox.setToolTipText("Send a reply SMS to the originator of every incoming pledge");
        smsReplyBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (gvc != null)
					gvc.setShouldSendReply(smsReplyBox.isSelected());
			}
        });
        
        pledgeInput = new JTextField(20);
        pledgeInput.addActionListener(addListener);
        pledgeInput.getDocument().addDocumentListener(addListener);
        pledgeInput.setToolTipText("Manually input a new pledge");
        
        tickerSpeedSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 500, 20));
        tickerSpeedSpinner.addChangeListener(this);
        tickerSpeedSpinner.setToolTipText("Set pledge ticker speed");
        transitionSpeedSpinner = new JSpinner(new SpinnerNumberModel(20, 0, 100, 1));
        transitionSpeedSpinner.addChangeListener(this);
        transitionSpeedSpinner.setToolTipText("Set pledge or slide transition timeout");
        
        final JPanel pledgeSpeedPane = new JPanel();
        pledgeSpeedPane.setLayout(new BoxLayout(pledgeSpeedPane,
                BoxLayout.Y_AXIS));
        
        final JPanel inputPane = new JPanel();
        inputPane.setLayout(new BoxLayout(inputPane,
                                           BoxLayout.LINE_AXIS));
        inputPane.add(pledgeInput);
        inputPane.add(addButton);
        inputPane.add(Box.createHorizontalStrut(5));
        inputPane.add(new JSeparator(SwingConstants.VERTICAL));
        inputPane.add(Box.createHorizontalStrut(5));
        inputPane.add(nextButton);
        
        //Create a panel that uses BoxLayout.
        final JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane,
                                           BoxLayout.LINE_AXIS));
        buttonPane.add(slideModeButton);
        buttonPane.add(pledgeModeButton);
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(smsReplyBox);
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(new JLabel("Ticker:"));
        buttonPane.add(tickerSpeedSpinner);
        buttonPane.add(Box.createHorizontalStrut(10));
        buttonPane.add(new JLabel("Transition:"));
        buttonPane.add(transitionSpeedSpinner);
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(rejectButton);
        buttonPane.add(acceptButton);
        buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        final JPanel bottomPane = new JPanel();
        bottomPane.setLayout(new BoxLayout(bottomPane,
                	BoxLayout.PAGE_AXIS));
        bottomPane.add(buttonPane);
        //bottomPane.add(Box.createVerticalStrut(5));
        bottomPane.add(new JSeparator(SwingConstants.HORIZONTAL));
        bottomPane.add(Box.createVerticalStrut(2));
        bottomPane.add(inputPane);
        bottomPane.add(Box.createVerticalStrut(2));
        
        add(listScrollPane, BorderLayout.CENTER);
        add(bottomPane, BorderLayout.PAGE_END);
        
        readProperties();
        
        pledgeClient = new PledgeClient(this);
    }
    
    private void readProperties() {
		Properties props = new Properties();
		
		try {
			props.load(new FileInputStream("control.properties"));
		} catch (FileNotFoundException e) {
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		String value;
		
		if ((value = (String)props.getProperty("SmsReplyMsg")) != null) {
			smsReplyMessage = value;
			System.out.println("SmsReplyMsg=" + value);
		} 
		
		if ((value = (String)props.getProperty("GoogleVoiceUser")) != null) {
			loginCredentials[0] = value;
			System.out.println("GoogleVoiceUser=" + value);
		}
		
		if ((value = (String)props.getProperty("GoogleVoicePassword")) != null) {
			loginCredentials[1] = value;
			System.out.println("GoogleVoicePassword=" + value);
		}
    }
    
    public boolean start() {
    	  if (!pledgeClient.start()) {
          	JOptionPane.showMessageDialog(getParent(),
          		    "Could not connect to the pledge server.\n" +
          		    "Please make sure the server is running before\n" +
          		    "starting the controller.",
          		    "Connection error",
          		    JOptionPane.ERROR_MESSAGE);
          	return false;
          }
    	  return true;
    }
    
    public void setGoogleVoiceConnector(GoogleVoiceConnector gvc) {
    	this.gvc = gvc;
        gvc.setReplyMessage(smsReplyMessage);
    }
    
    public void setPhoneNumber(String phoneNumber) {
    	pledgeClient.sendCommand(new DisplayCommand(DisplayCommand.CMD_SET_PHONE_NUMBER, phoneNumber));
    }
    
    class ModeButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand() == slideString) {
				pledgeClient.sendCommand(new DisplayCommand(DisplayCommand.CMD_SET_SLIDE_MODE));
			} else if (e.getActionCommand() == pledgeString) {
				pledgeClient.sendCommand(new DisplayCommand(DisplayCommand.CMD_SET_PLEDGE_MODE));
			}
		}
    }
    
    void setPledgeVerdictAtIndex(int index, boolean multirow, int state) {
		Pledge p;
    	int[] rows = table.getSelectedRows();
    	
		for (int i = 0; i < rows.length; i++) {
			index = rows[i];
			
			try {
				p = tableModel.data.get(index);
			} catch (ArrayIndexOutOfBoundsException ex) {
				System.err.println("No selected index");
				return;
			}

			int oldState = p.getState();

			p.setState(state);

			switch (state) {
			case Pledge.STATE_ACCEPTED:
				if (pledgeClient != null)
					pledgeClient.sendPledge(p);
				break;
			case Pledge.STATE_REJECTED:
				if (oldState == Pledge.STATE_ACCEPTED) {
					if (pledgeClient != null)
						pledgeClient.sendCommand(new RevokePledgeCommand(p));
				}
				break;
			case Pledge.STATE_UNDEFINED:
				break;
			}
			tableModel.fireTableRowsUpdated(index, index);
		}

		// Set the state of accept/reject buttons
		index = table.getSelectedRow();
		
		try {
			p = tableModel.data.get(index);
		} catch (ArrayIndexOutOfBoundsException ex) {
			System.err.println("No selected index");
			return;
		}
		
		switch (p.getState()) {
		case Pledge.STATE_ACCEPTED:
			//acceptButton.setText("Resend");
			//rejectButton.setText("Revoke");
			acceptButton.setEnabled(true);
			rejectButton.setEnabled(true);
			break;
		case Pledge.STATE_REJECTED:
			acceptButton.setEnabled(true);
			rejectButton.setEnabled(false);
			//acceptButton.setText(acceptString);
			//rejectButton.setText(rejectString);
			break;
		case Pledge.STATE_UNDEFINED:
			break;
		}

		// If single row is selected, make sure it is visible and 
		// move to the next one
		if (rows.length == 1 && (index < (tableModel.getRowCount() - 1))) {
			table.setRowSelectionInterval(index + 1, index + 1);
			Rectangle rect = table.getCellRect(index + 1, 0, true);
			table.scrollRectToVisible(rect);
		}
		
    }
    
    class TableKeyListener implements KeyListener {

		@Override
		public void keyPressed(KeyEvent e) {
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyTyped(KeyEvent e) {
			int index = table.getSelectedRow();
			if (e.getKeyChar() == 'a') {
				// Accept
				setPledgeVerdictAtIndex(index, false, Pledge.STATE_ACCEPTED);
			} else if (e.getKeyChar() == 'r') {
				// Reject
				setPledgeVerdictAtIndex(index, false, Pledge.STATE_REJECTED);
			}
		}
	}

	class MyTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		private String[] columnNames = { "From", "Text" };
		private Vector<Pledge> data = new Vector<Pledge>();
		// Note: This hash map does not allow conflicts since we are using the
		// hash itself as key
		private HashMap<Integer, Integer> rows = new HashMap<Integer, Integer>();

		MyTableModel() {
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public int getRowCount() {
			return data.size();
		}

		public String getColumnName(int col) {
			return columnNames[col];
		}

		public Object getValueAt(int row, int col) {
			if (col == 0)
				return data.get(row).getFrom();

			return data.get(row).getText();
		}

		public Class<?> getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}

		/*
		 * Don't need to implement this method unless your table's editable.
		 */
		public boolean isCellEditable(int row, int col) {
			return false;
		}

		public int getRow(Pledge p) {
			Integer row = rows.get(p.hashCode());
			
			if (row == null)
				return -1;
				
			return row.intValue();
		}
		
		public boolean addPledge(Pledge p) {
			if (rows.containsKey(p.hashCode()))
				return false;

			data.add(p);
			rows.put(p.hashCode(), data.size() - 1);
			this.fireTableDataChanged();

			return true;
		}
	}

	public class MyTableCellRenderer extends JLabel implements
	TableCellRenderer {
		private static final long serialVersionUID = 1L;
		boolean isBordered;

		public MyTableCellRenderer(boolean isBordered) {
			this.isBordered = isBordered;
			setOpaque(true); // MUST do this for background to show up.
		}

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			setText(value.toString());
			Pledge p = tableModel.data.get(row);

			switch (p.getState()) {
			case Pledge.STATE_UNDEFINED:
				if (p.isCanned())
					setBackground(Color.orange);
				else
					setBackground(table.getBackground());
				break;
			case Pledge.STATE_ACCEPTED:
				setBackground(Color.GREEN);
				break;
			case Pledge.STATE_REJECTED:
				setBackground(Color.RED);
				break;
			}
			if (isSelected) {
				Border border = new LineBorder(table.getSelectionBackground(),
						2); // LineBorder.createGrayLineBorder();
				setBorder(border);
				setBackground(getBackground().darker());
			} else
				setBorder(null);

			return this;
		}
	}

    //This listener is shared by the text field and the accept button.
    class AcceptRejectListener implements ActionListener {
       
        //Required by ActionListener.
        public void actionPerformed(ActionEvent e) {
			int index = table.getSelectedRow();
			
			if (index == -1)
				return;

			int state = Pledge.STATE_ACCEPTED;

			if (e.getActionCommand() == "Reject")
				state = Pledge.STATE_REJECTED;

			setPledgeVerdictAtIndex(index, true, state);
		}
    }

    //This listener is shared by the text field and the accept button.
    class AddListener implements ActionListener, DocumentListener {
        private boolean alreadyEnabled = false;
        private JButton button;

        public AddListener(JButton button) {
            this.button = button;
        }

        //Required by ActionListener.
        public void actionPerformed(ActionEvent e) {
            String name = pledgeInput.getText();

            //User didn't type in a unique name...
            if (name.equals("")) {
                Toolkit.getDefaultToolkit().beep();
                pledgeInput.requestFocusInWindow();
                pledgeInput.selectAll();
                return;
            }

        	Pledge p = new Pledge(pledgeInput.getText());
        	tableModel.addPledge(p);
        	int index = tableModel.data.size() - 1;
			
            //Reset the text field.
            pledgeInput.requestFocusInWindow();
            pledgeInput.setText("");

            //Select the new item and make it visible.
            table.setRowSelectionInterval(index, index);
            Rectangle rect = table.getCellRect(index, 0, true);
            table.scrollRectToVisible(rect);
        }

        //Required by DocumentListener.
        public void insertUpdate(DocumentEvent e) {
            enableButton();
        }

        //Required by DocumentListener.
        public void removeUpdate(DocumentEvent e) {
            handleEmptyTextField(e);
        }

        //Required by DocumentListener.
        public void changedUpdate(DocumentEvent e) {
            if (!handleEmptyTextField(e)) {
                enableButton();
            }
        }

        private void enableButton() {
            if (!alreadyEnabled) {
                button.setEnabled(true);
            }
        }

        private boolean handleEmptyTextField(DocumentEvent e) {
            if (e.getDocument().getLength() <= 0) {
                button.setEnabled(false);
                alreadyEnabled = false;
                return true;
            }
            return false;
        }
    }

    class ListUpdater implements Runnable {
    	Pledge p;
    	
    	public ListUpdater(Pledge p) {
    		this.p = p;
    		//System.out.println("New pledge " + p);
    	}
    	@Override
		public void run() {
			tableModel.addPledge(p);
		}
	}

	public void addPledge(Pledge p) {
		javax.swing.SwingUtilities.invokeLater(new ListUpdater(p));
	}

	@Override
	public void onStatusCommand(final StatusCommand cmd) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				switch (cmd.getType()) {
				case StatusCommand.CMD_DISPLAY_MODE_PLEDGE:
					pledgeModeButton.setSelected(true);
					slideModeButton.setSelected(false);
					break;
				case StatusCommand.CMD_DISPLAY_MODE_SLIDE:
					pledgeModeButton.setSelected(false);
					slideModeButton.setSelected(true);
					break;
				case StatusCommand.CMD_PLEDGE_LIST:
					Pledge[] list = (Pledge[]) cmd.getData();

					for (int i = 0; i < list.length; i++) {
						if (list[i] != null)
							tableModel.addPledge(list[i]);
					}
					break;
				case StatusCommand.CMD_TICKER_SPEED:
					tickerSpeedSpinner.setValue(cmd.getData());
					break;
				case StatusCommand.CMD_TRANSITION_TIME:
					transitionSpeedSpinner.setValue(cmd.getData());
					break;
				}
			}
		});
	}
    
	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == transitionSpeedSpinner) {
			Integer value = (Integer)transitionSpeedSpinner.getValue();
			//System.out.println("Transition is " + value);
			DisplayCommand cmd = new DisplayCommand(DisplayCommand.CMD_SET_TRANSITION_TIME, value);
			pledgeClient.sendCommand(cmd);
		}  else if (e.getSource() == tickerSpeedSpinner) {
			Integer value = (Integer)tickerSpeedSpinner.getValue();
			DisplayCommand cmd = new DisplayCommand(DisplayCommand.CMD_SET_TICKER_SPEED, value);
			pledgeClient.sendCommand(cmd);
		}
	}
	
	String loginCredentials[] = { "", "" };
	boolean sendSmsReply = false;
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    public static void createAndShowGUI(PledgeControlPanel ps) {
    	//Create and set up the window.
        JFrame frame = new JFrame("PhonePledge Controller");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = ps;
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
        GoogleVoiceConnector gvc = new GoogleVoiceConnector(ps);

    	gvc.setShouldSendReply(ps.sendSmsReply);

    	ps.setGoogleVoiceConnector(gvc);
    	
        while (true) {
        	if (ps.loginCredentials[0].length() == 0)
        		ps.loginCredentials = LoginDialog.showDialog(frame);

        	if (!gvc.connect(ps.loginCredentials[0], 
					ps.loginCredentials[1])) {
        		JOptionPane.showMessageDialog(ps.getParent(),
        				"Could not connect to the Google Voice account '" + 
        						ps.loginCredentials[0] + "'\n" +
        						"Please make sure the username and password is correct",
        						"Login Error",
        						JOptionPane.ERROR_MESSAGE);
        		ps.loginCredentials[0] = "";
        	} else {
        		break;
        	}
        }
        
    	ps.start();
		ps.gvcThread = new Thread(gvc);
		ps.gvcThread.start();
    }
    
    public static void main(String[] args) {
		int argc = 0;
		
		System.out.println("Launching control window");
		
		final PledgeControlPanel pledgeControl = new PledgeControlPanel();
		
		if (args.length == 2) {
			pledgeControl.loginCredentials[0] = args[argc++];
			pledgeControl.loginCredentials[1] = args[argc++];
		}
		

		System.out.println("Username: " + pledgeControl.loginCredentials[0]);
		
		while (argc < args.length) {
			if (args[argc].equals("-r")) {
				System.out.println("Setting SMS reply mode.");
				pledgeControl.sendSmsReply = true;
			}
			argc++; 
		}
		
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	createAndShowGUI(pledgeControl);
                /*
                 * Test Pledges
                for (int i = 0; i < 500; i++) {
                	pledgeControl.addPledge(new Pledge("This is a pledge to donate money $" + i));
                }
                */
            }
        });
    }
}