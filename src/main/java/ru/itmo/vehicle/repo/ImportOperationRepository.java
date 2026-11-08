package ru.itmo.vehicle.repo;

import org.springframework.stereotype.Repository;
import ru.itmo.vehicle.domain.ImportOperation;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Repository
public class ImportOperationRepository {
    private final EntityManager em;

    public ImportOperationRepository(EntityManagerFactory entityManagerFactory) {
        this.em = EntityManagerSupport.sharedEntityManager(entityManagerFactory);
    }

    public ImportOperation save(ImportOperation operation) {
        if (operation.getId() == null) {
            em.persist(operation);
            return operation;
        }
        return em.merge(operation);
    }

    public Optional<ImportOperation> findById(Long id) {
        return Optional.ofNullable(em.find(ImportOperation.class, id));
    }

    public List<ImportOperation> findPage(Long userId, int offset, int limit) {
        String jpql = "SELECT o FROM ImportOperation o"
                + (userId == null ? "" : " WHERE o.user.id = :userId")
                + " ORDER BY o.createdAt DESC, o.id DESC";
        TypedQuery<ImportOperation> query = em.createQuery(jpql, ImportOperation.class);
        if (userId != null) {
            query.setParameter("userId", userId);
        }
        return query.setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    public long count(Long userId) {
        String jpql = "SELECT COUNT(o) FROM ImportOperation o"
                + (userId == null ? "" : " WHERE o.user.id = :userId");
        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        if (userId != null) {
            query.setParameter("userId", userId);
        }
        return query.getSingleResult();
    }
}
