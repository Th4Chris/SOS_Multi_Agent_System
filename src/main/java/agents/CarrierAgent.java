package agents;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

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
import messageObjects.Shipment;

public class CarrierAgent extends Agent {
	
	private AID[] carrierAgents;
	private ConcurrentLinkedQueue<Shipment> shipments;
	
	//Carrier infos TODO: überlegen was wirklich notwendig ist für constrainterfüllung + kostenberechnung
	//TODO: shipment reihenfolge und berechnung von z.b. verfügbarem platz/ladungsgewicht nach jeder delivery
	private boolean onMyWay;
	private Shipment nextDelivery;
	private int maxLoad;
	
	protected void setup() {
	
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
		shipments = new ConcurrentLinkedQueue<Shipment>();
		
		addBehaviour(new ReceiveShipmentMessageBehaviour());
		addBehaviour(new OptimizeShipmentMessageBehaviour());
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
	
	private class ReceiveShipmentMessageBehaviour extends CyclicBehaviour {

		@Override
		public void action() {
			
			//empfange neue nachrichten
			ACLMessage msg = myAgent.receive();
			if (msg != null) {

				if(msg.getPerformative() == ACLMessage.REQUEST) {
					//verarbeite neuen request
					myAgent.addBehaviour(new ComputeShipmentCostBehaviour(msg));
				}
				else if(msg.getPerformative() == ACLMessage.CONFIRM) {
					//füge shipment zur liste hinzu
					try {
						shipments.add((Shipment)msg.getContentObject());
					} catch (UnreadableException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
			else {
				block();
			}
			
		}

	}
	
	private class ComputeShipmentCostBehaviour extends OneShotBehaviour {
		
		ACLMessage req;
		
		public ComputeShipmentCostBehaviour(ACLMessage req) {
			this.req = req;
		}

		@Override
		public void action() {
			//berechner kosten und sende antwort
			try {
				Shipment s = (Shipment)req.getContentObject();
				boolean deliveryPossible = false;
				
				//TODO: constraints überprüfen + kosten berechnen
				//if constraints erfüllbar -> deliveryPossible = true;
				//s.setCost(costs);
				//s.setPickup(pickup);
				//s.setDelivery(delivery);
				
				//sende antwort mit bestätigung
				ACLMessage reply = req.createReply();
				
				if(deliveryPossible) {
					
					reply.setPerformative(ACLMessage.CONFIRM);
					reply.setContentObject(s);
					//erwarte Antwort innerhalb von 10 Sekunden
					reply.setReplyByDate(new Date(new Date().getTime() + 10000));
				}
				else {
					//kann lieferung nicht übernehmen -> bereits voll, zu weit weg etc
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				
				myAgent.send(reply);
				
			} catch (UnreadableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

	}
	
	public class OptimizeShipmentMessageBehaviour extends Behaviour {

		@Override
		public void action() {
			
			updateCarrierList();
			
			//regelmäßige anfrage an andere carrier über routenaustausch
			//TODO: überlegen wann und wie oft angefragt wird bzw unter welchen bedingungen
			//TODO: informiere customer bei update ACLMessage.INFORM
			
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
		
		public void updateCarrierList() {
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("carrier");
			template.addServices(sd);
			try {
			DFAgentDescription[] result = DFService.search(myAgent, template);
			carrierAgents = new AID[result.length];
			for (int i = 0; i < result.length; ++i) {
				carrierAgents[i] = result[i].getName();
			}
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}
		}

	}

}
