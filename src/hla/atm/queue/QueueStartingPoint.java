package hla.atm.queue;

import hla.rti.RTIexception;

public class QueueStartingPoint {

	public static void main(String[] args) {
		try {
			new QueueFederate().runFederate();
		} catch (RTIexception rtIexception) {
			rtIexception.printStackTrace();
		}
	}

}
