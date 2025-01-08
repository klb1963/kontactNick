package com.contaktnick.contaktnick.repository;

import com.contaktnick.contaktnick.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNick(String nick);

}
