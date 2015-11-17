package messageObjects;

import java.io.Serializable;
import java.util.UUID;

import jade.core.AID;

public class Shipment implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5905741624012143495L;
	private Coordinates start;
	private Coordinates dest;
	private AID customerID;
	private AID carrierID;
	private double cost;
	private int weight;
	private UUID id;
	
	public Shipment(Coordinates start, Coordinates dest) {
		this.setStart(start);
		this.setDest(dest);
	}

	public AID getCarrierID() {
		return carrierID;
	}

	public void setCarrierID(AID carrierID) {
		this.carrierID = carrierID;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public AID getCustomerID() {
		return customerID;
	}

	public void setCustomerID(AID customerID) {
		this.customerID = customerID;
	}

	@Override
	public String toString() {
		return "Shipment [xCoordStart=" + getStart().getX() + ", yCoordStart="
				+ getStart().getY() + ", xCoordDest=" + getDest().getX() + ", yCoordDest="
				+ getDest().getY() + ", customerID=" + customerID + ", carrierID="
				+ carrierID + ", cost=" + cost + ", weight=" + weight + "]";
	}
	
	public boolean equals(Shipment s) {
		if(this.id == s.getId()) {
			return true;
		}
		return false;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Coordinates getStart() {
		return start;
	}

	public void setStart(Coordinates start) {
		this.start = start;
	}

	public Coordinates getDest() {
		return dest;
	}

	public void setDest(Coordinates dest) {
		this.dest = dest;
	}
}
