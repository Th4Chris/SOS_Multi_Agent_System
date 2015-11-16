package agents;


import java.io.IOException;
import java.util.Date;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import messageObjects.Shipment;

public class TransportServiceAgent extends Agent {
	
	private AID[] carrierAgents;
	
	protected void setup() {
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("transport-service");
		sd.setName("SoS_mas_logistics");
		dfd.addServices(sd);
		
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}	
		
		//starte kontinuierliches empfangen von requests
		addBehaviour(new ProcessShipmentRequestBehaviour());
		
		
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
	
	private class ProcessShipmentRequestBehaviour extends CyclicBehaviour {

		@Override
		public void action() {
			
			
			
			//bei neuer nachricht staret ein neues behaviour um alle carrier anzufragen
			ACLMessage msg = myAgent.receive();
			if (msg != null) {
				
				//überprüfen ob sich an der carrier list etwas geändert hat
				updateCarrierList();
				
				myAgent.addBehaviour(new CarrierRequestBehaviour(msg));
				
			}
			else {
				block();
			}
			
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
	
	private class CarrierRequestBehaviour extends Behaviour {
		
		ACLMessage req;
		
		public CarrierRequestBehaviour(ACLMessage req) {
			this.req = req;
		}

		@Override
		public void action() {
			
			
			Shipment s = null;
			try {
				s = (Shipment)req.getContentObject();
			} catch (UnreadableException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			int bestCost = Integer.MAX_VALUE;
			int currentCost = Integer.MAX_VALUE;
			ACLMessage bestCarrierMsg = null;
			
			//adressiere nachricht an alle carrier
			ACLMessage cfp = new ACLMessage(ACLMessage.REQUEST);
			for (int i = 0; i < carrierAgents.length; ++i) {
				cfp.addReceiver(carrierAgents[i]);
			}

			try {
				//leite Anfrage an alle carrier weiter(set reply within)
				cfp.setContentObject(s);
				//erwarte antwort in 10 Sekunden
				cfp.setReplyByDate(new Date(new Date().getTime() + 10000));
				myAgent.send(cfp);
				
				
				
				//schleife um alle carrier replys abzuarbeiten, TODO: abbruchbedingung???
				boolean brk = true;
				while(brk) {
					ACLMessage msg = myAgent.receive();
					if (msg != null) {
						if(msg.getPerformative() == ACLMessage.CONFIRM) {
							//ermittel kostengünstigsten carrier
							currentCost = ((Shipment)msg.getContentObject()).getCost();
							if(currentCost < bestCost) {
								bestCost = currentCost;
								bestCarrierMsg = msg;
							}	
						}
					}
					else {
						block();
					}
				}
				
				//sende bestätigung an carrier
				if(bestCarrierMsg != null) {
					ACLMessage reply = bestCarrierMsg.createReply();
					reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					reply.setContentObject(bestCarrierMsg.getContentObject());
					myAgent.send(reply);
				}
				
				//sende bestätigung an customer
				ACLMessage replyCustomer = req.createReply();
				if(bestCarrierMsg != null) {
					replyCustomer.setPerformative(ACLMessage.CONFIRM);
					s.setCarrierID(bestCarrierMsg.getSender());
					replyCustomer.setContentObject(s);
				}
				else {
					//kein carrier verfügbar
					replyCustomer.setPerformative(ACLMessage.REFUSE);
					replyCustomer.setContent("not-available");
				}
				myAgent.send(replyCustomer);
				
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnreadableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			};
			
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}

	}

}
