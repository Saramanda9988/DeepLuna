package com.luna.deepluna.repository;

import com.luna.deepluna.entity.Model;
import com.luna.deepluna.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelRepository extends JpaRepository<Model, String> {

}
