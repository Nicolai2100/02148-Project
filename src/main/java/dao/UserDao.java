package dao;

import model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDao {

    int insertUser(User user);

/*
    default int insertUser(User user){
        UUID id = UUID.randomUUID();
        return insertUser(user);
    }
*/

    List<User> selectAllUsers();

    Optional<User> selectUserById(UUID id);

    int delete(UUID id);

    int update(UUID id, User user);
}
