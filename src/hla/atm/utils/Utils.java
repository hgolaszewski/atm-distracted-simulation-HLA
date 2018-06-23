package hla.atm.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;

import hla.rti.LogicalTime;
import hla.rti.LogicalTimeInterval;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

public class Utils {

	public static final String CASH_MACHINE_AMBASSADOR_NAME = "CashMachineFederateAmbassador";
	public static final String CASH_MACHINE_FEDERATE_NAME = "CashMachineFederate";

	public static final String CLIENT_AMBASSADOR_NAME = "ClientFederateAmbassador";
	public static final String CLIENT_FEDERATE_NAME = "ClientFederate";

	public static final String GUI_AMBASSADOR_NAME = "GuiFederateAmbassador";
	public static final String GUI_FEDERATE_NAME = "GuiFederate";

	public static final String QUEUE_AMBASSADOR_NAME = "QueueFederateAmbassador";
	public static final String QUEUE_FEDERATE_NAME = "QueueFederate";

	public static final String SERVICE_AMBASSADOR_NAME = "ServiceFederateAmbassador";
	public static final String SERVICE_FEDERATE_NAME = "ServiceFederate";

	public static final String INT_TAG = "TAG";

	public static double convertTime(LogicalTime logicalTime) {
		return ((DoubleTime)logicalTime).getTime();
	}

	public static LogicalTime convertTime(double time) {
		return new DoubleTime(time);
	}

	public static Integer generateRandomInt(int a, int b) {
		return new Random().nextInt(a) + b;
	}

	public static LogicalTimeInterval convertInterval(double time) {
		return new DoubleTimeInterval(time);
	}

	public static void log(String message) {
		System.out.println(message);
	}

	public static void waitForSynchronization(String federateName) {
		log(federateName + ": $$$$$$$$$$$$$$$ Press any key to continue $$$$$$$$$$$$$$$");
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			reader.readLine();
		} catch(Exception e) {
			log(federateName + ": error while waiting for user input: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
