package com.example.PlagiarismChecker.Service;

import com.example.PlagiarismChecker.model.CodeFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.ConnectionCallback;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * High-performance JDBC batch insert service 10x faster than JPA saveAll for
 * bulk operations
 */
@Service
public class JdbcBatchInsertService {

	private static final Logger logger = LoggerFactory.getLogger(JdbcBatchInsertService.class);
	private static final int BATCH_SIZE = 100;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Bulk insert using raw JDBC for maximum performance Target: 2400 inserts in
	 * ~5-10 seconds
	 */
//	@Transactional
//	public int[][] batchInsertCodeFiles(List<CodeFile> codeFiles) {
//		String sql = "INSERT INTO code_files "
//				+ "(id, file_name, content, language, created_at, content_hash, trigrams_generated) "
//				+ "VALUES (nextval('code_file_sequence'), ?, ?, ?, ?, ?, false) "
//				+ "ON CONFLICT (content_hash) DO NOTHING";
//
//		long startTime = System.currentTimeMillis();
//
//		int[][] updateCounts = jdbcTemplate.batchUpdate(sql, codeFiles, BATCH_SIZE,
//				(PreparedStatement ps, CodeFile codeFile) -> {
//					ps.setString(1, codeFile.getFileName());
//					ps.setString(2, codeFile.getContent());
//					ps.setString(3, codeFile.getLanguage());
//					ps.setTimestamp(4, Timestamp.valueOf(codeFile.getCreatedAt()));
//					ps.setString(5, codeFile.getContentHash());
//				});
//
//		long elapsed = System.currentTimeMillis() - startTime;
//		logger.info("Batch inserted {} files in {}ms (avg: {}ms per file)", codeFiles.size(), elapsed,
//				elapsed / codeFiles.size());
//
//		return updateCounts;
//	}
//	public Set<String> batchInsertCodeFiles(List<CodeFile> codeFiles) {
//
//	    String sql = """
//	        INSERT INTO code_files
//	        (id, file_name, content, language, created_at, content_hash, trigrams_generated)
//	        VALUES (nextval('code_file_sequence'), ?, ?, ?, ?, ?, false)
//	        ON CONFLICT (content_hash) DO NOTHING
//	        RETURNING content_hash
//	        """;
//
//	    return jdbcTemplate.execute(
//	        (org.springframework.jdbc.core.ConnectionCallback<Set<String>>) con -> {
//
//	            Set<String> insertedHashes = new HashSet<>();
//
//	            try (PreparedStatement ps = con.prepareStatement(sql)) {
//	                for (CodeFile cf : codeFiles) {
//	                    ps.setString(1, cf.getFileName());
//	                    ps.setString(2, cf.getContent());
//	                    ps.setString(3, cf.getLanguage());
//	                    ps.setTimestamp(4, Timestamp.valueOf(cf.getCreatedAt()));
//	                    ps.setString(5, cf.getContentHash());
//	                    ps.addBatch();
//	                }
//
//	                ps.execute();
//
//	                try (ResultSet rs = ps.getResultSet()) {
//	                    while (rs.next()) {
//	                        insertedHashes.add(rs.getString(1));
//	                    }
//	                }
//	            }
//
//	            return insertedHashes;
//	        }
//	    );
//	}
	
	@Transactional
	public int[][] batchInsertIgnoreDuplicates(List<CodeFile> codeFiles) {

	    String sql = """
	        INSERT INTO code_files
	        (id, file_name, content, language, created_at, content_hash, trigrams_generated)
	        VALUES (nextval('code_file_sequence'), ?, ?, ?, ?, ?, false)
	        ON CONFLICT (content_hash) DO NOTHING
	        """;

	    long start = System.currentTimeMillis();

	    int[][] result = jdbcTemplate.batchUpdate(
	        sql,
	        codeFiles,
	        BATCH_SIZE,
	        (ps, cf) -> {
	            ps.setString(1, cf.getFileName());
	            ps.setString(2, cf.getContent());
	            ps.setString(3, cf.getLanguage());
	            ps.setTimestamp(4, Timestamp.valueOf(cf.getCreatedAt()));
	            ps.setString(5, cf.getContentHash());
	        }
	    );

	    logger.info(
	        "Batch insert completed: {} files processed in {}ms",
	        codeFiles.size(),
	        System.currentTimeMillis() - start
	    );

	    return result;
	}

	
	@Transactional
	public Set<String> insertAndReturnNewHashes(List<CodeFile> codeFiles) {

	    if (codeFiles.isEmpty()) {
	        return Set.of();
	    }

	    String sql = """
	        INSERT INTO code_files
	        (id, file_name, content, language, created_at, content_hash, trigrams_generated)
	        VALUES (nextval('code_file_sequence'), ?, ?, ?, ?, ?, false)
	        ON CONFLICT (content_hash) DO NOTHING
	        RETURNING content_hash
	        """;

	    return jdbcTemplate.execute(
	        (ConnectionCallback<Set<String>>) con -> {

	            Set<String> inserted = new HashSet<>();

	            try (PreparedStatement ps = con.prepareStatement(sql)) {

	                for (CodeFile cf : codeFiles) {
	                    ps.setString(1, cf.getFileName());
	                    ps.setString(2, cf.getContent());
	                    ps.setString(3, cf.getLanguage());
	                    ps.setTimestamp(4, Timestamp.valueOf(cf.getCreatedAt()));
	                    ps.setString(5, cf.getContentHash());
	                    ps.addBatch();
	                }

	                ps.execute();

	                try (ResultSet rs = ps.getResultSet()) {
	                    while (rs.next()) {
	                        inserted.add(rs.getString(1));
	                    }
	                }
	            }

	            return inserted;
	        }
	    );
	}

	




	/**
	 * Bulk insert with trigram vectors Used when trigrams are already generated
	 */
	@Transactional
	public int[][] batchInsertWithTrigrams(List<CodeFile> codeFiles) {
		String sql = "INSERT INTO code_files " + "(id, file_name, content, language, created_at, content_hash, "
				+ "trigram_vector, trigrams_generated) "
				+ "VALUES (nextval('code_file_sequence'), ?, ?, ?, ?, ?, ?::jsonb, true)";

		return jdbcTemplate.batchUpdate(sql, codeFiles, BATCH_SIZE, (PreparedStatement ps, CodeFile codeFile) -> {
			ps.setString(1, codeFile.getFileName());
			ps.setString(2, codeFile.getContent());
			ps.setString(3, codeFile.getLanguage());
			ps.setTimestamp(4, Timestamp.valueOf(codeFile.getCreatedAt()));
			ps.setString(5, codeFile.getContentHash());

			// Convert Map to JSON string
			String trigramJson = convertMapToJson(codeFile.Gettrigram_vector());
			ps.setString(6, trigramJson);
		});
	}

	/**
	 * Simple JSON conversion for trigrams
	 */
	private String convertMapToJson(java.util.Map<String, Integer> map) {
		if (map == null || map.isEmpty()) {
			return "{}";
		}

		StringBuilder json = new StringBuilder("{");
		boolean first = true;

		for (java.util.Map.Entry<String, Integer> entry : map.entrySet()) {
			if (!first)
				json.append(",");
			json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
			first = false;
		}

		json.append("}");
		return json.toString();
	}
}