CREATE OR REPLACE VIEW view_global_roles AS
  SELECT u.id,
         u.name,
         u.description,
         i.name AS it_system,
         IF (COUNT(g.user_role_id) > 0, true, false) AS checked
  FROM user_role u
  JOIN it_system i ON u.it_system_id = i.id
  LEFT JOIN global_role g ON (u.id = g.user_role_id)
  GROUP BY u.id;