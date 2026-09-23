CREATE TABLE folders (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    org_id     BIGINT NOT NULL REFERENCES organizations(id),
    name       VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, name)
);

CREATE TABLE tags (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    org_id     BIGINT NOT NULL REFERENCES organizations(id),
    name       VARCHAR(60) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, name)
);

CREATE TABLE url_tags (
    url_id BIGINT NOT NULL REFERENCES urls(id),
    tag_id BIGINT NOT NULL REFERENCES tags(id),
    PRIMARY KEY (url_id, tag_id)
);

ALTER TABLE urls ADD COLUMN folder_id BIGINT REFERENCES folders(id);

CREATE INDEX idx_urls_folder_id ON urls (folder_id);
CREATE INDEX idx_url_tags_tag_id ON url_tags (tag_id);
CREATE INDEX idx_folders_org_id ON folders (org_id);
CREATE INDEX idx_tags_org_id ON tags (org_id);
