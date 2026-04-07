package org.admissio.scraper.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Service
@AllArgsConstructor
public class ExportDataService {

    private final JdbcTemplate jdbcTemplate;
    private static final String OUTPUT_DIR = "output";

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR));
        } catch (IOException e) {
            log.error("Не вдалося створити директорію для експорту: {}", e.getMessage());
        }
    }

    public void exportStudentsToCSV() {
        performCopyExport("students", "students.csv");
    }

    public void exportApplicationToCSV() {
        performCopyExport("applications", "applications.csv");
    }

    private void performCopyExport(String tableName, String fileName) {
        Path filePath = Paths.get(OUTPUT_DIR, fileName);
        String columnsQuery = "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = ? AND column_name != 'id' " +
                "ORDER BY ordinal_position";

        try {
            java.util.List<String> columns = jdbcTemplate.queryForList(columnsQuery, String.class, tableName);
            String columnNames = String.join(", ", columns);

            String sql = String.format("COPY (SELECT %s FROM %s) TO STDOUT WITH (FORMAT CSV, HEADER false)",
                    columnNames, tableName);

            jdbcTemplate.execute((Connection conn) -> {
                PGConnection pgConnection = conn.unwrap(PGConnection.class);
                CopyManager copyManager = new CopyManager((BaseConnection) pgConnection);

                try (FileOutputStream out = new FileOutputStream(filePath.toFile())) {
                    copyManager.copyOut(sql, out);
                    log.info("Дані з таблиці '{}' успішно експортовано у {}", tableName, filePath);
                } catch (IOException | SQLException e) {
                    throw new RuntimeException("Помилка запису у файл або SQL: " + e.getMessage(), e);
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Помилка під час експорту таблиці {}: {}", tableName, e.getMessage());
        }
    }
}