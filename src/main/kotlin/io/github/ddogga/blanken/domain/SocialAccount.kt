package io.github.ddogga.blanken.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint


@Entity
@Table(name = "social_account",
        uniqueConstraints = [
    UniqueConstraint(name = "uk_social_account_provider_provider_id", columnNames = ["provider", "provider_id"]),
],)
class SocialAccount(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    var oauthProvider: OauthProvider,

    @Column(name = "provider_id", nullable = false)
    var providerId : String,

    @Column(name = "id")
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,

) : BaseTimeEntity()