package dev.tenfont.nodevoltageanalyzer;

import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * A node can represent any point in the circuit where the voltage may be different.
 * This can be the terminals of the battery, the terminals of a component, or a junction in the circuit.
 */
public class Node {

    private @Nullable Double voltage = null;

    private final Set<NodeConnection> connections = new HashSet<>();
    private @Nullable SuperConnection superConnection = null;

    public void addConnection(NodeConnection connection) {
        connections.add(connection);
    }

    public void addConnection(Node node, double resistance) {
        connections.add(new NodeConnection(node, resistance));
        node.addConnection(new NodeConnection(this, resistance));
    }

    public void addSuperConnection(SuperConnection connection) {
        superConnection = connection;
    }

    public void addSuperConnection(Node node, double voltageRaise) {
        superConnection = new SuperConnection(node, voltageRaise);
        node.addSuperConnection(new SuperConnection(this, -voltageRaise));
    }

    public Set<NodeConnection> getConnections() {
        return new HashSet<>(connections);
    }

    public @Nullable SuperConnection getSuperConnection() {
        return superConnection;
    }

    public void setVoltage(double v) {
        voltage = v;
    }

    public boolean hasKnownVoltage() {
        return voltage != null;
    }

    public double getVoltage() {
        if (voltage == null) throw new IllegalStateException("Node voltage is unknown");

        return voltage;
    }
}
