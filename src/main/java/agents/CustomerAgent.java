package agents;

import java.io.IOException;
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

public class CustomerAgent extends Agent {
	
	private AID[] transportServiceAgents;

	protected void setup() {
		
		System.out.println("CustomerAgent: setup");
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("customer");
		sd.setName("SoS_mas_logistics");
		dfd.addServices(sd);
		
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}		
		
		//erstelle regelmäßig neue shipments
		
		addBehaviour(new TickerBehaviour(this, 10000) {
			protected void onTick() {
				int shipmentweight = (int)Math.floor(Math.random() * 100);
				int startX = (int)Math.floor(Math.random() * 100);
				int startY = (int)Math.floor(Math.random() * 100);
				int destX = startX + (int)(Math.floor(Math.random() * 10));
				int destY = startY + (int)(Math.floor(Math.random() * 10));
				Shipment ship = new Shipment(new Coordinates(startX,startY), new Coordinates(destX, destY));
				ship.setWeight(shipmentweight);
				UUID id = UUID.randomUUID();
				ship.setId(id);
				ship.setCustomerID(getAID());
				myAgent.addBehaviour(new NewShipmentBehaviour(ship));
			}
		} );
		
		addBehaviour(new GetShipmentStatusBehaviour());
		
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
	
	private class NewShipmentBehaviour extends OneShotBehaviour {
		
		Shipment s;
		
		public NewShipmentBehaviour(Shipment s) {
			System.out.println("CustomerAgent: NewShipmentBehaviour");
			this.s = s;
		}

		@Override
		public void action() {
			
			System.out.println("CustomerAgent: action");
			updateTransportServiceList();
			
			//sende anfrage an alle transport service agents
			ACLMessage cfp = new ACLMessage(ACLMessage.REQUEST);
			for (int i = 0; i < transportServiceAgents.length; ++i) {
				cfp.addReceiver(transportServiceAgents[i]);
			}
			
			try {
				cfp.setContentObject(s);
				myAgent.send(cfp);
			} catch (IOException e) {
				e.printStackTrace();
			};
			
		}
		
		public void updateTransportServiceList() {
			System.out.println("CustomerAgent: updateTransportServiceList");
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("transport-service");
			template.addServices(sd);
			
			try {
				//durchsuche Service Datenbank (dfservice) nach passenden services
				DFAgentDescription[] result = DFService.search(myAgent, template);
				transportServiceAgents = new AID[result.length];
				for (int i = 0; i < result.length; ++i) {
					transportServiceAgents[i] = result[i].getName();
				}
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}
		}

	}
	
	private class GetShipmentStatusBehaviour extends CyclicBehaviour {

		//klasse um eingehende informationen zu shipments abzuarbeiten
		@Override
		public void action() {
			
			System.out.println("CustomerAgent: GetShipmentStatusBehaviour - action");
			//empfange neue nachrichten
			ACLMessage msg = myAgent.receive();
			if (msg != null) {

				if(msg.getPerformative() == ACLMessage.CONFIRM) {
					System.out.println("Shipment confirmed");
				}
				else if(msg.getPerformative() == ACLMessage.REFUSE) {
					System.out.println("Shipment denied - no carrier available");
				}
				
			}
			else {
				block();
			}
			
		}

	}
}
