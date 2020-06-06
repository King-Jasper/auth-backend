package com.mintfintech.savingsms.domain.services;

import com.mintfintech.savingsms.domain.entities.AbstractBaseEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
public interface AuditTrailService{
    enum AuditType { UPDATE, CREATE, DELETE}
    <T extends AbstractBaseEntity<Long>> void createAuditLog(AuthenticatedUser authenticatedUser, AuditType auditType, String description, T oldRecord, T newRecord);
    <T extends AbstractBaseEntity<Long>> void createAuditLog(AuthenticatedUser authenticatedUser, AuditType auditType, String description, T record);
}
