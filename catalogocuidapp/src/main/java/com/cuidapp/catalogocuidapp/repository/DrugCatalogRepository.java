package com.cuidapp.catalogocuidapp.repository;

import com.cuidapp.catalogocuidapp.model.DrugCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DrugCatalogRepository extends JpaRepository<DrugCatalog, Long> {

    Optional<DrugCatalog> findByBarcode(String barcode);

    @Query("SELECT d FROM DrugCatalog d WHERE " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(d.genericName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(d.brand) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<DrugCatalog> searchByName(@Param("q") String query);

    @Query("SELECT d FROM DrugCatalog d WHERE LOWER(d.name) LIKE LOWER(CONCAT(:prefix, '%')) ORDER BY d.name")
    List<DrugCatalog> findByNameStartingWith(@Param("prefix") String prefix);
}
