# Node Voltage Analyzer

Circuit analyzer built for the NotEnoughCopper minecraft project.
Uses nodal analysis to form KCL equations and calculate the voltage at each point
in the circuit, given the resistance between each node and a ground node with
a known voltage.

## Example Usage

---

```java
// Construct graph of circuit
Node batteryPositive = new Node(),
     batteryNegative = new Node(),
     bulbPositive    = new Node(),
     bulbNegative    = new Node();

batteryNegative.addSuperConnection(batteryPositive, 5);
bulbNegative.addConnection(bulbPositive, 1);
batteryNegative.addConnection(bulbNegative, 0.001);
batteryPositive.addConnection(bulbPositive, 0.001);

// Calculate voltages
NodalAnalysis analysis = new NodalAnalysis(batteryNegative);
analysis.analyzeVoltages();

System.out.print("Potential difference through bulb = ");
System.out.println(bulbPositive.getVoltage() - bulbNegative.getVoltage());
```
### Output
```
Potential difference through bulb = 4.99001996007984
```