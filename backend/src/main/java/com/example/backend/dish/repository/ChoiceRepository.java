package com.example.backend.dish.repository;

import com.example.backend.dish.entity.Choice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChoiceRepository extends JpaRepository<Choice, Integer> {

}
