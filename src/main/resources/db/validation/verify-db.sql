-- Read-only verification. Every *_errors count must be zero.
USE enjoytrip;
SELECT VERSION() AS mysql_version,DATABASE() AS app_database;
SELECT 'sidos' AS entity,COUNT(*) AS cnt FROM sidos
UNION ALL SELECT 'guguns',COUNT(*) FROM guguns
UNION ALL SELECT 'contenttypes',COUNT(*) FROM contenttypes
UNION ALL SELECT 'attractions',COUNT(*) FROM attractions;
SELECT content_type,COUNT(*) AS cnt FROM attractions GROUP BY content_type ORDER BY content_type;
SELECT * FROM contenttypes ORDER BY content_type_id;
SELECT COUNT(*) AS duplicate_content_id_errors FROM
  (SELECT content_id FROM attractions GROUP BY content_id HAVING COUNT(*)>1) d;
SELECT COUNT(*) AS region_errors FROM attractions a LEFT JOIN sidos s ON a.area_code=s.sido_code WHERE s.no IS NULL;
SELECT COUNT(*) AS type_errors FROM attractions a LEFT JOIN contenttypes t ON a.content_type=t.content_type_id WHERE t.content_type_id IS NULL;
SELECT COUNT(*) AS gugun_errors FROM attractions a LEFT JOIN guguns g
  ON a.area_code=g.sido_code AND a.sigungu_code=g.gugun_code
  WHERE a.sigungu_code IS NOT NULL AND g.no IS NULL;
SELECT COUNT(*) AS stop_reference_errors FROM plan_stops s LEFT JOIN attractions a ON a.id=s.attraction_id WHERE a.id IS NULL;
SELECT COUNT(*) AS image_reference_errors FROM attraction_images i LEFT JOIN attractions a ON a.id=i.attraction_id WHERE a.id IS NULL;
SELECT COUNT(*) AS coordinate_errors FROM attractions
  WHERE latitude=0 OR longitude=0 OR latitude NOT BETWEEN -90 AND 90 OR longitude NOT BETWEEN -180 AND 180;
SELECT TABLE_NAME,CONSTRAINT_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA=DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME,CONSTRAINT_NAME,ORDINAL_POSITION;
