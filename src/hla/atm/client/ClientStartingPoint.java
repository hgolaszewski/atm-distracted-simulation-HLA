package hla.atm.client;

import hla.rti.RTIexception;

public class ClientStartingPoint {

	public static void main(String[] args) {
		try {
			new ClientFederate().runFederate();
		} catch (RTIexception rtIexception) {
			rtIexception.printStackTrace();
		}
	}

}
