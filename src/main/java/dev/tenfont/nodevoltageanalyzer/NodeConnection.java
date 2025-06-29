package dev.tenfont.nodevoltageanalyzer;

/**
 * A simple connection between two nodes in the circuit.
 *
 * @param connectedNode the node this connection links to
 * @param resistance the connection's resistance in ohms
 */
public record NodeConnection(Node connectedNode, double resistance) {
}
