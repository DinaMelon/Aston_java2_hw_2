package db.crud.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            return new Configuration()
                    .configure()
                    .buildSessionFactory();

        } catch (Throwable exception) {
            System.err.println("Ошибка создания SessionFactory:");
            exception.printStackTrace();
            throw new ExceptionInInitializerError(exception);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}