package ru.itmo.vehicle.repo;

import org.springframework.stereotype.Repository;
import ru.itmo.vehicle.domain.AppUser;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.Locale;
import java.util.Optional;

@Repository
public class AppUserRepository {
    private final EntityManager em;

    public AppUserRepository(EntityManagerFactory entityManagerFactory) {
        this.em = EntityManagerSupport.sharedEntityManager(entityManagerFactory);
    }

    public Optional<AppUser> findByUsername(String username) {
        TypedQuery<AppUser> query = em.createQuery(
                "SELECT u FROM AppUser u WHERE LOWER(u.username) = :username", AppUser.class);
        query.setParameter("username", username == null ? "" : username.trim().toLowerCase(Locale.ROOT));
        return query.getResultList().stream().findFirst();
    }

    public Optional<AppUser> findById(Long id) {
        return Optional.ofNullable(em.find(AppUser.class, id));
    }

    public AppUser save(AppUser user) {
        if (user.getId() == null) {
            em.persist(user);
            return user;
        }
        return em.merge(user);
    }

    public long count() {
        return em.createQuery("SELECT COUNT(u) FROM AppUser u", Long.class).getSingleResult();
    }
}
