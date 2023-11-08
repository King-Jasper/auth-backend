package com.mintfintech.savingsms.infrastructure.servicesimpl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mintfintech.savingsms.domain.entities.AbstractBaseEntity;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.AuditTrailService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.events.outgoing.AuditLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import java.io.IOException;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@Slf4j
@Named
public class AuditTrailServiceImpl implements AuditTrailService {

    private Gson gson;

    private ApplicationEventService applicationEventService;
    @Autowired
    public void setApplicationEventService(ApplicationEventService applicationEventService) {
        this.applicationEventService = applicationEventService;
    }

    @Value("${spring.application.name}")
    private String systemName;

    @PostConstruct
    public void initialize() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY);
        gsonBuilder.setExclusionStrategies(new ExcludeProxiedFields()).create();
        gson = gsonBuilder.create();
       // log.info("Initialized audit service");
    }

    @Async
    @Override
    public <T extends AbstractBaseEntity<Long>> void createAuditLog(AuthenticatedUser authenticatedUser, AuditType auditType, String description, T record) {
        createLog(authenticatedUser, auditType, description, record, null);
    }

    @Async
    @Override
    public <T extends AbstractBaseEntity<Long>> void createAuditLog(AuthenticatedUser authenticatedUser, AuditType auditType, String description, T oldRecord, T newRecord) {
        createLog(authenticatedUser, auditType, description, newRecord, oldRecord);
    }

    private <T extends AbstractBaseEntity<Long>> void createLog(AuthenticatedUser currentUser, AuditType auditType, String description, T newRecord, T oldRecord) {

        String payload = gson.toJson(newRecord);
        AuditLogEvent auditLogEvent = AuditLogEvent.builder()
                .auditType(auditType.name())
                .actorId(currentUser != null ? currentUser.getUserId() : "")
                .accountId(currentUser != null ? currentUser.getAccountId() : "")
                .actorName(currentUser != null ? currentUser.getName() : "")
                .description(description)
                .newRecordPayload(payload)
                .entityId(newRecord.getId())
                .entityName(newRecord.getClass().getSimpleName())
                .oldRecordPayload(oldRecord != null ? gson.toJson(oldRecord) : "")
                .systemName(systemName)
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.APPLICATION_AUDIT_TRAIL, new EventModel<>(auditLogEvent));
    }

   /*
    private AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null && authentication.getPrincipal() != null) {
            return (AuthenticatedUser) authentication.getPrincipal();
        }
        return null;
    }*/

    private static class  HibernateProxyTypeAdapter extends TypeAdapter<HibernateProxy> {

        private final Gson context;
        private HibernateProxyTypeAdapter(Gson context) {
            this.context = context;
        }

        @Override
        public HibernateProxy read(JsonReader in) throws IOException {
            throw new UnsupportedOperationException("Not supported");
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public void write(JsonWriter out, HibernateProxy value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            // Retrieve the original (not proxy) class
            Class<?> baseType = Hibernate.getClass(value);
            // Get the TypeAdapter of the original class, to delegate the serialization
            TypeAdapter delegate = context.getAdapter(TypeToken.get(baseType));
            // Get a filled instance of the original class
            Object unproxiedValue = ((HibernateProxy) value).getHibernateLazyInitializer()
                    .getImplementation();
            // Serialize the value
            delegate.write(out, unproxiedValue);
        }

        private static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                return (HibernateProxy.class.isAssignableFrom(type.getRawType()) ? (TypeAdapter<T>) new HibernateProxyTypeAdapter(gson) : null);
            }
        };
    }
    private static class ExcludeProxiedFields implements ExclusionStrategy {

        @Override
        public boolean shouldSkipField(FieldAttributes fa) {
            return fa.getAnnotation(ManyToOne.class) != null ||
                    fa.getAnnotation(OneToOne.class) != null ||
                    fa.getName().equalsIgnoreCase("dateCreated") ||
                    fa.getName().equalsIgnoreCase("dateModified");
        }
        @Override
        public boolean shouldSkipClass(Class<?> type) {
            return false;
        }
    }


}
