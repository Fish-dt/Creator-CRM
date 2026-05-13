CREATE TABLE deals (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id          UUID        NOT NULL,
    brand_name          TEXT        NOT NULL,
    title               TEXT        NOT NULL,
    description         TEXT,
    value               NUMERIC(12,2) NOT NULL DEFAULT 0,
    currency            VARCHAR(255)  NOT NULL DEFAULT 'USD',
    status              TEXT        NOT NULL DEFAULT 'DRAFT',
    campaign_start_date TIMESTAMPTZ,
    campaign_end_date   TIMESTAMPTZ,
    version             BIGINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_deals_creator_id ON deals(creator_id);
CREATE INDEX idx_deals_status     ON deals(status);

COMMENT ON TABLE deals IS 'Brand deal records owned by a creator';
