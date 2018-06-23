package hla.atm.gui.swing;

import static hla.atm.utils.config.Config.QUEUE_SIZE_LIMIT;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

public class GUI implements Runnable {

	private List<Integer> clientIds = new ArrayList<>();

	public void setServiceActive(boolean serviceActive) {
		this.serviceActive = serviceActive;
	}

	public void setATMActive(boolean ATMActive) {
		this.ATMActive = ATMActive;
	}

	private boolean serviceActive = false;
	private boolean ATMActive = true;

	private JFrame window;
	private JPanel mainPanel;
	private DrawingBoard drawingBoard;

	@Override
	public void run() {
		window = new JFrame();
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout(5, 5));
		drawingBoard = new DrawingBoard();
		mainPanel.add(drawingBoard, BorderLayout.CENTER);
		window.setContentPane(mainPanel);
		window.pack();
		window.setVisible(true);
	}

	class DrawingBoard extends JPanel {

		private static final int WIDTH = 500;
		private static final int HEIGHT = 150;

		public DrawingBoard() {
			setOpaque(true);
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(WIDTH, HEIGHT);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			// DRAW ATM
			if(ATMActive) {
				g.setColor(Color.GREEN);
			} else {
				g.setColor(Color.RED);
			}

			g.fillRect(20, 20, 20, 20);
			g.setColor(Color.BLACK);
			g.drawString("ATM", 20, 15);

			// DRAW Service
			if(!serviceActive) {
				g.setColor(Color.RED);
			} else {
				g.setColor(Color.GREEN);
			}

			g.fillRect(20, 80, 20, 20);
			g.setColor(Color.BLACK);
			g.drawString("Service", 20, 75);

			int x = 50;
			int size = 0;

			int sizeQueue = clientIds.size();
			for (int i = 0; i < sizeQueue; i++) {
				g.setColor(Color.BLACK);
				g.drawRect(x, 20, 20, 20);
				g.setColor(Color.GRAY);
				g.fillOval(x, 20, 20, 20);
				x = x + 25;
				size++;
			}

			while(size < QUEUE_SIZE_LIMIT) {
				g.setColor(Color.BLACK);
				g.drawRect(x, 20, 20, 20);
				x = x + 25;
				size++;
			}
		}
	}

	public void repaint() {
		this.drawingBoard.repaint();
		this.drawingBoard.revalidate();
	}

	public List<Integer> getClientIds() {
		return clientIds;
	}

}
