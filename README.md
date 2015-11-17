# SOS_Multi_Agent_System


Öffne 4 Shells welche jeweils folgende Befehle ausführen

mvn compile exec:java -Dexec.mainClass=jade.Boot -Dexec.args='-gui'

mvn compile exec:java -Dexec.mainClass=jade.Boot -Dexec.args="-container -host 192.168.0.13 -agents tsa1:agents.TransportServiceAgent

mvn compile exec:java -Dexec.mainClass=jade.Boot -Dexec.args="-container -host 192.168.0.13 -agents carrier1:agents.CarrierAgent

mvn compile exec:java -Dexec.mainClass=jade.Boot -Dexec.args="-container -host 192.168.0.13 -agents customer1:agents.CustomerAgent