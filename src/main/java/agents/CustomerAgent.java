package agents;

import java.io.IOException;

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
		//TODO: random werte für neue shipments generieren
		addBehaviour(new TickerBehaviour(this, 6000) {
			protected void onTick() {
				Shipment ship = new Shipment(1,2,3,4);
				ship.setWeight(100);
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
				// TODO Auto-generated catch block
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
			
			System.out.println("CustomerAgent: GetSHipmentStatusBehaviour - action");
			//empfange neue nachrichten
			ACLMessage msg = myAgent.receive();
			if (msg != null) {

				if(msg.getPerformative() == ACLMessage.CONFIRM) {
					System.out.println("Shipment confirmed");
				}
				else if(msg.getPerformative() == ACLMessage.REFUSE) {
					System.out.println("Shipment denied - no carrier available");
				}
				else if(msg.getPerformative() == ACLMessage.INFORM) {
					System.out.println("Shipment update");
				}
				
			}
			else {
				block();
			}
			
		}

	}
}
