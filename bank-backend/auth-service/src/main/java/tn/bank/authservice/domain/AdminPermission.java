package tn.bank.authservice.domain;

/**
 * Permissions granulaires attribuables aux comptes ADMIN.
 * Un admin sans permission particulière peut toujours se connecter au panneau admin
 * (dashboard general, journal d'audit, alertes de securite) mais ne voit/execute pas
 * les actions liees aux modules ci-dessous tant qu'elles ne lui sont pas accordees.
 */
public enum AdminPermission {
    USERS_VIEW,
    USERS_MANAGE,
    ACCOUNTS_VIEW,
    ACCOUNTS_MANAGE,
    CREDITS_VIEW,
    CREDITS_VALIDATE
}