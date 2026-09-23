CREATE TABLE plans (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code                  VARCHAR(30) NOT NULL UNIQUE,
    name                  VARCHAR(60) NOT NULL,
    price_cents           INTEGER NOT NULL,
    max_links             INTEGER NOT NULL,  -- -1 means unlimited
    max_custom_domains    INTEGER NOT NULL,
    max_clicks_per_month  INTEGER NOT NULL,  -- -1 means unlimited
    api_access_allowed    BOOLEAN NOT NULL,
    stripe_price_id       VARCHAR(100)
);

INSERT INTO plans (code, name, price_cents, max_links, max_custom_domains, max_clicks_per_month, api_access_allowed, stripe_price_id) VALUES
    ('FREE', 'Free', 0, 25, 0, 1000, false, NULL),
    ('PRO', 'Pro', 2900, 1000, 1, 50000, true, NULL),
    ('BUSINESS', 'Business', 9900, -1, 5, -1, true, NULL);

CREATE TABLE subscriptions (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    org_id                  BIGINT NOT NULL UNIQUE REFERENCES organizations(id),
    plan_id                 BIGINT NOT NULL REFERENCES plans(id),
    status                  VARCHAR(20) NOT NULL,
    stripe_customer_id      VARCHAR(100),
    stripe_subscription_id  VARCHAR(100),
    current_period_end      TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_subscriptions_org_id ON subscriptions (org_id);
