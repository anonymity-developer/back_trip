-- Requires validated trip_source source and upgraded enjoytrip on the SAME isolated instance.
-- Stop application/sync and back up first. Never use mysql --force.
-- Policy: retain IDs and non-empty existing values, add new content IDs, fill missing values.
USE enjoytrip;
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS import_trip_source;
DELIMITER $$
CREATE PROCEDURE import_trip_source()
BEGIN
  DECLARE previous_count BIGINT;
  DECLARE added_count BIGINT;
  DECLARE filled_count BIGINT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  IF (SELECT COUNT(*) FROM trip_source.sidos) <> 17
    OR (SELECT COUNT(*) FROM trip_source.guguns) <> 234
    OR (SELECT COUNT(*) FROM trip_source.contenttypes) <> 8
    OR (SELECT COUNT(*) FROM trip_source.attractions) <> 50915 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Source counts differ from reviewed source snapshot.';
  END IF;
  IF EXISTS (SELECT 1 FROM trip_source.attractions WHERE content_id IS NULL OR content_id<=0 OR title IS NULL OR TRIM(title)='' OR CHAR_LENGTH(title)>255)
    OR EXISTS (SELECT content_id FROM trip_source.attractions GROUP BY content_id HAVING COUNT(*)>1)
    OR EXISTS (SELECT 1 FROM trip_source.attractions a LEFT JOIN sidos s ON s.sido_code=a.area_code WHERE s.no IS NULL)
    OR EXISTS (SELECT 1 FROM trip_source.attractions a LEFT JOIN contenttypes t ON t.content_type_id=a.content_type_id WHERE t.content_type_id IS NULL)
    OR EXISTS (SELECT 1 FROM trip_source.attractions a LEFT JOIN guguns g ON g.sido_code=a.area_code AND g.gugun_code=a.si_gun_gu_code WHERE a.si_gun_gu_code IS NOT NULL AND g.no IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid content ID, title, region/type/gugun in source.';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS import_original_ids;
  CREATE TEMPORARY TABLE import_original_ids (content_id BIGINT PRIMARY KEY, id BIGINT NOT NULL);
  DROP TEMPORARY TABLE IF EXISTS import_candidates;
  CREATE TEMPORARY TABLE import_candidates (
    content_id BIGINT PRIMARY KEY, title VARCHAR(255), content_type INT, area_code INT,
    sigungu_code INT, address VARCHAR(500), description TEXT,
    latitude DECIMAL(11,8), longitude DECIMAL(11,8), image_url VARCHAR(1000)
  );
  START TRANSACTION;
  SELECT COUNT(*) INTO previous_count FROM attractions;
  INSERT INTO import_original_ids SELECT content_id,id FROM attractions;
  INSERT INTO import_candidates
  SELECT content_id,title,content_type_id,area_code,si_gun_gu_code,
    NULLIF(TRIM(CONCAT_WS(' ',NULLIF(TRIM(addr1),''),NULLIF(TRIM(addr2),''))),''),
    NULLIF(TRIM(REPLACE(REPLACE(REGEXP_REPLACE(
      REGEXP_REPLACE(COALESCE(overview,''),'<br[[:space:]]*/?>',CHAR(10 USING utf8mb4),1,0,'i'),
      '<[^>]*>',''),'&nbsp;',' '),'&amp;','&')),''),
    CASE WHEN latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180 AND latitude<>0 AND longitude<>0 THEN latitude END,
    CASE WHEN latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180 AND latitude<>0 AND longitude<>0 THEN longitude END,
    NULLIF(TRIM(first_image1),'')
  FROM trip_source.attractions;

  INSERT INTO attractions(content_id,title,content_type,area_code,sigungu_code,address,description,latitude,longitude,image_url)
  SELECT s.content_id,s.title,s.content_type,s.area_code,s.sigungu_code,s.address,s.description,s.latitude,s.longitude,s.image_url
  FROM import_candidates s LEFT JOIN attractions a ON a.content_id=s.content_id
  WHERE a.id IS NULL ORDER BY s.content_id;
  SET added_count=ROW_COUNT();

  -- Fill only missing values. A source gugun is valid only for the retained application area.
  -- Existing source_updated_at is never synthesized or overwritten by this snapshot.
  UPDATE attractions a JOIN import_candidates s ON s.content_id=a.content_id
    JOIN import_original_ids old ON old.content_id=a.content_id
  SET a.address=CASE WHEN a.address IS NULL OR TRIM(a.address)='' THEN s.address ELSE a.address END,
      a.description=CASE WHEN a.description IS NULL OR TRIM(a.description)='' THEN s.description ELSE a.description END,
      a.image_url=CASE WHEN a.image_url IS NULL OR TRIM(a.image_url)='' THEN s.image_url ELSE a.image_url END,
      a.sigungu_code=CASE WHEN a.sigungu_code IS NULL AND a.area_code=s.area_code THEN s.sigungu_code ELSE a.sigungu_code END;
  SET filled_count=ROW_COUNT();
  -- Treat coordinates as a pair; do not mix a retained coordinate with a different source point.
  UPDATE attractions a JOIN import_candidates s ON s.content_id=a.content_id
    JOIN import_original_ids old ON old.content_id=a.content_id
  SET a.latitude=s.latitude,a.longitude=s.longitude
  WHERE a.latitude IS NULL AND a.longitude IS NULL AND s.latitude IS NOT NULL AND s.longitude IS NOT NULL;
  SET filled_count=filled_count+ROW_COUNT();

  IF EXISTS (SELECT 1 FROM import_original_ids old LEFT JOIN attractions a ON a.content_id=old.content_id WHERE a.id IS NULL OR a.id<>old.id)
    OR (SELECT COUNT(*) FROM attractions) <> previous_count+added_count THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Attraction ID/count preservation check failed.';
  END IF;
  COMMIT;
  SELECT previous_count AS previous_attractions,added_count AS added_attractions,
    filled_count AS fill_operations,(SELECT COUNT(*) FROM attractions) AS total_attractions;
  DROP TEMPORARY TABLE import_candidates;
  DROP TEMPORARY TABLE import_original_ids;
END$$
DELIMITER ;
CALL import_trip_source();
DROP PROCEDURE import_trip_source;
