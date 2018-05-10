package io.nuls.network.message.impl;

import io.netty.channel.socket.SocketChannel;
import io.nuls.network.connection.netty.NioChannelMap;
import io.nuls.network.entity.NetworkEventResult;
import io.nuls.network.entity.Node;
import io.nuls.network.protocol.handler.BaseNetworkMeesageHandler;
import io.nuls.network.protocol.message.BaseNetworkMessage;
import io.nuls.network.protocol.message.HandshakeMessage;
import io.nuls.network.protocol.message.NetworkMessageBody;

public class NodesMessageHandler implements BaseNetworkMeesageHandler {

    private static NodesMessageHandler instance = new NodesMessageHandler();

    private NodesMessageHandler() {

    }

    public static NodesMessageHandler getInstance() {
        return instance;
    }

    @Override
    public NetworkEventResult process(BaseNetworkMessage message, Node node) {

        HandshakeMessage handshakeMessage = (HandshakeMessage) message;

        SocketChannel socketChannel = NioChannelMap.get(node.getChannelId());

        NetworkMessageBody body = handshakeMessage.getMsgBody();


        return null;
    }
}
