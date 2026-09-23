package com.satyam.urlshortner.billing;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("plans")
@Data
@AllArgsConstructor
public class Plan implements Persistable<Long> {

    @Id
    private Long id;

    @Column("code")
    private String code;

    @Column("name")
    private String name;

    @Column("price_cents")
    private int priceCents;

    @Column("max_links")
    private int maxLinks;

    @Column("max_custom_domains")
    private int maxCustomDomains;

    @Column("max_clicks_per_month")
    private int maxClicksPerMonth;

    @Column("api_access_allowed")
    private boolean apiAccessAllowed;

    @Column("stripe_price_id")
    private String stripePriceId;

    @Override
    public boolean isNew() {
        return id == null;
    }

    public boolean isUnlimitedLinks() {
        return maxLinks < 0;
    }

    public boolean isUnlimitedClicks() {
        return maxClicksPerMonth < 0;
    }
}
