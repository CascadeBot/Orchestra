package org.cascadebot.orchestra.data;

import org.cascadebot.orchestra.data.enums.NodeType;

public class LavalinkNode {

    private final String name;
    private final String host;
    private final String password;
    private final NodeType nodeType;

    public LavalinkNode(String name, String host, String password, NodeType nodeType) {
        this.name = name;
        this.host = host;
        this.password = password;
        this.nodeType = nodeType;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

}
