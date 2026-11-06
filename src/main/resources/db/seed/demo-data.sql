-- Optional synthetic content ONLY for the dedicated test DB.
-- Run setup-test.py --with-attractions and provide a test administrator first.
USE enjoytrip_test;
SET NAMES utf8mb4;
SET time_zone='+09:00';
DROP PROCEDURE IF EXISTS seed_enjoytrip_demo;
DELIMITER $$
CREATE PROCEDURE seed_enjoytrip_demo()
BEGIN
  DECLARE owner_id BIGINT;
  DECLARE demo_plan BIGINT;
  DECLARE first_stop BIGINT;
  DECLARE second_stop BIGINT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  SELECT id INTO owner_id FROM users WHERE role='ADMIN' ORDER BY id LIMIT 1;
  SELECT id INTO first_stop FROM attractions WHERE content_id=2733967;
  SELECT id INTO second_stop FROM attractions WHERE area_code=1 AND content_type=12
    AND content_id<>2733967 AND latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY content_id LIMIT 1;
  IF owner_id IS NULL OR first_stop IS NULL OR second_stop IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Start test app with imported attractions before seeding demo content.';
  END IF;
  START TRANSACTION;
  INSERT INTO plans(user_id,title,start_date,end_date,budget,details,is_public)
  SELECT owner_id,'DB 시연: 서울 하루 여행',CURRENT_DATE(),CURRENT_DATE(),30000,'제공 관광지와 연결된 공개 시연 계획입니다.',TRUE
  WHERE NOT EXISTS (SELECT 1 FROM plans WHERE user_id=owner_id AND title='DB 시연: 서울 하루 여행');
  SELECT id INTO demo_plan FROM plans WHERE user_id=owner_id AND title='DB 시연: 서울 하루 여행' ORDER BY id LIMIT 1;
  INSERT INTO plan_stops(plan_id,attraction_id,visit_date,stop_order,budget,memo)
  SELECT demo_plan,first_stop,start_date,1,10000,'첫 번째 방문지' FROM plans WHERE id=demo_plan
    AND NOT EXISTS (SELECT 1 FROM plan_stops WHERE plan_id=demo_plan AND stop_order=1);
  INSERT INTO plan_stops(plan_id,attraction_id,visit_date,stop_order,budget,memo)
  SELECT demo_plan,second_stop,start_date,2,20000,'두 번째 방문지' FROM plans WHERE id=demo_plan
    AND NOT EXISTS (SELECT 1 FROM plan_stops WHERE plan_id=demo_plan AND stop_order=2);
  INSERT INTO posts(user_id,kind,title,body)
  SELECT owner_id,'NOTICE','DB 시연 안내','검증용 DB의 예시 공지입니다.'
  WHERE NOT EXISTS (SELECT 1 FROM posts WHERE user_id=owner_id AND kind='NOTICE' AND title='DB 시연 안내');
  INSERT INTO posts(user_id,kind,title,body)
  SELECT owner_id,'BOARD','DB 시연 여행 정보','제공 관광지 데이터를 이용해 여행 계획을 만들어 보세요.'
  WHERE NOT EXISTS (SELECT 1 FROM posts WHERE user_id=owner_id AND kind='BOARD' AND title='DB 시연 여행 정보');
  COMMIT;
  SELECT demo_plan AS public_demo_plan_id;
END$$
DELIMITER ;
CALL seed_enjoytrip_demo();
DROP PROCEDURE seed_enjoytrip_demo;
