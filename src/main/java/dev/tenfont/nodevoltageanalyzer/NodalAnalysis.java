package dev.tenfont.nodevoltageanalyzer;

import org.apache.commons.math3.linear.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class NodalAnalysis {

    private final Node groundNode;
    private final List<Node> unknownVoltageNodes = new ArrayList<>();

    public NodalAnalysis(Node groundNode) {
        this.groundNode = groundNode;

        groundNode.setVoltage(0);

        traverseNodes();
    }

    private void traverseNodes() {
        // Use simple DFS algorithm to traverse all nodes in the circuit
        // and find unknown voltage nodes
        HashSet<Node> visited = new HashSet<>();
        visited.add(groundNode);

        Deque<Node> deque = new ArrayDeque<>();
        deque.add(groundNode);
        while (!deque.isEmpty()) {
            Node current = deque.remove();
            if (!current.hasKnownVoltage()) {
                unknownVoltageNodes.add(current);
            }
            if (current.getSuperConnection() != null) {
                Node neighbour = current.getSuperConnection().connectedNode();
                if (!visited.add(neighbour)) continue;
                deque.add(neighbour);
            }
            for (NodeConnection connection : current.getDirectConnections()) {
                Node neighbour = connection.connectedNode();
                if (!visited.add(neighbour)) continue;
                deque.add(neighbour);
            }
        }
    }

    public void analyzeVoltages() {
        // Keep track of the index of the coefficient of each node voltage variable
        // in the coefficient matrix
        Map<Node, Integer> nodeIndexMap = new HashMap<>(unknownVoltageNodes.size());
        AtomicInteger nextNodeIndex = new AtomicInteger(0);

        double[][] coefficientMatrix = new double[unknownVoltageNodes.size()][unknownVoltageNodes.size()];
        double[] solutionVector = new double[unknownVoltageNodes.size()];

        // Used to skip connected supernodes of already visited nodes
        HashSet<Node> visitedNodes = new HashSet<>();
        for (Node node : unknownVoltageNodes) {
            // Get the index of the coefficient of the current node in the matrix
            //  or the next free slot if it does not exist
            int nodeIndex = nodeIndexMap.computeIfAbsent(node, n -> nextNodeIndex.getAndIncrement());

            // If the node has a super-connection, we'll first form the dependency equation
            // i.e. V_A - V_B = c
            // Then we'll form a KCL equation linking the connection of both the current and the super-connected node
            //
            // The number of equations must equal the number of unknown voltages:
            // A super-connected node adds 2 equations instead of 1, but this invariant still holds because the
            //  alternate super-connected node is then skipped, which ultimately reduces the equation count by 1
            if (node.getSuperConnection() != null && !visitedNodes.contains(node)) {
                var connection = node.getSuperConnection();
                Node connectedNode = connection.connectedNode();
                visitedNodes.add(connectedNode);
                double voltageRaise = connection.voltageRaise();

                // Since each unknown node corresponds to a unique unknown variable,
                //  and the number of unknown variables matches the number of equations,
                //  then the index of the node can simply be used as the index for the equation as well
                //  - coefficientMatrix[nodeIndex][...]
                //  - solutionVector[nodeIndex]

                // Equation: 1 * currentNode
                coefficientMatrix[nodeIndex][nodeIndex] = 1;

                if (connectedNode.hasKnownVoltage()) {
                    // Equation: 1 * currentNodeVoltage = connectedNodeVoltage - voltageRaise
                    solutionVector[nodeIndex] = connectedNode.getVoltage() - voltageRaise;
                    continue;
                }

                int connectionIndex = nodeIndexMap.computeIfAbsent(connectedNode, n -> nextNodeIndex.getAndIncrement());
                // Equation: 1 * currentNode - 1 * connectedNode
                coefficientMatrix[nodeIndex][connectionIndex] = -1;
                // Equation: 1 * currentNode - 1 * connectedNode = -voltageRaise
                solutionVector[nodeIndex] = -voltageRaise;
            }

            // A little trick to make sure that super-connected nodes have 2 equations in separate rows:
            // The first node that is iterated in a super-connection will form the "dependency" equation
            // in the row denoted by its own index. It will also mark the alternate connected node as visited.
            // The alternate node is then skipped in the dependency equation part of the code above.
            // Instead, the alternate node's index is used to form the second, combined KCL equation.
            //
            // In other words, the first iterated super-connected node uses its equation "slot"
            // to form the dependency equation, i.e. V_A - V_B = c.
            // In the part of the code below where the KCL equations are formed, we check if the currently
            // iterated node is a super-connected node, and has NOT been visited.
            // This is essentially checking that it is the FIRST node to be iterated in a super-connection.
            // If it is, then we'll add the coefficients to the alternate super-connected node's KCL equation instead.
            // Effectively combining the KCL equation of both nodes and storing it in the SECOND node's equation slot.
            // (Remember: the first node's equation slot is used for the dependency equation)
            // This is easily done by setting the equation index to the alternate super-connected
            // node's index.
            int equationIndex = nodeIndex;
            if (node.getSuperConnection() != null && !visitedNodes.contains(node)) {
                equationIndex = nodeIndexMap.get(node.getSuperConnection().connectedNode());
            }

            for (NodeConnection connection : node.getDirectConnections()) {
                Node connectedNode = connection.connectedNode();

                double resistance = connection.resistance();
                double coefficient = 1 / resistance;

                coefficientMatrix[equationIndex][nodeIndex] += coefficient;

                if (connectedNode.hasKnownVoltage()) {
                    solutionVector[equationIndex] += coefficient * connectedNode.getVoltage();
                } else {
                    int connectionIndex = nodeIndexMap.computeIfAbsent(connectedNode, n -> nextNodeIndex.getAndIncrement());
                    coefficientMatrix[equationIndex][connectionIndex] -= coefficient;
                }
            }
        }

        // Use the Apache Commons Math library to solve for the unknown voltages.
        RealMatrix coefficients = new Array2DRowRealMatrix(coefficientMatrix, false);
        DecompositionSolver solver = new LUDecomposition(coefficients).getSolver();
        RealVector constants = new ArrayRealVector(solutionVector);
        RealVector solution = solver.solve(constants);

        // Assign voltages to each node using its index in the solution vector.
        for (var entry : nodeIndexMap.entrySet()) {
            entry.getKey().setVoltage(solution.getEntry(entry.getValue()));
        }
    }
}
