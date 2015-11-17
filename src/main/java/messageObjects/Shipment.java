package messageObjects;

import java.io.Serializable;

import FIPA.DateTime;
import jade.core.AID;

public class Shipment implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int xCoordStart;
	private int yCoordStart;
	private int xCoordDest;
	private int yCoordDest;
	private AID customerID;
	private AID carrierID;
	private int cost;
	private int weight;
	private DateTime delivery;
	private DateTime pickup;
	private DateTime latestDeliveryConstraint;
	private DateTime earliestpickupConstraint;
	private DateTime earliestDeliveryConstraint;
	private DateTime latestpickupConstraint;
	//public enum constraints = [REFRIGERATOR, ONTIMEDELIVERY];
	
	public Shipment(int x1, int y1, int x2, int y2) {
		this.xCoordStart = x1;
		this.yCoordStart = y1;
		this.xCoordDest = x2;
		this.yCoordDest = y2;
	}

	public AID getCarrierID() {
		return carrierID;
	}

	public void setCarrierID(AID carrierID) {
		this.carrierID = carrierID;
	}

	public int getCost() {
		return cost;
	}

	public void setCost(int cost) {
		this.cost = cost;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public DateTime getDelivery() {
		return delivery;
	}

	public void setDelivery(DateTime delivery) {
		this.delivery = delivery;
	}

	public DateTime getPickup() {
		return pickup;
	}

	public void setPickup(DateTime pickup) {
		this.pickup = pickup;
	}

	public AID getCustomerID() {
		return customerID;
	}

	public void setCustomerID(AID customerID) {
		this.customerID = customerID;
	}

	@Override
	public String toString() {
		return "Shipment [xCoordStart=" + xCoordStart + ", yCoordStart="
				+ yCoordStart + ", xCoordDest=" + xCoordDest + ", yCoordDest="
				+ yCoordDest + ", customerID=" + customerID + ", carrierID="
				+ carrierID + ", cost=" + cost + ", weight=" + weight
				+ ", delivery=" + delivery + ", pickup=" + pickup
				+ ", latestDeliveryConstraint=" + latestDeliveryConstraint
				+ ", earliestpickupConstraint=" + earliestpickupConstraint
				+ ", earliestDeliveryConstraint=" + earliestDeliveryConstraint
				+ ", latestpickupConstraint=" + latestpickupConstraint + "]";
	}
}
