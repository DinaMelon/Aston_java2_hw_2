package db.crud.dao;

import db.crud.model.User;

import java.util.List;

public interface UserDaoInterface {
    void create(User user);
    void update(User user);
    void delete(long userId);
    List<User> readAll();
    User read(long userId);
}
