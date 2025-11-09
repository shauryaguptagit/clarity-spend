package com.example.clarityspend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // This is a custom method Spring Data JPA will create for us
    // It's how we'll find users when they try to log in
    Optional<User> findByUsername(String username);
}