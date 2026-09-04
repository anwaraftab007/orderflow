package com.orderflow.orderflow.repository;

import com.orderflow.orderflow.entity.SystemCheck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemCheckRepository extends JpaRepository<SystemCheck, Long> {
}
