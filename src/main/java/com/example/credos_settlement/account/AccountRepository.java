package com.example.credos_settlement.account;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
          SELECT a
          FROM Account a
          WHERE a.id IN :ids
          ORDER BY a.id
      """)
  List<Account> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);
}
