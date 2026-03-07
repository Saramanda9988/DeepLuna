DO
$$
BEGIN
    -- 兼容历史库：如果表名是 users，重命名为 "user"
    IF to_regclass('users') IS NOT NULL AND to_regclass('"user"') IS NULL THEN
        EXECUTE 'ALTER TABLE users RENAME TO "user"';
    END IF;
END
$$;

DO
$$
BEGIN
    -- 兼容历史库：userId -> user_id
    IF to_regclass('"user"') IS NOT NULL
        AND EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'user'
              AND table_schema = current_schema()
              AND column_name = 'userId'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'user'
              AND table_schema = current_schema()
              AND column_name = 'user_id'
        ) THEN
        EXECUTE 'ALTER TABLE "user" RENAME COLUMN "userId" TO user_id';
    END IF;
END
$$;

DO
$$
BEGIN
    -- 兼容历史库：id -> user_id
    IF to_regclass('"user"') IS NOT NULL
        AND EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'user'
              AND table_schema = current_schema()
              AND column_name = 'id'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'user'
              AND table_schema = current_schema()
              AND column_name = 'user_id'
        ) THEN
        EXECUTE 'ALTER TABLE "user" RENAME COLUMN id TO user_id';
    END IF;
END
$$;

DO
$$
BEGIN
    -- 兼容历史库：userName -> user_name
    IF to_regclass('"user"') IS NOT NULL
        AND EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'user'
              AND table_schema = current_schema()
              AND column_name = 'userName'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'user'
              AND table_schema = current_schema()
              AND column_name = 'user_name'
        ) THEN
        EXECUTE 'ALTER TABLE "user" RENAME COLUMN "userName" TO user_name';
    END IF;
END
$$;

DO
$$
BEGIN
    -- 兼容历史库：username -> user_name
    IF to_regclass('"user"') IS NOT NULL
        AND EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'user'
              AND table_schema = current_schema()
              AND column_name = 'username'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'user'
              AND table_schema = current_schema()
              AND column_name = 'user_name'
        ) THEN
        EXECUTE 'ALTER TABLE "user" RENAME COLUMN username TO user_name';
    END IF;
END
$$;

DO
$$
BEGIN
    -- 统一 user_name 唯一约束
    IF to_regclass('"user"') IS NOT NULL
        AND EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'user'
              AND table_schema = current_schema()
              AND column_name = 'user_name'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM pg_constraint c
                     JOIN pg_class t ON c.conrelid = t.oid
                     JOIN pg_namespace n ON t.relnamespace = n.oid
            WHERE t.relname = 'user'
              AND n.nspname = current_schema()
              AND c.conname = 'uk_user_name'
        ) THEN
        EXECUTE 'ALTER TABLE "user" ADD CONSTRAINT uk_user_name UNIQUE (user_name)';
    END IF;
END
$$;
