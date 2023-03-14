package app.nzyme.core.distributed.messaging;

public interface MessageBus {

    void initialize();

    void send(Message message);
    void onMessageReceived(MessageType type, MessageHandler messageHandler);

}