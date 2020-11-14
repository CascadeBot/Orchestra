package org.cascadebot.orchestra.data.lavalink;

import lavalink.client.io.Lavalink;
import lavalink.client.io.LavalinkLoadBalancer;
import lavalink.client.io.LavalinkSocket;
import lavalink.client.io.PenaltyProvider;
import org.cascadebot.orchestra.data.LavalinkNode;
import org.cascadebot.orchestra.data.enums.NodeType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class CascadeLoadBalancer {

    private CascadeLavalink lavalink;
    private List<PenaltyProvider> penaltyProviders = new ArrayList<>();

    public CascadeLoadBalancer(CascadeLavalink lavalink) {
        this.lavalink = lavalink;
    }

    public LavalinkSocket getBestSocketForType(long guildId, NodeType nodeType, boolean allowLower) {
        LavalinkSocket lowest = null;
        int lowestNumber = Integer.MAX_VALUE;
        for (LavalinkNode lavalinkNode : lavalink.getLavalinkNodes()) {
            LavalinkSocket socket = lavalinkNode.getLavalinkSocket();

            LavalinkLoadBalancer.Penalties penalties;

            try {
                Constructor<LavalinkLoadBalancer.Penalties> constructor = LavalinkLoadBalancer.Penalties.class.getDeclaredConstructor(LavalinkSocket.class, long.class, List.class, Lavalink.class);
                constructor.setAccessible(true);
                penalties = constructor.newInstance(socket, guildId, penaltyProviders, lavalink);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                break;
            }

            if (allowLower && lavalinkNode.getNodeType().ordinal() <= nodeType.ordinal()) {
                int multiplier = lavalinkNode.getNodeType().equals(nodeType) ? 1 : NodeType.values().length - lavalinkNode.getNodeType().ordinal();
                int total = penalties.getTotal() * multiplier;
                if (total < lowestNumber) {
                    lowest = socket;
                    lowestNumber = total;
                }
            } else if (lavalinkNode.getNodeType().equals(nodeType)) {
                int total = penalties.getTotal();
                if (total < lowestNumber) {
                    lowest = socket;
                    lowestNumber = total;
                }
            }
        }
        if (lowest == null) {
            lowest = lavalink.getNodes().get(0);
            // TODO log this
        }
        return lowest;
    }

}
