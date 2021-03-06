package javabot.operations;

import java.util.ArrayList;
import java.util.List;

import com.antwerkz.maven.SPI;
import javabot.IrcEvent;
import javabot.Message;
import javabot.dao.ApiDao;
import javabot.dao.ClazzDao;
import javabot.javadoc.Api;
import javabot.javadoc.Clazz;
import javabot.javadoc.Field;
import javabot.javadoc.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@SPI(BotOperation.class)
public class JavadocOperation extends BotOperation {
    @Autowired
    private ApiDao apiDao;
    @Autowired
    private ClazzDao dao;
    private static final int RESULT_LIMIT = 5;

    @Override
    @Transactional
    public List<Message> handleMessage(final IrcEvent event) {
        final String message = event.getMessage();
        final List<Message> responses = new ArrayList<Message>();
        if (message.toLowerCase().startsWith("javadoc ")) {
            final String key = message.substring("javadoc ".length()).trim();
            if (key.startsWith("-list") || "".equals(key)) {
                displayApiList(event, responses);
            } else {
                final List<String> urls = handle(key);
                if (!urls.isEmpty()) {
                    StringBuilder urlMessage = new StringBuilder(event.getSender() + ": ");
                    String destination = event.getChannel();
                    if (urls.size() > RESULT_LIMIT) {
                        responses.add(new Message(event.getChannel(), event,
                            String.format("%s, too many results found.  Please see your private messages for results",
                                event.getSender())));
                        destination = event.getSender().getNick();
                    }
                    for (int index = 0; index < urls.size(); index++) {
                        if ((urlMessage + urls.get(index)).length() > 400) {
                            responses.add(new Message(destination, event, urlMessage.toString()));
                            urlMessage = new StringBuilder();
                        }
                        urlMessage
                            .append(index == 0 ? "" : "; ")
                            .append(urls.get(index));
                    }
                    responses.add(new Message(destination, event, urlMessage.toString()));
                } else if (urls.isEmpty()) {
                    responses.add(new Message(event.getChannel(), event,
                        "I don't know of any documentation for " + key));
                }
            }
        }
        return responses;
    }

    public List<String> handle(final String key) {
        final List<String> urls = new ArrayList<String>();
        final int openIndex = key.indexOf('(');
        if (openIndex == -1) {
            parseFieldOrClassRequest(urls, key);
        } else {
            parseMethodRequest(urls, key, openIndex);
        }
        return urls;
    }

    private void parseFieldOrClassRequest(final List<String> urls, final String key) {
        final int finalIndex = key.lastIndexOf('.');
        if (finalIndex == -1) {
            findClasses(urls, key);
        } else {
            final String className = key.substring(0, finalIndex);
            final String fieldName = key.substring(finalIndex + 1);
            if (Character.isUpperCase(fieldName.charAt(0)) && !fieldName.toUpperCase().equals(fieldName)) {
                findClasses(urls, key);
            } else {
                final List<Field> list = dao.getField(className, fieldName);
                for (final Field field : list) {
                    urls.add(field.getDisplayUrl(field.toString(), dao));
                }
            }
        }
    }

    private void findClasses(final List<String> urls, final String key) {
        for (final Clazz clazz : dao.getClass(key)) {
            urls.add(clazz.getDisplayUrl(clazz.toString(), dao));
        }
    }

    private void parseMethodRequest(final List<String> urls, final String key, final int openIndex) {
        final int finalIndex = key.lastIndexOf('.', openIndex);
        final int closeIndex = key.indexOf(')');
        if (closeIndex != -1) {
            String className = key.substring(0, finalIndex);
            final String methodName = key.substring(finalIndex + 1, openIndex);
            final String signatureTypes = key.substring(openIndex + 1, closeIndex);
            for (final Method method : dao.getMethods(className, methodName, signatureTypes)) {
                urls.add(method.getDisplayUrl(method.toString(), dao));
            }
        }
    }

    private void displayApiList(final IrcEvent event, final List<Message> responses) {
        final StringBuilder builder = new StringBuilder();
        for (final Api api : apiDao.findAll()) {
            if (builder.length() != 0) {
                builder.append("; ");
            }
            builder.append(api.getName())
                .append(" ( ")
                .append(api.getBaseUrl())
                .append(" ) ");
        }
        responses.add(new Message(event.getChannel(), event, event.getSender()
            + ", I know of the following APIs: " + builder));
    }
}