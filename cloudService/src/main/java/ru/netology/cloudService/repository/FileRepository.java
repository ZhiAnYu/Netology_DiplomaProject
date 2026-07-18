package ru.netology.cloudService.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloudService.entity.FileInfo;
import ru.netology.cloudService.entity.User;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileInfo, Long> {
    List<FileInfo> findByUser(User user, Pageable pageable);

    Optional<FileInfo> findByUserAndFilename(User user, String filename);

    boolean existsByUserAndFilename(User user, String filename);
}
