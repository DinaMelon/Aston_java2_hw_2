package db.crud.dao;

import db.crud.model.User;
import db.crud.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;

public class UserDaoImpl implements UserDaoInterface {

    @Override
    public void create(User user) {

        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            transaction = session.beginTransaction();

            session.persist(user);

            transaction.commit();

            System.out.println("Пользователь успешно создан.");

        } catch (ConstraintViolationException e) {

            if (transaction != null) {
                transaction.rollback();
            }

            System.out.println("Ошибка: пользователь с таким email уже существует.");

        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }

            System.out.println("Ошибка при создании пользователя:");
            e.printStackTrace();
        }
    }

    @Override
    public void update(User user) {

        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            transaction = session.beginTransaction();

            session.merge(user);

            transaction.commit();

            System.out.println("Пользователь успешно обновлен.");

        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }

            System.out.println("Ошибка при обновлении пользователя:");
            e.printStackTrace();
        }
    }

    @Override
    public void delete(long userId) {

        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            transaction = session.beginTransaction();

            User user = session.get(User.class, userId);

            if (user == null) {
                System.out.println("Пользователь с ID " + userId + " не найден.");
                transaction.rollback();
                return;
            }

            session.remove(user);

            transaction.commit();

            System.out.println("Пользователь успешно удален.");

        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }

            System.out.println("Ошибка при удалении пользователя:");
            e.printStackTrace();
        }
    }

    @Override
    public List<User> readAll() {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            return session
                    .createQuery("FROM User", User.class)
                    .getResultList();

        } catch (Exception e) {

            System.out.println("Ошибка при получении пользователей:");
            e.printStackTrace();

            return List.of();
        }
    }

    @Override
    public User read(long userId) {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            User user = session.get(User.class, userId);

            if (user == null) {
                System.out.println("Пользователь с ID " + userId + " не найден.");
            }

            return user;

        } catch (Exception e) {

            System.out.println("Ошибка при получении пользователя:");
            e.printStackTrace();

            return null;
        }
    }
}