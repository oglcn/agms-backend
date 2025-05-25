package com.agms.backend.repository;

import com.agms.backend.model.File;
import com.agms.backend.model.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Integer> {
    List<File> findByUploader(User uploader);
    Optional<File> findByFilePath(String filePath);
} 