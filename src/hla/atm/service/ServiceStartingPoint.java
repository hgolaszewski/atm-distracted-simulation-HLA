package hla.atm.service;

import hla.rti.RTIexception;

public class ServiceStartingPoint {

	public static void main(String[] args) {
		try {
			new ServiceFederate().runFederate();
		} catch (RTIexception rtIexception) {
			rtIexception.printStackTrace();
		}
	}

}
