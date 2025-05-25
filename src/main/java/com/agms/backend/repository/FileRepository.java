package com.agms.backend.repository;

import com.agms.backend.model.File;
import com.agms.backend.model.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Integer> {
    List<File> findByUploader(User uploader);
} 