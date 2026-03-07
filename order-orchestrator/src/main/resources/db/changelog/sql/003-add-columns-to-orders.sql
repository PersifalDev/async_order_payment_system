ALTER TABLE orders
    ADD COLUMN client_estimate     NUMERIC(12,2),
    ADD COLUMN final_amount        NUMERIC(12,2),
    ADD COLUMN authorized_amount   NUMERIC(12,2),
    ADD COLUMN captured_amount     NUMERIC(12,2),
    ADD COLUMN version             BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN payment_status      INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN cancellation_reason TEXT,
    ADD COLUMN created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN updated_at          TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_orders_payment_status
    ON orders (payment_status);

