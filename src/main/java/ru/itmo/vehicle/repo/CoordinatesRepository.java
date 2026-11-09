package ru.itmo.vehicle.repo;

import org.springframework.stereotype.Repository;
import ru.itmo.vehicle.domain.Coordinates;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;

@Repository
public class CoordinatesRepository {
    private final EntityManager em;

    public CoordinatesRepository(EntityManagerFactory entityManagerFactory) {
        this.em = EntityManagerSupport.sharedEntityManager(entityManagerFactory);
    }

    public List<Coordinates> findAll() {
        return em.createQuery("SELECT c FROM Coordinates c ORDER BY c.id", Coordinates.class)
                .getResultList();
    }

    public Optional<Coordinates> findById(Long id) {
        return Optional.ofNullable(em.find(Coordinates.class, id));
    }

    public void setSerializableIsolation() {
        em.createNativeQuery("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE").executeUpdate();
    }

    public Optional<Coordinates> findByPair(Double x, int y) {
        return em.createQuery("SELECT c FROM Coordinates c WHERE c.x = :x AND c.y = :y ORDER BY c.id",
                        Coordinates.class)
                .setParameter("x", x)
                .setParameter("y", y)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }

    public Coordinates save(Coordinates coordinates) {
        if (coordinates.getId() == null) {
            em.persist(coordinates);
            return coordinates;
        }
        return em.merge(coordinates);
    }

    public void delete(Coordinates coordinates) {
        em.remove(em.contains(coordinates) ? coordinates : em.merge(coordinates));
    }
}
