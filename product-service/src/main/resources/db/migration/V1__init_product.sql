-- Product service write-model schema (database: gesili_product).
-- Elasticsearch holds the read model; it is not managed by Flyway.

CREATE TABLE categories (
    id          UUID PRIMARY KEY,
    name        VARCHAR(150) NOT NULL UNIQUE,
    slug        VARCHAR(150) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE products (
    id          UUID PRIMARY KEY,
    sku         VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    price       NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    currency    CHAR(3)      NOT NULL,
    category_id UUID         NOT NULL REFERENCES categories (id),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_product_category ON products (category_id);
CREATE INDEX idx_product_active   ON products (active);
