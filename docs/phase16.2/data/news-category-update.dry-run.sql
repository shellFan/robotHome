-- DRY RUN ONLY. Do not execute before backup and explicit production write authorization.
-- UPDATE-only, idempotent guard: assigns the existing “融资” category only to verified news article 21.
UPDATE article SET category_id = 10 WHERE id = 21 AND category_id IS NULL AND deleted = 0;
