-- Удаление таблицы перед созданием (если уже существует)
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS tasks CASCADE;

-- Создание таблицы пользователей
CREATE TABLE users (
                       id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(100) NOT NULL,
                       role VARCHAR(50) NOT NULL,  -- ROLE_ADMIN / ROLE_USER
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Вставка тестовых пользователей (Администратор и Обычный пользователь)
INSERT INTO users (email, password, first_name, last_name, role)
VALUES
    ('admin@mail.com', 'adminpass', 'Admin', 'User', 'ROLE_ADMIN'),
    ('user@mail.com', 'userpass', 'Test', 'User', 'ROLE_USER');

CREATE TABLE tasks (
                       id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       description TEXT,
                       status VARCHAR(50) CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED')) NOT NULL,
                       priority VARCHAR(50) CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')) NOT NULL,
                       due_date TIMESTAMP,
                       author_id BIGINT NOT NULL,
                       assignee_id BIGINT,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
                       FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL
);

INSERT INTO tasks (title, description, status, priority, due_date, author_id)
VALUES
    ('API - task', 'Создать API для управления задачами', 'PENDING', 'HIGH', '2025-03-15 23:59:59', 1),
    ('API - user', 'Создать API для управления пользователями', 'PENDING', 'HIGH', '2025-03-15 23:59:59', 2),
    ('API - comment', 'Создать API для управления коментариями', 'PENDING', 'HIGH', '2025-03-15 23:59:59', 2);