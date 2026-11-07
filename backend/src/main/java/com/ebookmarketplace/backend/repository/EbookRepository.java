package com.ebookmarketplace.backend.repository;

import com.ebookmarketplace.backend.entity.Ebook;
import com.ebookmarketplace.backend.entity.EbookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EbookRepository extends JpaRepository<Ebook, Long> {

    Optional<Ebook> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Ebook> findAllByStatus(
            EbookStatus status,
            Pageable pageable
    );

    Page<Ebook> findAllByStatusAndCategoryId(
            EbookStatus status,
            Long categoryId,
            Pageable pageable
    );

    Page<Ebook> findAllByTitleContainingIgnoreCaseAndStatus(
            String title,
            EbookStatus status,
            Pageable pageable
    );

    Page<Ebook> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Ebook> findAllByTitleContainingIgnoreCaseAndStatusAndCategoryId(
                    String title,
                    EbookStatus status,
                    Long categoryId,
                    Pageable pageable);
                    
}