package com.jpd.web.repository;

import com.jpd.web.model.CreatorMediaCapacity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorMediaCapacityRepository extends JpaRepository <CreatorMediaCapacity,Long>{
    CreatorMediaCapacity findByCreatorId(String creatorId);
}
