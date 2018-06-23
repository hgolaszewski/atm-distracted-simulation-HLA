package hla.atm.gui;

import hla.rti.RTIexception;

public class GUIStartingPoint {

	public static void main(String[] args) {
		try {
			new GUIFederate().runFederate();
		} catch (RTIexception rtIexception) {
			rtIexception.printStackTrace();
		}
	}

}
