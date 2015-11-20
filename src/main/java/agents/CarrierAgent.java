package agents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import messageObjects.Coordinates;
import messageObjects.Shipment;

public class CarrierAgent extends Agent {
	
	private ArrayList<Shipment> shipments;
	
	//Carrier infos 
	private boolean onMyWay;
	private double currentCost;
	private int remainingCapacity;
	private int maxCapacity;
	private Coordinates currentPos;

	
	protected void setup() {
	
		System.out.println("CA: setup");
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("carrier");
		sd.setName("SoS_mas_logistics");
		dfd.addServices(sd);
		
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		shipments = new ArrayList<Shipment>();
		
		//lege maximale kapazität fest
		remainingCapacity = (int)(Math.random() * 900) + 100;
		maxCapacity = remainingCapacity;
		currentPos = new Coordinates((int)Math.floor(Math.random() * 100), (int)Math.floor(Math.random() * 100));
		onMyWay = false;
		currentCost = 0;
		shipments = new ArrayList<Shipment>();
		addBehaviour(new ReceiveShipmentMessageBehaviour());
		
		//check alle 60 sec ob zumindest eine lieferung vorhanden, wenn ja fahr los
		addBehaviour(new TickerBehaviour(this, 60000) {
			protected void onTick() {
				if(shipments.size() > 0 && !onMyWay)
				{
					System.out.println("CA: delivery");
					onMyWay = true;
					addBehaviour(new DeliveryBehaviour());
				}
			}
		} );
	}
	
	
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("serial")
	private class ReceiveShipmentMessageBehaviour extends CyclicBehaviour {

		@Override
		public void action() {
//			System.out.println("CA: Action");
			//empfange neue nachrichten
			ACLMessage msg = myAgent.receive();
			if (msg != null) {
				try {
					System.out.println("CA: message received - " + msg.getPerformative() + " " + msg.getContentObject());
				} catch (UnreadableException e1) {
					e1.printStackTrace();
				}

				if(msg.getPerformative() == ACLMessage.REQUEST) {
					//verarbeite neuen request
					myAgent.addBehaviour(new ComputeShipmentCostBehaviour(msg));
				}
				if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
					//füge shipment zur liste hinzu
					System.out.println("CA: accept proposal");
					try {
						shipments.add((Shipment)msg.getContentObject());
						currentCost = ((Shipment) msg.getContentObject()).getCost();

						remainingCapacity -= ((Shipment) msg.getContentObject()).getWeight();
						System.out.println("CA: adding shipment");
						
					} catch (UnreadableException e) {
						e.printStackTrace();
					}
				}
				
			}
			
			
		}
		
	}
	
	@SuppressWarnings("serial")
	private class ComputeShipmentCostBehaviour extends OneShotBehaviour {
		
		ACLMessage req;
		
		public ComputeShipmentCostBehaviour(ACLMessage req) {
			System.out.println("CA: new ComputShipmentCostBehaviour");
			this.req = req;
		}

		@Override
		public void action() {
			//berechne kosten und sende antwort
			System.out.println("CA: action");
			try {
				Shipment s = (Shipment)req.getContentObject();
				
				ACLMessage reply = req.createReply();
				
				//bestätige nur, wenn noch platz ist und der carrier nicht unterwegs
				if(s.getWeight() <= remainingCapacity && onMyWay == false) {
					double cost = 0;
					//if(shipments.isEmpty()) {
						cost += currentPos.getDistance(s.getStart());
						cost += s.getStart().getDistance(s.getDest());
						//test neue berechnung
						currentPos = s.getDest();
					/*}
					else {
						
						cost = calculateCost(s, false);	
					}*/
					
					
					reply.setPerformative(ACLMessage.CONFIRM);
					s.setCost(cost - currentCost);
					reply.setContentObject(s);
					//erwarte Antwort innerhalb von 10 Sekunden
					reply.setReplyByDate(new Date(new Date().getTime() + 10000));
				}
				else {
					//kann lieferung nicht übernehmen -> zu wenig kapazität
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				
				
				myAgent.send(reply);
	
				
			} catch (UnreadableException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

	}
	
	@SuppressWarnings("serial")
	private class DeliveryBehaviour extends OneShotBehaviour {

		@Override
		public void action() {
			//benutze wegkosten als multiplikator für sleep
			
			try {
				//sleep für jede kosteneinheit 1 sec
				Thread.sleep((long) (currentCost * 100));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//setze aktuelle Position auf endposition der route und resette shipments, kosten und kapazität
			remainingCapacity = maxCapacity;
			shipments.clear();
			currentCost = 0;
			onMyWay = false;
			
		}
		
	}
	
	//Benötigt zu viel heap space, daher wird für die protoyp entwicklung diese funktion ausgenommen und eine simplere kostenberechnung verwendet
	private double calculateCost(Shipment s, boolean accept) {
		
		
		ArrayList<Shipment> possibleShipments = new ArrayList<Shipment>();
		possibleShipments.addAll(shipments);
		possibleShipments.add(s);
		int [] indexShip = new int[shipments.size()+2];
		ArrayList<Coordinates> route = new ArrayList<Coordinates>();
		double cost = 0;
		
		Coordinates current = currentPos;
		
		int ix = 0;
		//bilde billigste tour aus startkoordinaten
		while(!possibleShipments.isEmpty()) {
			
			double minCost = Double.MAX_VALUE;
			Coordinates minC = null;
			for(Shipment sh : possibleShipments) {
				if(current.getDistance(sh.getStart()) < minCost) {
					minCost = current.getDistance(sh.getStart());
					minC = sh.getStart();
				}								
			}
			current = minC;
			indexShip[ix] = shipments.indexOf(current);
			//ix++;
			//route.add(current);
			possibleShipments.remove(current);
		}
		System.out.println("costcalc bool:"+ accept);
		
		ArrayList<Shipment> possibleDestinations = new ArrayList<Shipment>(shipments);
		possibleDestinations.add(s);
		//füge zielkoordianten an kostengünstigster position nach den startkoordinaten ein		
		double minCost = Double.MAX_VALUE;
		int pos = 0;
		Coordinates last = null;
		for(Shipment sh : possibleDestinations) {
			int index = route.indexOf(sh.getStart());
			for(int i = index; i < route.size(); i++) {
				double cst = route.get(i).getDistance(sh.getDest());
				if(cst < minCost) {
					minCost = cst;
					pos = i+1;
				}
			}
			route.add(pos, sh.getDest());
			last = sh.getDest();
		}
		if(accept)
		{
			//lastRoutePos = last;
		}
		
		cost = 0;
		current = currentPos;
		//berechne gesamtkosten
		for(Coordinates c : route) {
			cost += current.getDistance(c);
			current = c;
		}
		possibleShipments = null;
		possibleDestinations = null;
		route = null;
		
		return cost;
	}
	

}
