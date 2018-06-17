package hla.atm.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;

import hla.rti.LogicalTime;
import hla.rti.LogicalTimeInterval;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

public class Utils {

	public static final String SYNC_POINT = "ATM_FEDERATION_SYNC_POINT";

	public static double convertTime(LogicalTime logicalTime) {
		return ((DoubleTime)logicalTime).getTime();
	}

	public static LogicalTime convertTime(double time) {
		return new DoubleTime(time);
	}

	public static LogicalTimeInterval convertInterval(double time) {
		return new DoubleTimeInterval(time);
	}

	public static double randomTime() {
		Random random = new Random();
		return 1 + (4 * random.nextDouble());
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
