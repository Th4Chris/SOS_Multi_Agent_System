package agents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
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
	//TODO: shipment reihenfolge und berechnung von z.b. verfügbarem platz/ladungsgewicht nach jeder delivery
	private boolean onMyWay;
	private double currentCost;
	private int remainingCapacity;
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
		currentPos = new Coordinates((int)Math.floor(Math.random() * 100), (int)Math.floor(Math.random() * 100));
		onMyWay = false;
		currentCost = 0;
		addBehaviour(new ReceiveShipmentMessageBehaviour());
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
				else if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
					//füge shipment zur liste hinzu
					try {
						shipments.add((Shipment)msg.getContentObject());
						currentCost = calculateCost((Shipment) msg.getContentObject());
						remainingCapacity -= ((Shipment) msg.getContentObject()).getWeight();
						//TODO: move behaviour um shipments abzuarbeiten
						//TODO: wann fährt er los? wenn voll? nach vergangener zeit,auch wenn nicht voll?
					} catch (UnreadableException e) {
						e.printStackTrace();
					}
				}
				
			}
			else {
				block();
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
					if(shipments.isEmpty()) {
						cost += currentPos.getDistance(s.getStart());
						cost += s.getStart().getDistance(s.getDest());
					}
					else {
						
						cost = calculateCost(s);
						
						
					}
					
					
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
	
	private class DeliveryBehaviour extends OneShotBehaviour {

		@Override
		public void action() {
			// TODO: let's move
			//benutze wegkosten als multiplikator für timeout in einer schleife, bei der die route abgearbeitet wird
			//am ende onMyWay = false;
			
		}
		
	}
	
	
	private double calculateCost(Shipment s) {
		
		//JEDS MAL KOMPLETTE ROUTE NEU KALKULIEREN	
		
		//shipments durchlaufen -> nur start betrachten, wenn start ausgewählt, entfernt shipment und 
		//füge es in eine neue destination liste hinzu um anzuzeigen, dass die destination dieses shipments 
		//ausgewählt werden kann
		//alle ausgewählten koordinaten kommen in eine extra liste, der index bestimmt die reihenfolge
		//da jedes mal die komplette strecke neu berechnet wird, für die kosten die differenz zwischen alt und neu ausgeben
		//FÜGE DEST AN BESTMÖGLICHER KOORDINATE NACH (größererIndex) START EIN
		//neue idee: alle startknoten nach besten kosten einfügen
		//danach die zielknoten an einem bestmöglichen, höheren index als der startpunkt, einfügen
		
		ArrayList<Shipment> possibleShipments = new ArrayList<Shipment>();
		possibleShipments.addAll(shipments);
		possibleShipments.add(s);
		ArrayList<Shipment> possibleDestinations = new ArrayList<Shipment>(possibleShipments);
		ArrayList<Coordinates> route = new ArrayList<Coordinates>();
		double cost = 0;
		
		Coordinates current = currentPos;
		
		
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
			route.add(current);
			possibleShipments.remove(current);
		}
		
		//füge zielkoordianten an kostengünstigster position nach den startkoordinaten ein		
		double minCost = Double.MAX_VALUE;
		int pos = 0;
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
		}
		
		cost = 0;
		current = currentPos;
		//berechne gesamtkosten
		for(Coordinates c : route) {
			cost += current.getDistance(c);
			current = c;
		}
		
		return cost;
	}
	

}
