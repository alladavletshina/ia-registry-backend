package com.example.taskservice.repository;

import com.example.taskservice.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

}
