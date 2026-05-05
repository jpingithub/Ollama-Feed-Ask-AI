package com.rb.feed_ask_ai.repository;

import com.rb.feed_ask_ai.entity.RagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RagRepository extends JpaRepository<RagEntity, Integer> {
}
