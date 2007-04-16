package javabot.dao;

import javabot.dao.model.factoids;
import javabot.dao.model.logs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Date;
import java.util.Properties;

// User: joed
// Date: Apr 11, 2007
// Time: 2:41:22 PM


public class LogDaoHibernate extends AbstractDaoHibernate<factoids> implements LogDao {

    private Properties _properties;

    private HtmlRoutines html = new HtmlRoutines();

    private static final Log log = LogFactory.getLog(LogDaoHibernate.class);

    public LogDaoHibernate() {
        super(logs.class);
    }

    public void logMessage(String nick, String channel, String message) {

        logs logMessage = new logs();

        logMessage.setNick(nick);
        logMessage.setChannel(channel);
        logMessage.setMessage(message);
        logMessage.setDate(new Date());

        Session session = getSession();
        Transaction transaction = session.beginTransaction();
        session.save(logMessage);
        transaction.commit();

    }


    public logs getMessage(String nick, String channel) {
        String query = "from logs s where s.nick = :nick" +
                " AND s.channel = :channel";

        logs m_user = (logs) getSession().createQuery(query)
                .setString("nick", nick)
                .setString("channel", channel)
                .setMaxResults(1)
                .uniqueResult();

        if (m_user == null) {

            logs notFound = new logs();
            return notFound;
        }

        return m_user;

    }

}