package com.reggie.api_gateway.repository;

import com.reggie.api_gateway.dto.VectorRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VectorRepository {

    private final JdbcTemplate jdbc;

    public VectorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(int id, String label, float[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i != vector.length - 1) {
                sb.append(",");
            }
        }
        String vectorStr = sb.toString();
        jdbc.update(
                "INSERT INTO vectors (id, label, vector) VALUES (?, ?, ?)",
                id, label, vectorStr
        );
    }

    public List<VectorRecord> findAll() {
        return jdbc.query("SELECT id, label, vector FROM vectors", (rs, rowNum) -> {
            int id = rs.getInt("id");
            String label = rs.getString("label");
            String vectorStr = rs.getString("vector");
            String[] vectorArr = vectorStr.split(",");
            float[] vectors = new float[vectorArr.length];
            for (int i = 0; i < vectorArr.length; i++) {
                vectors[i] = Float.parseFloat(vectorArr[i]);
            }
            return new VectorRecord(id, label, vectors);
        });
    }


}
