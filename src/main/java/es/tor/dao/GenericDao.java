package es.tor.dao;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class GenericDao<E> {
    protected abstract EntityManager getEntityManager();
    protected abstract JpaTransactionManager getTransactionManager();

    protected abstract Class<E> getEntityClass();
    
    protected String getOrderBy() {
        return "";
    }
    
    public List<E> findAll() {
        return getEntityManager().createQuery(
                "select p from " + getEntityClass().getName() + " p" + getOrderBy(), getEntityClass())
                .getResultList();
    }
    
    public E findById(Long id) {
        return getEntityManager().find(getEntityClass(), id);
    }
    
    public Long count() {
        return (Long) getEntityManager().createQuery("select count(e) from " + getEntityClass().getName() + " e").getSingleResult();
    }
    
    public E save(final E entity) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(getTransactionManager());
        return transactionTemplate.execute(new TransactionCallback<E>() {
            @Override
            public E doInTransaction(TransactionStatus status) {
                return getEntityManager().merge(entity);
            }
        });
    }

    public void remove(final Object id) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(getTransactionManager());
        transactionTemplate.execute(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction(TransactionStatus status) {
                getEntityManager().remove(getEntityManager().getReference(getEntityClass(), id));
                return null;
            }
        });
    }
}
