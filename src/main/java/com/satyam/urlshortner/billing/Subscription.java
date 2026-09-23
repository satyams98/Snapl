package com.satyam.urlshortner.billing;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("subscriptions")
@Data
@AllArgsConstructor
public class Subscription implements Persistable<Long> {

    @Id
    private Long id;

    @Column("org_id")
    private Long orgId;

    @Column("plan_id")
    private Long planId;

    @Column("status")
    private SubscriptionStatus status;

    @Column("stripe_customer_id")
    private String stripeCustomerId;

    @Column("stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column("current_period_end")
    private Instant currentPeriodEnd;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Override
    public boolean isNew() {
        return id == null;
    }
}
