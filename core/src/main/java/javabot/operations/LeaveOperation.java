package javabot.operations;

import javabot.BotEvent;
import javabot.Javabot;
import javabot.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeaveOperation extends BotOperation {
    private static final Logger log = LoggerFactory.getLogger(LeaveOperation.class);

    public LeaveOperation(final Javabot bot) {
        super(bot);
    }

    @Override
    public boolean handleMessage(final BotEvent event) {
        final String message = event.getMessage();
        final String channel = event.getChannel();
        final String sender = event.getSender();
        boolean handled = false;
        if ("leave".equals(message.toLowerCase())) {
            if (channel.equals(sender)) {
                getBot().postMessage(new Message(channel, event, "I cannot leave a private message, " + sender + "."));
            } else {
                getBot().postMessage(new Message(channel, event, "I'll be back..."));
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getBot().partChannel(channel, "I was asked to leave.");
                        try {
                            Thread.sleep(60000 * 15);
                        } catch (InterruptedException exception) {
                            log.error(exception.getMessage(), exception);
                        }
                        getBot().joinChannel(channel);
                    }
                }).start();
                handled = true;
            }
        }
        return handled;
    }
}