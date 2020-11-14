package org.cascadebot.orchestra.data;

import lavalink.client.io.LavalinkSocket;
import org.cascadebot.orchestra.data.enums.NodeType;

import java.net.URI;

public class LavalinkNode {

    private final String name;
    private final URI host;
    private final String password;
    private final NodeType nodeType;
    private LavalinkSocket lavalinkSocket;

    public LavalinkNode(String name, URI host, String password, NodeType nodeType) {
        this.name = name;
        this.host = host;
        this.password = password;
        this.nodeType = nodeType;
    }

    public String getName() {
        return name;
    }

    public URI getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setLavalinkSocket(LavalinkSocket lavalinkSocket) {
        this.lavalinkSocket = lavalinkSocket;
    }

    public LavalinkSocket getLavalinkSocket() {
        return lavalinkSocket;
    }
}
