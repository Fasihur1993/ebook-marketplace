
package com.ebookmarketplace.backend.repository;

import com.ebookmarketplace.backend.entity.EbookPreviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EbookPreviewImageRepository
        extends JpaRepository<EbookPreviewImage, Long> {

    List<EbookPreviewImage> findAllByEbookIdOrderByPageNumberAsc(
            Long ebookId);

    void deleteAllByEbookId(Long ebookId);
}
