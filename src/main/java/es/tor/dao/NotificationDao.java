package es.tor.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import es.tor.entity.Notification;

@Repository(value = "notificacionDao")
public class NotificationDao extends GenericAfiliacionDao<Notification> {
    public final static Integer NUM_RESULTS_PAGINATOR = 20;

    @Override
    protected Class<Notification> getEntityClass() {
        return Notification.class;
    }

    @Override
    protected String getOrderBy() {
        return " order by p.fecha desc";
    }

    @Override
    public List<Notification> findAll() {
        return findAll(0);
    }

    public List<Notification> findAll(int start) {
        return getEntityManager().createQuery("select p from Notificacion p" + getOrderBy(), Notification.class).setFirstResult(start)
                .setMaxResults(NUM_RESULTS_PAGINATOR).getResultList();
    }

    public List<Notification> findByCanales(String canales) {
        return getEntityManager()
                .createQuery(
                        "select p from Notificacion p where p.canal.idCanal in ('" + canales.replaceAll(",", "', '")
                                + "')" + getOrderBy(), Notification.class).setMaxResults(NUM_RESULTS_PAGINATOR)
                .getResultList();
    }

    public List<Notification> findByIdDispositivo(Long idDispositivo) {
        return getEntityManager()
                .createQuery("select p from Notificacion p where p.idDispositivo = :idDispositivo" + getOrderBy(),
                        Notification.class).setParameter("idDispositivo", idDispositivo)
                .setMaxResults(NUM_RESULTS_PAGINATOR).getResultList();
    }
}