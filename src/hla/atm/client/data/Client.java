package hla.atm.client.data;

import java.util.Objects;

public class Client {

	private static int seq = 1;
	private int id = seq++;

	public int getId() {
		return id;
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
