package hla.atm.cashmachine;

public class CashMachineStartingPoint {

	public static void main(String[] args) {
		try {
			new CashMachineFederate().runFederate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
