package hla.atm.client.data;

import java.util.Objects;

public class Client {

	private static int seq = 1;
	private int id = seq++;

	private double leaveTime = 0.0;

	public int getId() {
		return id;
	}

	public double getLeaveTime() {
		return leaveTime;
	}

	public void setLeaveTime(double leaveTime) {
		this.leaveTime = leaveTime;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Client client = (Client) o;
		return id == client.id;
	}

	@Override
	public int hashCode() {

		return Objects.hash(id);
	}
}
