package com.example.clarityspend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List; // <-- NEW IMPORT

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // --- NEW METHOD ---
    // Spring Data JPA automatically creates this query for us:
    // "Find all transactions where the user's ID matches the given userId"
    List<Transaction> findByUserId(Long userId);
}