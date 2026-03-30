-- Seed data is provisioned by Spring Boot bootstrap components so default users
-- and demo assets can be created with encoded passwords and runtime-resolved owner ids.
DELETE FROM app_jwt_session WHERE 1 = 0;
