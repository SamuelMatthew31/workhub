-- 1. TABEL EVENTS (Append-Only Source of Truth - ADR-005)
CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users(id),
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    sequence_number BIGSERIAL,
    schema_version INT NOT NULL DEFAULT 1,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indeks GIN pada payload JSONB untuk query cepat (FR-ACT-4)
CREATE INDEX idx_events_payload_gin ON events USING GIN (payload);
CREATE INDEX idx_events_workspace_seq ON events (workspace_id, sequence_number DESC);
CREATE INDEX idx_events_entity ON events (entity_type, entity_id);

-- 2. TABEL PROJECTS
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_projects_workspace ON projects (workspace_id);

-- 3. TABEL TASK STATUSES (Status Kustom per Project - FR-TASK-1)
CREATE TABLE task_statuses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    position INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_project_status_name UNIQUE (project_id, name)
);

CREATE INDEX idx_task_statuses_project ON task_statuses (project_id, position);

-- 4. TABEL TASKS (Proyeksi dari Event - FR-TASK-4 & FR-TASK-7)
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    status_id UUID NOT NULL REFERENCES task_statuses(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, URGENT
    due_date TIMESTAMP WITH TIME ZONE,
    assignee_id UUID REFERENCES users(id) ON DELETE SET NULL,
    creator_id UUID NOT NULL REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0, -- Optimistic Locking (FR-TASK-7)
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE, -- Soft Delete (ADR-007)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tasks_project ON tasks (project_id);
CREATE INDEX idx_tasks_status ON tasks (status_id);
CREATE INDEX idx_tasks_assignee ON tasks (assignee_id);
