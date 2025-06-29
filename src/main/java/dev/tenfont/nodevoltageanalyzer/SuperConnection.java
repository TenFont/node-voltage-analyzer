package dev.tenfont.nodevoltageanalyzer;

/**
 * Similar to {@link NodeConnection} except it <i>super-connects</i> two nodes that act as a source.
 * The voltage difference between the two super-connected nodes must be known.
 * This means that the two nodes will form a single combined KCL equation,
 *  along with a "dependency" equation i.e. V_A - V_B = c.
 *
 * @param connectedNode the node this connection links to
 * @param voltageRaise voltage difference between the two nodes
 */
public record SuperConnection(Node connectedNode, double voltageRaise) {
}
