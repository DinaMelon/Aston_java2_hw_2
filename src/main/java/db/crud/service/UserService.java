package db.crud.service;

import db.crud.dao.UserDaoInterface;
import db.crud.model.User;

import java.util.List;

public class UserService {

    private final UserDaoInterface userDao;

    public UserService(UserDaoInterface userDao) {
        this.userDao = userDao;
    }

    public void createUser(String name, String email, int age) {

        User user = new User(name, email, age);

        userDao.create(user);
    }

    public User getUser(long id) {
        return userDao.read(id);
    }

    public List<User> getAllUsers() {
        return userDao.readAll();
    }

    public void updateUser(long id, String name, String email, int age) {

        User user = userDao.read(id);

        if (user == null) {
            return;
        }

        user.setName(name);
        user.setEmail(email);
        user.setAge(age);

        userDao.update(user);
    }

    public void deleteUser(long id) {
        userDao.delete(id);
    }
}