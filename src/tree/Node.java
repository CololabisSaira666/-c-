package tree;

import lexer.Token;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private NodeType nodeType;
    private Token leafToken;
    public int nodeNum;
    private List<Node> childNodes = new ArrayList<>();

    public Node(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public void addToken(Token thisToken) {
        this.leafToken = thisToken;
    }

    public void addNode(Node node) {
        this.childNodes.add(node);
    }

    public NodeType getType() {
        return nodeType;
    }

    public Token getToken() {
        return leafToken;
    }

    public List<Node> getchildNodes() {
        return childNodes;
    }
}
