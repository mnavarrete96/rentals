package com.rentals.repository;

import com.rentals.domain.OneTimeCharge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OneTimeChargeRepository extends JpaRepository<OneTimeCharge, Long> {
}
