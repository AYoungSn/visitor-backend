package com.ftseoul.visitor.data;

import com.ftseoul.visitor.data.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByName(String name);
    boolean existsStaffByName(String name);
}
