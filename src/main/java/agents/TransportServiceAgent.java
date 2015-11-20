package agents;


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

import java.io.IOException;
import java.util.Date;

import FIPA.DateTime;
import messageObjects.Shipment;

public class TransportServiceAgent extends Agent {
	
	private AID[] carrierAgents;
		
	protected void setup() {
		System.out.println("TSA: setup transport service agent");
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
	
	@SuppressWarnings("serial")
	private class ProcessShipmentRequestBehaviour extends CyclicBehaviour {

		@Override
		public void action() {
			
			
			System.out.println("TSA: action processShipmentRequestBehaviour");
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
			System.out.println("TSA: updateCarrierList");
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
	
	@SuppressWarnings("serial")
	private class CarrierRequestBehaviour extends OneShotBehaviour {
		
		ACLMessage req;
		
		public CarrierRequestBehaviour(ACLMessage req) {
			this.req = req;
		}

		@Override
		public void action() {
			System.out.println("TSA: action CarrierRequestBehaviour");
			
			Shipment s = null;
			try {
				s = (Shipment)req.getContentObject();
				System.out.println(s.toString());
			} catch (UnreadableException e1) {
				e1.printStackTrace();
			}
			
			double bestCost = Double.MAX_VALUE;
			double currentCost = Double.MAX_VALUE;
			ACLMessage bestCarrierMsg = null;
			
			int carrierCount = carrierAgents.length;
			//adressiere nachricht an alle carrier
			ACLMessage cfp = new ACLMessage(ACLMessage.REQUEST);
			for (int i = 0; i < carrierCount; ++i) {
				cfp.addReceiver(carrierAgents[i]);
			}

			
			try {
				//leite Anfrage an alle carrier weiter(set reply within)
				cfp.setContentObject(s);
				//erwarte antwort in 10 Sekunden
				cfp.setReplyByDate(new Date(new Date().getTime() + 10000));
				myAgent.send(cfp);
				

				long startTime = new Date().getTime();
				long endTime = 0;
				int count = 0;
				//schleife um alle carrier replys abzuarbeiten, abbruch wenn alle geantwortet haben oder nach 40 sec
				while((startTime - endTime) > 40000 && count < carrierCount) {
				//boolean test = true;
				//while(test) {
					ACLMessage msg = myAgent.receive();
					if (msg != null) {
						if(((Shipment)msg.getContentObject()).getId().equals(s.getId())) {
							count++;
							System.out.println("TSA: Antwort" + msg.getPerformative());
							if(msg.getPerformative() == ACLMessage.CONFIRM) {
								//ermittel kostengünstigsten carrier
								currentCost = ((Shipment)msg.getContentObject()).getCost();
								if(currentCost < bestCost) {
									bestCost = currentCost;
									bestCarrierMsg = msg;
								}	
							}
						}
						endTime = new Date().getTime();
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
					System.out.println("TSA: Best Carrier cost " + ((Shipment)reply.getContentObject()).getCost());
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
					System.out.println("TSA: Kein Carrier Verf�gbar");
				}
				myAgent.send(replyCustomer);
					
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (UnreadableException e) {
				e.printStackTrace();
			};
			
		}

	}

}
