package hla.atm.gui.visualization;

import static hla.atm.config.Config.QUEUE_SIZE_LIMIT;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

public class GUI implements Runnable {

	private List<Integer> clientIds = new ArrayList<>();
	private boolean serviceActive = false;
	private boolean ATMActive = true;
	private DrawingBoard drawingBoard;

	@Override
	public void run() {
		JFrame window = new JFrame();
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout(5, 5));
		drawingBoard = new DrawingBoard();
		mainPanel.add(drawingBoard, BorderLayout.CENTER);
		window.setContentPane(mainPanel);
		window.pack();
		window.setVisible(true);
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	class DrawingBoard extends JPanel {

		private static final int WIDTH = 320;
		private static final int HEIGHT = 120;
		private static final int SQUARE_SIZE = 20;
		private static final int ATM_X = 20;
		private static final int ATM_Y = 20;
		private static final int SERVICE_X = 20;
		private static final int SERVICE_Y = 80;


		DrawingBoard() {
			setOpaque(true);
			setBackground(Color.gray);
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(WIDTH, HEIGHT);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			drawATM(g);
			drawService(g);
			drawQueue(g);
		}

		private void drawQueue(Graphics g) {
			int x = ATM_X + 30;
			int size = 0;
			int sizeQueue = clientIds.size();
			for (int i = 0; i < sizeQueue; i++) {
				g.setColor(Color.BLACK);
				g.drawRect(x, ATM_Y, SQUARE_SIZE, SQUARE_SIZE);
				g.setColor(Color.cyan);
				g.fillOval(x, ATM_Y, SQUARE_SIZE, SQUARE_SIZE);
				x += 25;
				size++;
			}

			while(size < QUEUE_SIZE_LIMIT) {
				g.setColor(Color.BLACK);
				g.drawRect(x, 20, SQUARE_SIZE, SQUARE_SIZE);
				x += 25;
				size++;
			}
		}

		private void drawService(Graphics g) {
			g.setColor(Color.BLACK);
			g.drawRect(SERVICE_X, SERVICE_Y, SQUARE_SIZE, SQUARE_SIZE);
			if(!serviceActive) {
				g.setColor(Color.RED);
			} else {
				g.setColor(Color.GREEN);
			}
			g.fillRect(SERVICE_X, SERVICE_Y, SQUARE_SIZE, SQUARE_SIZE);
			g.setColor(Color.BLACK);
			g.drawString("SERVICE", SERVICE_X - 10, SERVICE_Y - 7);
		}

		private void drawATM(Graphics g) {
			g.setColor(Color.BLACK);
			g.drawRect(ATM_X, ATM_Y, SQUARE_SIZE, SQUARE_SIZE);
			if(ATMActive) {
				g.setColor(Color.GREEN);
			} else {
				g.setColor(Color.RED);
			}
			g.fillRect(ATM_X, ATM_Y, SQUARE_SIZE, SQUARE_SIZE);
			g.setColor(Color.BLACK);
			g.drawString("ATM", ATM_X, ATM_Y - 7);
		}
	}

	public void repaint() {
		this.drawingBoard.repaint();
		this.drawingBoard.revalidate();
	}

	public List<Integer> getClientIds() {
		return clientIds;
	}

	public void setServiceActive(boolean serviceActive) {
		this.serviceActive = serviceActive;
	}

	public void setATMActive(boolean ATMActive) {
		this.ATMActive = ATMActive;
	}

}
